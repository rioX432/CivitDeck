package com.riox432.civitdeck.feature.search.presentation

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod

/** Why a search result set is not exactly what the active filters asked for, for one provider. */
enum class SearchSourceNoticeReason {
    /** The provider's request failed; results from the other providers are still shown. */
    SOURCE_FAILED,

    /** The provider keeps its own order; only Most Downloaded matches it. */
    UNSUPPORTED_SORT,
    UNSUPPORTED_PERIOD,
    UNSUPPORTED_BASE_MODEL,
    UNSUPPORTED_FRESH_FIND,
    UNSUPPORTED_QUALITY_FILTER,

    /** The provider does not rate its previews, so they are hidden under the current NSFW level. */
    UNRATED_PREVIEWS_HIDDEN,
}

/** A provider-specific limitation of the current search. Platforms turn [reason] into text. */
data class SearchSourceNotice(
    val source: ModelSource,
    val reason: SearchSourceNoticeReason,
)

/**
 * What each non-Civitai provider can honor. Civitai receives every filter in its request; the
 * other providers are queried by text only, so filters are applied to their mapped fields here.
 */
internal object SearchProviderPolicy {

    // Both external search APIs return results by download count.
    private val NATIVE_EXTERNAL_SORT = SortOrder.MostDownloaded

    // Hugging Face results carry no model versions, so there is no base model to check.
    private fun supportsBaseModel(source: ModelSource): Boolean = source != ModelSource.HUGGING_FACE

    /** Notices for every selected external provider that cannot honor an active filter. */
    fun capabilityNotices(filter: FilterState): List<SearchSourceNotice> =
        filter.selectedSources
            .filter { it != ModelSource.CIVITAI }
            .sortedBy { it.ordinal }
            .flatMap { source -> unsupportedReasons(source, filter).map { SearchSourceNotice(source, it) } }

    private fun unsupportedReasons(source: ModelSource, filter: FilterState): List<SearchSourceNoticeReason> =
        buildList {
            if (filter.selectedSort != NATIVE_EXTERNAL_SORT) add(SearchSourceNoticeReason.UNSUPPORTED_SORT)
            if (filter.selectedPeriod != TimePeriod.AllTime) add(SearchSourceNoticeReason.UNSUPPORTED_PERIOD)
            if (filter.selectedBaseModels.isNotEmpty() && !supportsBaseModel(source)) {
                add(SearchSourceNoticeReason.UNSUPPORTED_BASE_MODEL)
            }
            if (filter.isFreshFindEnabled) add(SearchSourceNoticeReason.UNSUPPORTED_FRESH_FIND)
            if (filter.isQualityFilterActive()) add(SearchSourceNoticeReason.UNSUPPORTED_QUALITY_FILTER)
            if (filter.nsfwFilterLevel != NsfwFilterLevel.All) add(SearchSourceNoticeReason.UNRATED_PREVIEWS_HIDDEN)
        }

    /**
     * Applies the filters an external provider's mapped fields can answer, and hides its previews
     * unless every NSFW level is allowed: no external provider rates its images, so a preview's
     * level is unknown, never verified safe. The model itself stays so it shows a placeholder.
     *
     * Hidden and viewed IDs are Civitai IDs; an external model's numeric ID is synthetic and is
     * never compared with them.
     */
    fun applyExternalFilters(models: List<Model>, filter: FilterState): List<Model> {
        val included = filter.includedTags.map { it.lowercase() }.toSet()
        val excluded = filter.excludedTags.map { it.lowercase() }.toSet()
        val baseModels = filter.selectedBaseModels.map { it.apiValue.lowercase() }.toSet()
        return models
            .filter { model ->
                val tags = model.tags.map { it.lowercase() }.toSet()
                (filter.selectedType == null || model.type == filter.selectedType) &&
                    tags.containsAll(included) &&
                    tags.none { it in excluded } &&
                    (baseModels.isEmpty() || !supportsBaseModel(model.source) || model.matchesBaseModel(baseModels))
            }
            .map { model -> if (filter.nsfwFilterLevel == NsfwFilterLevel.All) model else model.withoutPreviews() }
    }

    private fun Model.matchesBaseModel(baseModels: Set<String>): Boolean =
        modelVersions.any { version -> version.baseModel?.lowercase() in baseModels }

    private fun Model.withoutPreviews(): Model =
        copy(modelVersions = modelVersions.map { it.copy(images = emptyList()) })
}

internal fun FilterState.isQualityFilterActive(): Boolean = isQualityFilterEnabled && qualityThreshold > 0
