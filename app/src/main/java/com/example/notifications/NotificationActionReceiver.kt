package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.ManojSonaliApp
import com.example.R
import com.example.domain.model.CallStatus
import com.example.domain.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val targetType = intent.getStringExtra(EXTRA_TARGET_TYPE).orEmpty()
        val targetId = intent.getStringExtra(EXTRA_TARGET_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID)
            ?: ManojSonaliMessagingService.CHANNEL_MESSAGES

        val app = context.applicationContext as? ManojSonaliApp ?: return
        val container = app.container
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                        if (replyText.isNotEmpty() && targetId.isNotBlank()) {
                            val uid = container.auth.currentUser?.uid
                            if (uid != null) {
                                val senderName = container.authAndUserRepository
                                    .getUserProfileOnce(uid)
                                    ?.displayName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: container.auth.currentUser?.displayName
                                    ?: "Me"

                                if (targetType == "group") {
                                    val snap = container.firestore
                                        .collection("groups")
                                        .document(targetId)
                                        .get()
                                        .await()
                                    @Suppress("UNCHECKED_CAST")
                                    val memberIds = snap.get("memberIds") as? List<String> ?: emptyList()
                                    if (memberIds.isNotEmpty()) {
                                        container.groupRepository.sendGroupMessage(
                                            groupId = targetId,
                                            memberIds = memberIds,
                                            senderName = senderName,
                                            type = MessageType.TEXT,
                                            text = replyText
                                        )
                                    }
                                } else {
                                    val snap = container.firestore
                                        .collection("chats")
                                        .document(targetId)
                                        .get()
                                        .await()
                                    @Suppress("UNCHECKED_CAST")
                                    val participantIds = snap.get("participantIds") as? List<String> ?: emptyList()
                                    if (participantIds.isNotEmpty()) {
                                        container.chatRepository.sendMessage(
                                            chatId = targetId,
                                            participantIds = participantIds,
                                            senderName = senderName,
                                            type = MessageType.TEXT,
                                            text = replyText
                                        )
                                        container.chatRepository.markChatRead(targetId, emptyList())
                                    }
                                }

                                // Update notification momentarily to confirm reply was sent, then dismiss cleanly
                                runCatching {
                                    val confirmation = NotificationCompat.Builder(context, channelId)
                                        .setSmallIcon(R.drawable.ic_notification)
                                        .setColor(NotificationBitmapHelper.BRAND_EMERALD)
                                        .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
                                        .setContentText("You: $replyText")
                                        .setSubText("Reply sent")
                                        .setSilent(true)
                                        .setTimeoutAfter(2500L)
                                        .setAutoCancel(true)
                                        .build()
                                    NotificationManagerCompat.from(context).notify(notificationId, confirmation)
                                }
                            }
                        }
                    }

                    ACTION_MARK_READ -> {
                        if (targetType == "chat" && targetId.isNotBlank()) {
                            container.chatRepository.markChatRead(targetId, emptyList())
                        }
                        if (notificationId != 0) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }

                    ACTION_DECLINE_CALL -> {
                        if (targetId.isNotBlank()) {
                            container.callRepository.updateCallStatus(targetId, CallStatus.REJECTED)
                        }
                        if (notificationId != 0) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }
                }
            } catch (_: Exception) {
                if (notificationId != 0) {
                    runCatching { NotificationManagerCompat.from(context).cancel(notificationId) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.example.notifications.ACTION_REPLY"
        const val ACTION_MARK_READ = "com.example.notifications.ACTION_MARK_READ"
        const val ACTION_DECLINE_CALL = "com.example.notifications.ACTION_DECLINE_CALL"

        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TARGET_TYPE = "extra_target_type"
        const val EXTRA_TARGET_ID = "extra_target_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
    }
}
