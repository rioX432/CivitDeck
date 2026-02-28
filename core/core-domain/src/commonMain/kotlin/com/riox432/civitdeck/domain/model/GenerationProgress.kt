package com.riox432.civitdeck.domain.model

/**
 * Real-time progress snapshot emitted during a ComfyUI generation via WebSocket.
 */
data class GenerationProgress(
    val promptId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val currentNode: String = "",
) {
    /** Progress fraction in [0.0, 1.0]. Returns 0 when totalSteps is unknown. */
    val fraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f
}
