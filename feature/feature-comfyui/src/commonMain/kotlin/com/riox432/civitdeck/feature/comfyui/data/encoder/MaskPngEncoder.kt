package com.riox432.civitdeck.feature.comfyui.data.encoder

/**
 * Platform-specific implementation for encoding mask path data into a PNG byte array.
 * The output is a white-on-black mask image at the specified resolution:
 * - White (255) = masked area (region to inpaint)
 * - Black (0) = unmasked area (preserve original)
 *
 * When [inverted] is true, the colors are swapped.
 */
expect class MaskPngEncoder() {
    /**
     * Renders the given path segments into a PNG mask image.
     *
     * @param segments list of drawing segments with normalized [0,1] coordinates
     * @param width output image width in pixels
     * @param height output image height in pixels
     * @param inverted if true, swap black and white regions
     * @return PNG-encoded bytes of the mask image
     */
    fun encode(
        segments: List<com.riox432.civitdeck.feature.comfyui.domain.model.PathSegment>,
        width: Int,
        height: Int,
        inverted: Boolean = false,
    ): ByteArray
}
