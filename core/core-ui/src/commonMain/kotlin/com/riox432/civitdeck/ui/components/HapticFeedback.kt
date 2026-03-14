package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable
import com.riox432.civitdeck.domain.model.HapticFeedbackType

/**
 * Provides a performHaptic function that triggers platform-appropriate haptic feedback.
 * On Android, uses View.performHapticFeedback(). On other platforms, returns a no-op.
 */
@Composable
expect fun rememberHapticFeedback(): (HapticFeedbackType) -> Unit
