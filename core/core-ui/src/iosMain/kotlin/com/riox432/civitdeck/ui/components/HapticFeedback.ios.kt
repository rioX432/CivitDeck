package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeSuccess
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeWarning
import platform.UIKit.UISelectionFeedbackGenerator

/**
 * iOS haptic feedback via UIKit feedback generators.
 *
 * SwiftUI screens trigger haptics natively, but the shared Compose UI (used when a screen is
 * rendered through Compose Multiplatform on iOS) routes through this actual. Feedback generators
 * must be invoked on the main thread; the returned lambda is only called from Compose event
 * handlers, which run on the main thread, so no explicit dispatch is required.
 */
@Composable
actual fun rememberHapticFeedback(): (HapticFeedbackType) -> Unit {
    val impactGenerator = remember { UIImpactFeedbackGenerator(style = UIImpactFeedbackStyleMedium) }
    val selectionGenerator = remember { UISelectionFeedbackGenerator() }
    val notificationGenerator = remember { UINotificationFeedbackGenerator() }
    return remember {
        {
                type: HapticFeedbackType ->
            when (type) {
                HapticFeedbackType.Impact -> {
                    impactGenerator.prepare()
                    impactGenerator.impactOccurred()
                }
                HapticFeedbackType.Selection -> {
                    selectionGenerator.prepare()
                    selectionGenerator.selectionChanged()
                }
                HapticFeedbackType.Success -> {
                    notificationGenerator.prepare()
                    notificationGenerator.notificationOccurred(UINotificationFeedbackTypeSuccess)
                }
                HapticFeedbackType.Warning -> {
                    notificationGenerator.prepare()
                    notificationGenerator.notificationOccurred(UINotificationFeedbackTypeWarning)
                }
                HapticFeedbackType.Error -> {
                    notificationGenerator.prepare()
                    notificationGenerator.notificationOccurred(UINotificationFeedbackTypeError)
                }
            }
        }
    }
}
