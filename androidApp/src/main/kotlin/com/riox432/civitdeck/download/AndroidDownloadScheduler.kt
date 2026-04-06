package com.riox432.civitdeck.download

import android.content.Context
import com.riox432.civitdeck.domain.download.DownloadScheduler as DownloadSchedulerInterface

/**
 * Android implementation wrapping the existing WorkManager-based [DownloadScheduler] object.
 */
class AndroidDownloadScheduler(private val context: Context) : DownloadSchedulerInterface {

    override fun enqueue(downloadId: Long) {
        DownloadScheduler.enqueue(context, downloadId)
    }

    override fun cancel(downloadId: Long) {
        DownloadScheduler.cancel(context, downloadId)
    }
}
