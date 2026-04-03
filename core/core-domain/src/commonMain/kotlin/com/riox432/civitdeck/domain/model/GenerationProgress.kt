package com.riox432.civitdeck.domain.model

/**
 * Real-time progress snapshot emitted during a ComfyUI generation via WebSocket.
 */
data class GenerationProgress(
    val promptId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val currentNode: String = "",
    /** Preview image bytes from WebSocket binary frames (JPEG/PNG). Null when no preview available. */
    val previewImageBytes: ByteArray? = null,
) {
    /** Progress fraction in [0.0, 1.0]. Returns 0 when totalSteps is unknown. */
    val fraction: Float
        get() = if (totalSteps > 0) currentStep.toFloat() / totalSteps.toFloat() else 0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GenerationProgress) return false
        return promptId == other.promptId &&
            currentStep == other.currentStep &&
            totalSteps == other.totalSteps &&
            currentNode == other.currentNode &&
            previewImageBytes.contentEquals(other.previewImageBytes)
    }

    override fun hashCode(): Int {
        var result = promptId.hashCode()
        result = 31 * result + currentStep
        result = 31 * result + totalSteps
        result = 31 * result + currentNode.hashCode()
        result = 31 * result + (previewImageBytes?.contentHashCode() ?: 0)
        return result
    }
}

private fun ByteArray?.contentEquals(other: ByteArray?): Boolean {
    if (this === other) return true
    if (this == null || other == null) return this == other
    return this.contentEquals(other)
}
