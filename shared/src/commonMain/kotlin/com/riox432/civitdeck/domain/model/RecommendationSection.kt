package com.riox432.civitdeck.domain.model

data class RecommendationSection(
    val title: String,
    val reason: String,
    val models: List<Model>,
)
