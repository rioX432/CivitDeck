package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.download.DownloadScheduler

/**
 * No-op Desktop implementation.
 * Desktop does not have background download scheduling.
 */
class DesktopDownloadScheduler : DownloadScheduler {
    override fun enqueue(downloadId: Long) {
        // No-op: Desktop has no background download scheduling
    }

    override fun cancel(downloadId: Long) {
        // No-op: Desktop has no background download scheduling
    }
}
