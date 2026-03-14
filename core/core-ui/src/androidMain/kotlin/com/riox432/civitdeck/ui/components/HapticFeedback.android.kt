package com.riox432.civitdeck.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.riox432.civitdeck.domain.model.HapticFeedbackType

@Composable
actual fun rememberHapticFeedback(): (HapticFeedbackType) -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
                type: HapticFeedbackType ->
            view.performHapticFeedback(type.toHapticConstant())
        }
    }
}

private fun HapticFeedbackType.toHapticConstant(): Int = when (this) {
    HapticFeedbackType.Impact -> HapticFeedbackConstants.CONTEXT_CLICK
    HapticFeedbackType.Selection ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
    HapticFeedbackType.Success ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
    HapticFeedbackType.Warning ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.GESTURE_START
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    HapticFeedbackType.Error ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
}
