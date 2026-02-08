package com.riox432.civitdeck.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

object SharedElementKeys {
    fun modelThumbnail(modelId: Long, suffix: String = ""): String =
        if (suffix.isEmpty()) {
            "model_thumbnail_$modelId"
        } else {
            "model_thumbnail_${modelId}_$suffix"
        }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
