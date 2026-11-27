package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.ManojSonaliApp
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.app_name)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "New activity"
        val targetId = message.data["chatId"]
            ?: message.data["groupId"]
            ?: message.data["callId"]
        val senderPhotoUrl = message.data["senderPhotoUrl"] ?: message.data["photoUrl"]
        val senderName = message.data["senderName"]
        val isVideoCall = message.data["callType"]?.equals("VIDEO", ignoreCase = true) == true ||
            body.contains("video", ignoreCase = true)

        showNotification(
            context = this,
            type = type,
            title = title,
            body = body,
            targetId = targetId,
            senderPhotoUrl = senderPhotoUrl,
            senderName = senderName,
            isVideoCall = isVideoCall
        )
    }

    companion object {
        const val CHANNEL_MESSAGES = "channel_messages"
        const val CHANNEL_GROUPS = "channel_groups"
        const val CHANNEL_CALLS = "channel_calls"

        const val EXTRA_NAV_TYPE = "extra_nav_type"
        const val EXTRA_NAV_ID = "extra_nav_id"

        private const val GROUP_KEY_MESSAGES = "com.example.notifications.GROUP_MESSAGES"
        private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_MESSAGES,
                        "Direct Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Conversation cards with quick reply for 1-to-1 chats"
                        enableLights(true)
                        lightColor = NotificationBitmapHelper.BRAND_EMERALD
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    },
                    NotificationChannel(
                        CHANNEL_GROUPS,
                        "Group Chats",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Conversation alerts for group channels"
                        enableLights(true)
                        lightColor = NotificationBitmapHelper.BRAND_EMERALD
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    },
                    NotificationChannel(
                        CHANNEL_CALLS,
                        "Voice & Video Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Priority incoming and missed call alerts"
                        enableLights(true)
                        lightColor = NotificationBitmapHelper.BRAND_ROSE
                        enableVibration(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                )
                channels.forEach { manager.createNotificationChannel(it) }
            }
        }

        fun showNotification(
            context: Context,
            type: String,
            title: String,
            body: String,
            targetId: String? = null,
            senderPhotoUrl: String? = null,
            senderName: String? = null,
            isVideoCall: Boolean = false
        ) {
            val appContext = context.applicationContext
            notificationScope.launch {
                showNotificationAsync(
                    context = appContext,
                    type = type,
                    title = title,
                    body = body,
                    targetId = targetId,
                    senderPhotoUrl = senderPhotoUrl,
                    senderName = senderName,
                    isVideoCall = isVideoCall
                )
            }
        }

        suspend fun showNotificationAsync(
            context: Context,
            type: String,
            title: String,
            body: String,
            targetId: String? = null,
            senderPhotoUrl: String? = null,
            senderName: String? = null,
            isVideoCall: Boolean = false
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
                else -> CHANNEL_MESSAGES
            }

            val navType = when (type) {
                "group" -> "group"
                "call" -> "call"
                else -> if (targetId != null) "chat" else null
            }

            val notificationId = "${type}:${targetId ?: title}".hashCode()

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (navType != null && targetId != null) {
                    putExtra(EXTRA_NAV_TYPE, navType)
                    putExtra(EXTRA_NAV_ID, targetId)
                }
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isCall = type == "call" || type == "missed_call"
            val avatarDisplayName = senderName?.takeIf { it.isNotBlank() } ?: title
            val avatarBitmap = withContext(Dispatchers.IO) {
                NotificationBitmapHelper.loadCircularAvatarOrMonogram(
                    photoUrl = senderPhotoUrl,
                    displayName = avatarDisplayName,
                    isCall = isCall
                )
            }
            val personIcon = IconCompat.createWithBitmap(avatarBitmap)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(avatarBitmap)
                .setColor(
                    if (type == "missed_call") NotificationBitmapHelper.BRAND_ROSE
                    else NotificationBitmapHelper.BRAND_EMERALD
                )
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)

            when (type) {
                "call" -> {
                    val callerPerson = Person.Builder()
                        .setName(title)
                        .setIcon(personIcon)
                        .setImportant(true)
                        .build()

                    val declinePendingIntent = targetId?.let { callId ->
                        val declineIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                            action = NotificationActionReceiver.ACTION_DECLINE_CALL
                            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                            putExtra(NotificationActionReceiver.EXTRA_TARGET_TYPE, "call")
                            putExtra(NotificationActionReceiver.EXTRA_TARGET_ID, callId)
                        }
                        PendingIntent.getBroadcast(
                            context,
                            notificationId * 31 + 3,
                            declineIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    }

                    builder
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setSubText(if (isVideoCall) "Incoming Video Call" else "Incoming Voice Call")
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setTimeoutAfter(45_000L)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                    val callStyled = if (declinePendingIntent != null) {
                        runCatching {
                            val callStyle = NotificationCompat.CallStyle.forIncomingCall(
                                callerPerson,
                                declinePendingIntent,
                                openPendingIntent
                            )
                            builder.setStyle(callStyle)
                            true
                        }.getOrDefault(false)
                    } else {
                        false
                    }

                    if (!callStyled) {
                        builder
                            .setContentTitle(title)
                            .setContentText(body)
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .setBigContentTitle(title)
                                    .bigText(body)
                            )
                            .addAction(0, "Answer", openPendingIntent)
                        if (declinePendingIntent != null) {
                            builder.addAction(0, "Decline", declinePendingIntent)
                        }
                    }
                }

                "missed_call" -> {
                    builder
                        .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                        .setSubText("Missed Call")
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle(title)
                                .bigText(body)
                        )
                        .addAction(0, "Open Calls", openPendingIntent)
                }

                "group" -> {
                    val mePerson = Person.Builder()
                        .setName("You")
                        .build()
                    val memberPerson = Person.Builder()
                        .setName(senderName?.takeIf { it.isNotBlank() } ?: title)
                        .setIcon(personIcon)
                        .build()

                    val cleanBody = if (!senderName.isNullOrBlank() && body.startsWith("$senderName: ")) {
                        body.removePrefix("$senderName: ")
                    } else {
                        body
                    }

                    val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
                        .setConversationTitle(title)
                        .setGroupConversation(true)
                        .addMessage(cleanBody, System.currentTimeMillis(), memberPerson)

                    builder
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setSubText("Group Chat")
                        .setGroup(GROUP_KEY_MESSAGES)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setStyle(messagingStyle)

                    if (!targetId.isNullOrBlank()) {
                        builder.addAction(
                            createInlineReplyAction(
                                context = context,
                                notificationId = notificationId,
                                targetType = "group",
                                targetId = targetId,
                                title = title,
                                channelId = channelId
                            )
                        )
                    }
                    builder.addAction(0, "Open Group", openPendingIntent)
                }

                else -> {
                    val mePerson = Person.Builder()
                        .setName("You")
                        .build()
                    val peerPerson = Person.Builder()
                        .setName(title)
                        .setIcon(personIcon)
                        .setImportant(true)
                        .build()

                    val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
                        .setGroupConversation(false)
                        .addMessage(body, System.currentTimeMillis(), peerPerson)

                    builder
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setSubText("Direct Message")
                        .setGroup(GROUP_KEY_MESSAGES)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setStyle(messagingStyle)

                    if (!targetId.isNullOrBlank()) {
                        builder.addAction(
                            createInlineReplyAction(
                                context = context,
                                notificationId = notificationId,
                                targetType = "chat",
                                targetId = targetId,
                                title = title,
                                channelId = channelId
                            )
                        )
                        builder.addAction(
                            createMarkReadAction(
                                context = context,
                                notificationId = notificationId,
                                targetId = targetId,
                                channelId = channelId
                            )
                        )
                    }
                }
            }

            runCatching {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        }

        private fun createInlineReplyAction(
            context: Context,
            notificationId: Int,
            targetType: String,
            targetId: String,
            title: String,
            channelId: String
        ): NotificationCompat.Action {
            val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
                .setLabel("Write a reply...")
                .build()

            val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REPLY
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(NotificationActionReceiver.EXTRA_TARGET_TYPE, targetType)
                putExtra(NotificationActionReceiver.EXTRA_TARGET_ID, targetId)
                putExtra(NotificationActionReceiver.EXTRA_TITLE, title)
                putExtra(NotificationActionReceiver.EXTRA_CHANNEL_ID, channelId)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 31 + 1,
                replyIntent,
                flags
            )

            return NotificationCompat.Action.Builder(0, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()
        }

        private fun createMarkReadAction(
            context: Context,
            notificationId: Int,
            targetId: String,
            channelId: String
        ): NotificationCompat.Action {
            val readIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(NotificationActionReceiver.EXTRA_TARGET_TYPE, "chat")
                putExtra(NotificationActionReceiver.EXTRA_TARGET_ID, targetId)
                putExtra(NotificationActionReceiver.EXTRA_CHANNEL_ID, channelId)
            }
            val readPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 31 + 2,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(0, "Mark as read", readPendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .build()
        }
    }
}
