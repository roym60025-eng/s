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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? ManojSonaliApp ?: return
        serviceScope.launch {
            app.container.authAndUserRepository.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "message"
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: "New activity"
        showNotification(this, type, title, body)
    }

    companion object {
        const val CHANNEL_MESSAGES = "channel_messages"
        const val CHANNEL_GROUPS = "channel_groups"
        const val CHANNEL_CALLS = "channel_calls"
        const val CHANNEL_STATUS = "channel_status"

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

        fun showNotification(context: Context, type: String, title: String, body: String) {
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

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context)
                .notify((System.currentTimeMillis() % 100000).toInt(), notification)
        }
    }
}
