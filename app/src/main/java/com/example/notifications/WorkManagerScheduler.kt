package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ManojSonaliApp
import com.example.domain.model.CallSession
import com.example.domain.model.CallStatus
import com.example.domain.model.CallType
import com.example.domain.model.ChatRoom
import com.example.domain.model.GroupChat
import com.example.domain.model.UserPrivateSettings
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class StatusExpiryCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ManojSonaliApp ?: return Result.success()
        app.container.statusRepository.deleteExpiredOwnStatuses()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "manoj_sonali_status_expiry_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StatusExpiryCleanupWorker>(
                6, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

class IncomingMessageSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ManojSonaliApp ?: return Result.success()
        val container = app.container
        val uid = container.auth.currentUser?.uid ?: return Result.success()

        return try {
            val settingsSnap = container.firestore
                .collection("users").document(uid)
                .collection("private").document("settings")
                .get()
                .await()
            val settings = settingsSnap.toObject(UserPrivateSettings::class.java)
            val allowMessages = settings?.notifyMessages ?: true
            val allowGroups = settings?.notifyGroups ?: true
            val allowCalls = settings?.notifyCalls ?: true
            val blocked = settings?.blockedUserIds.orEmpty().toSet()

            var lastNotifiedMs = container.preferencesDataStore.getLastNotifiedMessageMs()
            if (lastNotifiedMs == 0L) {
                lastNotifiedMs = System.currentTimeMillis()
                container.preferencesDataStore.updateLastNotifiedMessageMs(lastNotifiedMs)
            }
            var newestMs = lastNotifiedMs

            if (allowMessages) {
                val chatSnaps = container.firestore
                    .collection("chats")
                    .whereArrayContains("participantIds", uid)
                    .get()
                    .await()
                val rooms = chatSnaps.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                rooms.forEach { room ->
                    val msgTimeMs = room.lastMessageTime?.toDate()?.time ?: 0L
                    val unread = room.unreadFor(uid)
                    val peerId = room.peerId(uid)
                    if (unread > 0 &&
                        room.lastMessage.isNotBlank() &&
                        room.lastMessageSenderId != uid &&
                        uid !in room.mutedBy &&
                        peerId !in blocked &&
                        msgTimeMs > lastNotifiedMs
                    ) {
                        val senderName = container.authAndUserRepository
                            .getUserProfileOnce(peerId)
                            ?.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: "New message"
                        ManojSonaliMessagingService.showNotification(
                            context = applicationContext,
                            type = "message",
                            title = senderName,
                            body = room.lastMessage,
                            targetId = room.id
                        )
                        if (msgTimeMs > newestMs) {
                            newestMs = msgTimeMs
                        }
                    }
                }
            }

            if (allowGroups) {
                val myName = container.authAndUserRepository.getUserProfileOnce(uid)?.displayName
                val groupSnaps = container.firestore
                    .collection("groups")
                    .whereArrayContains("memberIds", uid)
                    .get()
                    .await()
                val groups = groupSnaps.toObjects(GroupChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                groups.forEach { group ->
                    val msgTimeMs = group.lastMessageTime?.toDate()?.time ?: 0L
                    if (group.lastMessage.isNotBlank() &&
                        group.lastMessageSenderName != null &&
                        group.lastMessageSenderName != myName &&
                        uid !in group.mutedBy &&
                        msgTimeMs > lastNotifiedMs
                    ) {
                        ManojSonaliMessagingService.showNotification(
                            context = applicationContext,
                            type = "group",
                            title = group.name,
                            body = "${group.lastMessageSenderName}: ${group.lastMessage}",
                            targetId = group.id
                        )
                        if (msgTimeMs > newestMs) {
                            newestMs = msgTimeMs
                        }
                    }
                }
            }

            if (allowCalls) {
                val cutoff = System.currentTimeMillis() - 90_000L
                val callSnaps = container.firestore
                    .collection("calls")
                    .whereArrayContains("participantIds", uid)
                    .get()
                    .await()
                val calls = callSnaps.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                calls.forEach { call ->
                    val startedMs = call.startedAt?.toDate()?.time ?: 0L
                    if (call.receiverId == uid &&
                        call.statusEnum == CallStatus.RINGING &&
                        call.callerId !in blocked &&
                        startedMs >= cutoff
                    ) {
                        val kind = if (call.typeEnum == CallType.VIDEO) "video" else "voice"
                        ManojSonaliMessagingService.showNotification(
                            context = applicationContext,
                            type = "call",
                            title = call.callerName ?: "Incoming Call",
                            body = "Incoming $kind call",
                            targetId = call.id
                        )
                    }
                }
            }

            if (newestMs > lastNotifiedMs) {
                container.preferencesDataStore.updateLastNotifiedMessageMs(newestMs)
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val WORK_NAME = "manoj_sonali_incoming_message_sync"
        private const val EXPEDITED_WORK_NAME = "manoj_sonali_incoming_message_expedited"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<IncomingMessageSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleExpeditedCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<IncomingMessageSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                EXPEDITED_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
