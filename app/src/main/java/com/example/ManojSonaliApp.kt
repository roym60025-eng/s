package com.example

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.di.AppContainer
import com.example.notifications.BackgroundNotificationService
import com.example.notifications.IncomingMessageSyncWorker
import com.example.notifications.ManojSonaliMessagingService
import com.example.notifications.StatusExpiryCleanupWorker
import com.example.utils.DataUriFetcher
import com.google.firebase.auth.FirebaseAuth

class ManojSonaliApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            container.notificationMonitor.start(user.uid)
        } else {
            container.notificationMonitor.stop()
            BackgroundNotificationService.stop(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        registerNotificationChannels()
        StatusExpiryCleanupWorker.schedule(applicationContext)
        IncomingMessageSyncWorker.schedule(applicationContext)
        container.auth.addAuthStateListener(authStateListener)
    }

    private fun registerNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            val incomingMessagesChannel = NotificationChannel(
                ManojSonaliMessagingService.CHANNEL_MESSAGES,
                "Incoming Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notifications for incoming direct messages"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(incomingMessagesChannel)
        }
        ManojSonaliMessagingService.createChannels(applicationContext)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(DataUriFetcher.Factory())
            }
            .crossfade(true)
            .build()
    }
}
