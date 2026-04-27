package com.riox432.civitdeck.feature.comfyui.domain.model

/**
 * Represents a single continuous drawing stroke on the mask canvas.
 * Each segment is a list of (x, y) points normalized to [0, 1] range
 * relative to the canvas size.
 */
data class PathSegment(
    val points: List<Pair<Float, Float>>,
    val isEraser: Boolean = false,
    val brushSize: Float = DEFAULT_BRUSH_SIZE,
) {
    companion object {
        const val DEFAULT_BRUSH_SIZE = 40f
        const val MIN_BRUSH_SIZE = 5f
        const val MAX_BRUSH_SIZE = 150f
    }
}
