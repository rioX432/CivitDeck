package com.riox432.civitdeck.domain.util

/**
 * Indicates how well a model file fits the user's GPU VRAM.
 */
enum class VramCompatibility {
    /** Model file fits in available VRAM with comfortable margin (>= 2 GB spare). */
    FITS,

    /** Model file fits but within the 2 GB safety margin. */
    TIGHT,

    /** Model file exceeds available VRAM; CPU offloading may be required. */
    NEEDS_OFFLOADING,

    /** No system stats available — cannot determine compatibility. */
    UNKNOWN,
}

private const val KB_PER_MB = 1024.0
private const val MARGIN_MB = 2048L

/**
 * Estimates VRAM compatibility for a model file.
 *
 * Heuristic: the VRAM needed to load a model is roughly proportional to its file
 * size on disk. FP16/FP32 precision affects the actual runtime memory, but the
 * file size already reflects the stored precision. We use total VRAM (not free)
 * because the baseline VRAM usage of the inference runtime (~300-500 MB) is
 * negligible compared to model weights.
 *
 * @param modelFileSizeKB file size in kilobytes (from CivitAI `sizeKB` field)
 * @param vramTotalMB     total GPU VRAM in megabytes
 */
fun calculateVramCompatibility(
    modelFileSizeKB: Double,
    vramTotalMB: Long,
): VramCompatibility {
    if (vramTotalMB <= 0) return VramCompatibility.UNKNOWN
    val fileSizeMB = (modelFileSizeKB / KB_PER_MB).toLong()
    return when {
        fileSizeMB + MARGIN_MB <= vramTotalMB -> VramCompatibility.FITS
        fileSizeMB <= vramTotalMB -> VramCompatibility.TIGHT
        else -> VramCompatibility.NEEDS_OFFLOADING
    }
}
