package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.ManojSonaliApp
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ManojSonaliMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        val app = applicationContext as? ManojSonaliApp ?: return
        serviceScope.launch {
            app.container.authAndUserRepository.saveFcmToken(installationId)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "message"
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: "New activity"
        val targetId = message.data["chatId"] ?: message.data["groupId"] ?: message.data["callId"]
        showNotification(this, type, title, body, targetId)
    }

    companion object {
        const val CHANNEL_MESSAGES = "channel_messages"
        const val CHANNEL_GROUPS = "channel_groups"
        const val CHANNEL_CALLS = "channel_calls"
        const val CHANNEL_STATUS = "channel_status"

        const val EXTRA_NAV_TYPE = "extra_nav_type"
        const val EXTRA_NAV_ID = "extra_nav_id"

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_MESSAGES,
                        "Direct Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "1-to-1 chat notifications" },
                    NotificationChannel(
                        CHANNEL_GROUPS,
                        "Group Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Group chat notifications" },
                    NotificationChannel(
                        CHANNEL_CALLS,
                        "Voice & Video Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Incoming and missed call alerts" },
                    NotificationChannel(
                        CHANNEL_STATUS,
                        "Status Updates",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "24-hour status story updates" }
                )
                channels.forEach { manager.createNotificationChannel(it) }
            }
        }

        fun showNotification(
            context: Context,
            type: String,
            title: String,
            body: String,
            targetId: String? = null
        ) {
            createChannels(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val channelId = when (type) {
                "group" -> CHANNEL_GROUPS
                "call", "missed_call" -> CHANNEL_CALLS
                "status" -> CHANNEL_STATUS
                else -> CHANNEL_MESSAGES
            }

            val navType = when (type) {
                "group" -> "group"
                "call" -> "call"
                else -> if (targetId != null) "chat" else null
            }

            val notificationId = "${type}:${targetId ?: title}".hashCode()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (navType != null && targetId != null) {
                    putExtra(EXTRA_NAV_TYPE, navType)
                    putExtra(EXTRA_NAV_ID, targetId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val category = if (type == "call") {
                NotificationCompat.CATEGORY_CALL
            } else {
                NotificationCompat.CATEGORY_MESSAGE
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(category)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
