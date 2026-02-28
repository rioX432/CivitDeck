package com.riox432.civitdeck.ui.tutorial

import androidx.compose.ui.graphics.Color
import com.riox432.civitdeck.ui.theme.CivitDeckColors

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
        accentColor = CivitDeckColors.tutorialAccentBlue,
    ),
    TutorialStep(
        title = "Quick Actions",
        description = "Swipe a model card to reveal quick actions like " +
            "favorite, download, or hide.",
        accentColor = CivitDeckColors.tutorialAccentPink,
    ),
    TutorialStep(
        title = "Image Comparison",
        description = "Drag the slider on image comparisons to reveal " +
            "before and after results side by side.",
        accentColor = CivitDeckColors.tutorialAccentGray,
    ),
)
