package com.example.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ManojSonaliApp
import com.example.domain.model.ChatRoom
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
            if (settings?.notifyMessages == false) {
                return Result.success()
            }
            val blocked = settings?.blockedUserIds.orEmpty().toSet()
            val lastNotifiedMs = container.preferencesDataStore.getLastNotifiedMessageMs()

            val chatSnaps = container.firestore
                .collection("chats")
                .whereArrayContains("participantIds", uid)
                .get()
                .await()
            val rooms = chatSnaps.toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            var newestMs = lastNotifiedMs
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
    }
}
