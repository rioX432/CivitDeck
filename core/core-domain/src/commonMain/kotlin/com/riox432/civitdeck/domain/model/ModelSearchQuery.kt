package com.riox432.civitdeck.domain.model

/**
 * Encapsulates all query parameters for searching models.
 * Used by [com.riox432.civitdeck.domain.repository.ModelRepository] and callers.
 */
data class ModelSearchQuery(
    val query: String? = null,
    val tag: String? = null,
    val type: ModelType? = null,
    val sort: SortOrder? = null,
    val period: TimePeriod? = null,
    val baseModels: List<BaseModel>? = null,
    val cursor: String? = null,
    val limit: Int? = null,
    val username: String? = null,
    val nsfw: Boolean? = null,
)
