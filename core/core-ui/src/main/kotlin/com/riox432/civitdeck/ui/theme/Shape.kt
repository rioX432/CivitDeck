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
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
}

object CornerRadius {
    val card = 12.dp
    const val chip = 50
    val image = 8.dp
    val searchBar = 8.dp
}

object IconSize {
    val statIcon = 10.dp
    val errorPlaceholder = 36.dp
    val navBar = 24.dp
}
