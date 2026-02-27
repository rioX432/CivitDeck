package com.riox432.civitdeck.domain.model

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
                else -> "%.1f MB".format(sizeMb)
            }
        }
}
