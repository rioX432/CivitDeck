package com.riox432.civitdeck.util

import kotlin.math.round

object FormatUtils {
    fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${roundToOneDecimal(count / 1_000_000.0)}M"
        count >= 1_000 -> "${roundToOneDecimal(count / 1_000.0)}K"
        else -> count.toString()
    }

    fun formatRating(rating: Double): String = roundToOneDecimal(rating)

    fun formatFileSize(sizeKB: Double): String = when {
        sizeKB >= 1_000_000 -> "${roundToOneDecimal(sizeKB / 1_000_000.0)} GB"
        sizeKB >= 1_000 -> "${roundToOneDecimal(sizeKB / 1_000.0)} MB"
        else -> "${round(sizeKB).toInt()} KB"
    }

    private fun roundToOneDecimal(value: Double): String {
        val rounded = round(value * 10) / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            "${rounded.toLong()}.0"
        } else {
            rounded.toString()
        }
    }
}
