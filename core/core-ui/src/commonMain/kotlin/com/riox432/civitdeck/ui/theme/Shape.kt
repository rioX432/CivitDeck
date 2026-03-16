package com.riox432.civitdeck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CivitDeckShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object CornerRadius {
    val xs = 4.dp
    val card = 12.dp
    const val chip = 50
    val image = 8.dp
    val searchBar = 8.dp
}

object Elevation {
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
}

object IconSize {
    val statIcon = 10.dp
    val errorPlaceholder = 36.dp
    val navBar = 24.dp
}
