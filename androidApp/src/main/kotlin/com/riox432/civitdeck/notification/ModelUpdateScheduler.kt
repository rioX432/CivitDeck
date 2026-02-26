package com.riox432.civitdeck.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.riox432.civitdeck.domain.model.PollingInterval
import java.util.concurrent.TimeUnit

object ModelUpdateScheduler {

    fun schedule(context: Context, interval: PollingInterval) {
        val workManager = WorkManager.getInstance(context)

        if (interval == PollingInterval.Off) {
            workManager.cancelUniqueWork(ModelUpdateWorker.WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ModelUpdateWorker>(
            interval.minutes.toLong(),
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ModelUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ModelUpdateWorker.WORK_NAME)
    }
}
