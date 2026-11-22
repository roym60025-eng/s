package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.ManojSonaliApp
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
