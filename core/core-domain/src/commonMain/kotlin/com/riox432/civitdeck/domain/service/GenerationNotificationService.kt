package com.riox432.civitdeck.domain.service

/**
 * Platform-specific local notification service for ComfyUI generation events.
 * Fires heads-up / banner notifications when generation completes or fails
 * while the app is in the background.
 */
interface GenerationNotificationService {
    /**
     * Show a "generation complete" local notification.
     *
     * @param promptId the ComfyUI prompt ID
     * @param imageCount number of generated images
     * @param elapsedMs wall-clock time from submission to completion
     */
    fun notifyGenerationComplete(promptId: String, imageCount: Int, elapsedMs: Long)

    /**
     * Show a "generation failed" local notification.
     *
     * @param promptId the ComfyUI prompt ID
     * @param errorMessage human-readable error description
     */
    fun notifyGenerationError(promptId: String, errorMessage: String)
}
