package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.di.AppContainer
import com.example.notifications.IncomingMessageSyncWorker
import com.example.notifications.ManojSonaliMessagingService
import com.example.notifications.StatusExpiryCleanupWorker
import com.example.utils.DataUriFetcher

class ManojSonaliApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        ManojSonaliMessagingService.createChannels(applicationContext)
        StatusExpiryCleanupWorker.schedule(applicationContext)
        IncomingMessageSyncWorker.schedule(applicationContext)
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
