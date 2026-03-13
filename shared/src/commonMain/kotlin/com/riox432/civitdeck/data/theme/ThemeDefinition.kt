package com.riox432.civitdeck.data.theme

import kotlinx.serialization.Serializable

/**
 * JSON-serializable theme definition for importing custom themes.
 * Colors are hex strings (e.g. "#FF272822") parsed to ARGB Long values.
 */
@Serializable
data class ThemeDefinition(
    val id: String,
    val name: String,
    val author: String = "",
    val version: String = "1.0",
    val light: ThemeColors,
    val dark: ThemeColors,
)

@Serializable
data class ThemeColors(
    val primary: String,
    val onPrimary: String,
    val primaryContainer: String,
    val onPrimaryContainer: String,
    val secondary: String,
    val onSecondary: String,
    val secondaryContainer: String,
    val onSecondaryContainer: String,
    val tertiary: String,
    val onTertiary: String,
    val tertiaryContainer: String,
    val onTertiaryContainer: String,
    val background: String,
    val onBackground: String,
    val surface: String,
    val onSurface: String,
    val surfaceVariant: String,
    val onSurfaceVariant: String,
    val error: String,
    val onError: String,
    val errorContainer: String,
    val onErrorContainer: String,
    val outline: String,
    val outlineVariant: String,
)

/**
 * Parse a hex color string (e.g. "#FF272822" or "#272822") to an ARGB Long.
 * Supports 6-digit (RGB, alpha=FF) and 8-digit (ARGB) formats.
 */
fun parseHexColor(hex: String): Long {
    val cleaned = hex.removePrefix("#")
    return when (cleaned.length) {
        6 -> (0xFF000000L or cleaned.toLong(16))
        8 -> cleaned.toLong(16)
        else -> throw IllegalArgumentException("Invalid hex color: $hex")
    }
}
