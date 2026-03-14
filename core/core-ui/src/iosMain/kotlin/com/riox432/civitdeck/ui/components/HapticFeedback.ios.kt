package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable
import com.riox432.civitdeck.domain.model.HapticFeedbackType

@Composable
actual fun rememberHapticFeedback(): (HapticFeedbackType) -> Unit {
    // No-op on iOS — haptic feedback is handled natively in SwiftUI views
    return { _ -> }
}
