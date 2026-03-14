package com.riox432.civitdeck.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.riox432.civitdeck.domain.model.Model

/**
 * Android SwipeableModelCard — wraps [ModelCard] in a [SwipeableCardLayout]
 * with platform haptic feedback.
 */
@Composable
@Suppress("LongParameterList")
fun SwipeableModelCard(
    model: Model,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onHide: () -> Unit,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isOwned: Boolean = false,
    swipeThreshold: Float = 0.3f,
) {
    val haptic = LocalHapticFeedback.current

    SwipeableCardLayout(
        isFavorite = isFavorite,
        onFavoriteToggle = onFavoriteToggle,
        onHide = onHide,
        modifier = modifier,
        swipeThreshold = swipeThreshold,
        onHapticFeedback = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
    ) {
        ModelCard(
            model = model,
            onClick = onClick,
            onLongPress = onLongPress,
            isOwned = isOwned,
        )
    }
}
