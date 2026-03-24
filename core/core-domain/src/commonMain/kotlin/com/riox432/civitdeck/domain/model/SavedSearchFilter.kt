package com.riox432.civitdeck.domain.model

data class SavedSearchFilter(
    val id: Long,
    val name: String,
    val query: String,
    val selectedType: ModelType?,
    val selectedSort: SortOrder,
    val selectedPeriod: TimePeriod,
    val selectedBaseModels: Set<BaseModel>,
    val nsfwFilterLevel: NsfwFilterLevel,
    val isFreshFindEnabled: Boolean,
    val excludedTags: List<String>,
    val includedTags: List<String>,
    val selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
    val savedAt: Long,
)
