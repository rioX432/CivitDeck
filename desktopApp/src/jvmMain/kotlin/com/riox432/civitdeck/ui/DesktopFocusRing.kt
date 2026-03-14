package com.riox432.civitdeck.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

/**
 * Modifier that adds a visible focus ring when the element is focused via keyboard (Tab navigation).
 * Uses the primary color with a rounded border to indicate focus state.
 */
fun Modifier.desktopFocusRing(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = MaterialTheme.colorScheme.primary

    this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) {
                Modifier.border(
                    width = FOCUS_RING_WIDTH,
                    color = borderColor,
                    shape = RoundedCornerShape(FOCUS_RING_CORNER),
                )
            } else {
                Modifier
            }
        )
}

private val FOCUS_RING_WIDTH = 2.dp
private val FOCUS_RING_CORNER = 8.dp
