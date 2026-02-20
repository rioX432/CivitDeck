package com.riox432.civitdeck.ui.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

private const val BASE_COLUMNS = 2
private const val MEDIUM_BONUS = 1
private const val EXPANDED_BONUS = 2

@Composable
fun adaptiveGridColumns(userPreference: Int): Int {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val bonus = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> EXPANDED_BONUS
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> MEDIUM_BONUS
        else -> 0
    }
    return userPreference + bonus
}

@Composable
fun adaptiveGridColumns(): Int = adaptiveGridColumns(BASE_COLUMNS)

@Composable
fun isExpandedWidth(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
}

@Composable
fun isTableTopPosture(): Boolean {
    val posture = currentWindowAdaptiveInfo().windowPosture
    return posture.isTabletop
}
