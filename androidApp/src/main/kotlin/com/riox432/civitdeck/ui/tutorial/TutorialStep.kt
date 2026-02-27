package com.riox432.civitdeck.ui.tutorial

import androidx.compose.ui.graphics.Color

/**
 * Represents a single step in the gesture tutorial walkthrough.
 */
data class TutorialStep(
    val title: String,
    val description: String,
    val accentColor: Color,
)

val tutorialSteps = listOf(
    TutorialStep(
        title = "Swipe to Discover",
        description = "Swipe left or right on model cards to discover new models. " +
            "Swipe up to skip a model.",
        accentColor = Color(0xFF3755C3),
    ),
    TutorialStep(
        title = "Quick Actions",
        description = "Swipe a model card to reveal quick actions like " +
            "favorite, download, or hide.",
        accentColor = Color(0xFF75546F),
    ),
    TutorialStep(
        title = "Image Comparison",
        description = "Drag the slider on image comparisons to reveal " +
            "before and after results side by side.",
        accentColor = Color(0xFF5A5D72),
    ),
)
