package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun isReducedMotionEnabled(): Boolean {
    // Stub: always return false on iOS (animations enabled)
    return false
}
