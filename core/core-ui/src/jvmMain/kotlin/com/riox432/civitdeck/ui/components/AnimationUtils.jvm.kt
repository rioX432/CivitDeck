package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun isReducedMotionEnabled(): Boolean {
    // Desktop: always return false (animations enabled)
    return false
}
