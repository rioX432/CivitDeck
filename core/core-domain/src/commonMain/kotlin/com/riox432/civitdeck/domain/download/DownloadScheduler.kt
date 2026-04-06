package com.riox432.civitdeck.domain.download

/**
 * Platform-abstracted download scheduling.
 * Android: wraps WorkManager-based scheduling.
 * iOS/Desktop: no-op (platform handles scheduling independently).
 */
interface DownloadScheduler {
    fun enqueue(downloadId: Long)
    fun cancel(downloadId: Long)
}
