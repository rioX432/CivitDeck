package com.riox432.civitdeck.domain.model

import kotlin.math.roundToInt

data class CacheInfo(
    val sizeBytes: Long,
    val entryCount: Int,
) {
    val sizeMb: Double
        get() = sizeBytes.toDouble() / (1024.0 * 1024.0)

    val formattedSize: String
        get() {
            return when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> {
                    val rounded = (sizeMb * 10).roundToInt() / 10.0
                    val text = rounded.toString()
                    // Ensure exactly one decimal place
                    if ('.' !in text) {
                        "$text.0 MB"
                    } else {
                        "$text MB"
                    }
                }
            }
        }
}
