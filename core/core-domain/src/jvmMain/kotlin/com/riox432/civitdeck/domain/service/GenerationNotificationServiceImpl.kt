package com.riox432.civitdeck.domain.service

import com.riox432.civitdeck.util.Logger

/**
 * JVM/Desktop no-op implementation. Desktop apps are always in the foreground,
 * so system-level notifications are not needed.
 */
class GenerationNotificationServiceImpl : GenerationNotificationService {

    override fun notifyGenerationComplete(promptId: String, imageCount: Int, elapsedMs: Long) {
        Logger.d(TAG, "Generation complete: promptId=$promptId, images=$imageCount, elapsed=${elapsedMs}ms")
    }

    override fun notifyGenerationError(promptId: String, errorMessage: String) {
        Logger.d(TAG, "Generation error: promptId=$promptId, error=$errorMessage")
    }

    private companion object {
        private const val TAG = "GenerationNotification"
    }
}
