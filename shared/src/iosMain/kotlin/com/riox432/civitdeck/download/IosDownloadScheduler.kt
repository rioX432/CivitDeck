package com.riox432.civitdeck.download

import com.riox432.civitdeck.domain.download.DownloadScheduler

/**
 * No-op iOS implementation.
 * iOS handles download scheduling independently via DownloadService.swift.
 */
class IosDownloadScheduler : DownloadScheduler {
    override fun enqueue(downloadId: Long) {
        // No-op: iOS DownloadService.swift handles scheduling
    }

    override fun cancel(downloadId: Long) {
        // No-op: iOS DownloadService.swift handles cancellation
    }
}
