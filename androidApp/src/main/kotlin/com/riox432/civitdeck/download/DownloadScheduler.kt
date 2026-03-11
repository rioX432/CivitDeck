package com.riox432.civitdeck.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object DownloadScheduler {

    fun enqueue(context: Context, downloadId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(ModelDownloadWorker.KEY_DOWNLOAD_ID to downloadId))
            .addTag(tagForDownload(downloadId))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(context: Context, downloadId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagForDownload(downloadId))
    }

    private fun tagForDownload(downloadId: Long): String = "download_$downloadId"
}
