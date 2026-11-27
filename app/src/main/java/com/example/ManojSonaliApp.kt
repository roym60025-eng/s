package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.di.AppContainer
import com.example.notifications.BackgroundNotificationService
import com.example.notifications.IncomingMessageSyncWorker
import com.example.notifications.ManojSonaliMessagingService
import com.example.utils.DataUriFetcher
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ManojSonaliApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        appScope.launch {
            ManojSonaliMessagingService.createChannels(applicationContext)
            IncomingMessageSyncWorker.schedule(applicationContext)
        }
        container.auth.addAuthStateListener(authStateListener)
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
