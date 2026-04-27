package com.riox432.civitdeck.feature.comfyui.domain.model

/**
 * Immutable state for the mask painting editor.
 * Tracks drawn path segments and undo/redo history.
 */
data class MaskEditorState(
    /** All committed path segments forming the current mask. */
    val pathSegments: List<PathSegment> = emptyList(),
    /** Segments removed by undo, available for redo. */
    val redoStack: List<PathSegment> = emptyList(),
    /** Current brush size in dp. */
    val brushSize: Float = PathSegment.DEFAULT_BRUSH_SIZE,
    /** Whether eraser mode is active. */
    val isEraserMode: Boolean = false,
    /** Whether the mask has been inverted. */
    val isInverted: Boolean = false,
    /** Source image URL (the image being masked). */
    val sourceImageUrl: String? = null,
    /** Whether mask upload is in progress. */
    val isUploading: Boolean = false,
    /** Upload error message. */
    val uploadError: String? = null,
    /** Uploaded mask filename on the ComfyUI server (e.g. "mask_12345.png"). */
    val uploadedMaskFilename: String? = null,
) {
    val canUndo: Boolean get() = pathSegments.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val hasContent: Boolean get() = pathSegments.isNotEmpty()
}
