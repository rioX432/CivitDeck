package com.riox432.civitdeck.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring as SpringSpec

// Duration tokens (ms)
object Duration {
    const val fast = 150
    const val normal = 300
    const val slow = 500
}

// Easing tokens
object Easing {
    val standard = FastOutSlowInEasing // Material standard
    val decelerate = LinearOutSlowInEasing // entering elements
    val accelerate = FastOutLinearInEasing // exiting elements
}

// Spring tokens
object Spring {
    val default = spring<Float>(
        dampingRatio = SpringSpec.DampingRatioNoBouncy,
        stiffness = SpringSpec.StiffnessMedium,
    )
    val bouncy = spring<Float>(
        dampingRatio = SpringSpec.DampingRatioMediumBouncy,
        stiffness = SpringSpec.StiffnessMedium,
    )
    val stiff = spring<Float>(
        dampingRatio = SpringSpec.DampingRatioNoBouncy,
        stiffness = SpringSpec.StiffnessHigh,
    )
}

// Tween helper
object Tween {
    fun fast(easing: androidx.compose.animation.core.Easing = Easing.standard) =
        tween<Float>(Duration.fast, easing = easing)
    fun normal(easing: androidx.compose.animation.core.Easing = Easing.standard) =
        tween<Float>(Duration.normal, easing = easing)
    fun slow(easing: androidx.compose.animation.core.Easing = Easing.standard) =
        tween<Float>(Duration.slow, easing = easing)
}
