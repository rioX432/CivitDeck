package com.riox432.civitdeck.ui.compare

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * A clip shape that reveals a fraction of the content based on slider position.
 *
 * For [SliderOrientation.Horizontal], clips from the left edge to [fraction] of the width.
 * For [SliderOrientation.Vertical], clips from the top edge to [fraction] of the height.
 */
internal class SliderClipShape(
    private val fraction: Float,
    private val orientation: SliderOrientation,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rect = when (orientation) {
            SliderOrientation.Horizontal -> androidx.compose.ui.geometry.Rect(
                left = 0f,
                top = 0f,
                right = size.width * fraction,
                bottom = size.height,
            )
            SliderOrientation.Vertical -> androidx.compose.ui.geometry.Rect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height * fraction,
            )
        }
        return Outline.Rectangle(rect)
    }
}
