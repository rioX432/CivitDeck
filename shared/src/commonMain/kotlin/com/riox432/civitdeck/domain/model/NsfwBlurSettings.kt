package com.riox432.civitdeck.domain.model

/**
 * Per-NSFW-level blur intensity settings.
 * Each value is 0..100 representing blur percentage.
 * 0 = completely hidden (no image shown), 25/50/75/100 = visible with decreasing blur.
 */
data class NsfwBlurSettings(
    val softIntensity: Int = DEFAULT_SOFT,
    val matureIntensity: Int = DEFAULT_MATURE,
    val explicitIntensity: Int = DEFAULT_EXPLICIT,
) {
    /**
     * Returns blur radius in dp for the given NSFW level.
     * 0% intensity -> MAX_BLUR_RADIUS (fully blurred / hidden)
     * 100% intensity -> 0 (no blur, fully visible)
     */
    fun blurRadiusFor(level: NsfwLevel): Float = when (level) {
        NsfwLevel.None -> 0f
        NsfwLevel.Soft -> intensityToRadius(softIntensity)
        NsfwLevel.Mature -> intensityToRadius(matureIntensity)
        NsfwLevel.X -> intensityToRadius(explicitIntensity)
    }

    /**
     * Returns whether the image should be shown at all for the given level.
     * An intensity of 0 means "hidden" (don't show the image).
     */
    fun isVisibleFor(level: NsfwLevel): Boolean = when (level) {
        NsfwLevel.None -> true
        NsfwLevel.Soft -> softIntensity > 0
        NsfwLevel.Mature -> matureIntensity > 0
        NsfwLevel.X -> explicitIntensity > 0
    }

    companion object {
        const val DEFAULT_SOFT = 75
        const val DEFAULT_MATURE = 25
        const val DEFAULT_EXPLICIT = 0
        const val MAX_BLUR_RADIUS = 30f
        const val MIN_INTENSITY = 0
        const val MAX_INTENSITY = 100
    }
}

private fun intensityToRadius(intensity: Int): Float {
    // intensity 0 -> hidden (handled by isVisibleFor)
    // intensity 100 -> 0 blur radius
    // intensity 25 -> 22.5 blur radius
    val clamped = intensity.coerceIn(NsfwBlurSettings.MIN_INTENSITY, NsfwBlurSettings.MAX_INTENSITY)
    return NsfwBlurSettings.MAX_BLUR_RADIUS * (1f - clamped / 100f)
}
