package com.omooooori.civitdeck.ui.util

import java.util.Locale

object FormatUtils {
    fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }

    fun formatRating(rating: Double): String =
        String.format(Locale.US, "%.1f", rating)

    fun formatFileSize(sizeKB: Double): String = when {
        sizeKB >= 1_000_000 -> String.format(Locale.US, "%.1f GB", sizeKB / 1_000_000.0)
        sizeKB >= 1_000 -> String.format(Locale.US, "%.1f MB", sizeKB / 1_000.0)
        else -> String.format(Locale.US, "%.0f KB", sizeKB)
    }
}
