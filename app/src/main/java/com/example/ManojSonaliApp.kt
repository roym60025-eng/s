package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.notifications.ManojSonaliMessagingService
import com.example.notifications.StatusExpiryCleanupWorker

class ManojSonaliApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
        ManojSonaliMessagingService.createChannels(applicationContext)
        StatusExpiryCleanupWorker.schedule(applicationContext)
    }
}
