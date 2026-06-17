package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.domain.model.SystemStats

/**
 * An actionable optimization suggestion based on the user's hardware.
 */
data class OptimizationSuggestion(
    val id: String,
    val title: String,
    val description: String,
)

private const val LOW_VRAM_THRESHOLD_MB = 6 * 1024L
private const val MID_VRAM_THRESHOLD_MB = 8 * 1024L
private const val LOW_FREE_RATIO = 0.15

/**
 * Generates optimization suggestions based on the connected ComfyUI server's
 * hardware capabilities.
 */
fun generateOptimizationSuggestions(stats: SystemStats): List<OptimizationSuggestion> {
    val suggestions = mutableListOf<OptimizationSuggestion>()

    if (stats.vramTotalMB in 1 until LOW_VRAM_THRESHOLD_MB) {
        suggestions.add(
            OptimizationSuggestion(
                id = "fp16_tiling",
                title = "Enable FP16 + Tiling",
                description = "With ${stats.vramTotalMB} MB VRAM, enable FP16 precision " +
                    "and tiled VAE decode to reduce memory usage.",
            ),
        )
    } else if (stats.vramTotalMB in LOW_VRAM_THRESHOLD_MB until MID_VRAM_THRESHOLD_MB) {
        suggestions.add(
            OptimizationSuggestion(
                id = "fp16",
                title = "Use FP16 Precision",
                description = "With ${stats.vramTotalMB} MB VRAM, FP16 models will give " +
                    "the best balance of quality and performance.",
            ),
        )
    }

    if (stats.vramTotalMB > 0) {
        val freeRatio = stats.vramFreeMB.toDouble() / stats.vramTotalMB
        if (freeRatio < LOW_FREE_RATIO) {
            suggestions.add(
                OptimizationSuggestion(
                    id = "low_free_vram",
                    title = "Low Free VRAM",
                    description = "Only ${stats.vramFreeMB} MB free of ${stats.vramTotalMB} MB. " +
                        "Close GPU-intensive apps or use ComfyUI's /free endpoint to release cached models.",
                ),
            )
        }
    }

    return suggestions
}
