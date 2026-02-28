package com.riox432.civitdeck.domain.model

/**
 * Curated accent color palette for theme customization.
 * Each entry has a display name and light/dark hex seed color.
 */
enum class AccentColor(val displayName: String, val seedHex: Long) {
    Blue("Blue", 0xFF1E40AF),
    Indigo("Indigo", 0xFF4338CA),
    Purple("Purple", 0xFF7C3AED),
    Pink("Pink", 0xFFDB2777),
    Red("Red", 0xFFDC2626),
    Orange("Orange", 0xFFEA580C),
    Amber("Amber", 0xFFD97706),
    Green("Green", 0xFF16A34A),
    Teal("Teal", 0xFF0D9488),
    Cyan("Cyan", 0xFF0891B2),
}
