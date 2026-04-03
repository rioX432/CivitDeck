package com.riox432.civitdeck.domain.model

data class RecommendationSection(
    val title: String,
    val reason: String,
    val models: List<Model>,
    val sectionType: RecommendationSectionType = RecommendationSectionType.PERSONALIZED,
)

enum class RecommendationSectionType {
    PERSONALIZED,
    TRENDING,
    EXPLORATION,
}
