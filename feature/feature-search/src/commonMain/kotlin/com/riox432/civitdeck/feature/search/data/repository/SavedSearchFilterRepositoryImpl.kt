package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.dao.SavedSearchFilterDao
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.SavedSearchFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedSearchFilterRepositoryImpl(
    private val dao: SavedSearchFilterDao,
) : SavedSearchFilterRepository {

    override fun observeAll(): Flow<List<SavedSearchFilter>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(filter: SavedSearchFilter): Long = dao.insert(filter.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    private fun SavedSearchFilterEntity.toDomain() = SavedSearchFilter(
        id = id,
        name = name,
        query = query,
        selectedType = selectedType?.let {
            runCatching { ModelType.valueOf(it) }.getOrNull()
        },
        selectedSort = runCatching { SortOrder.valueOf(selectedSort) }
            .getOrElse { SortOrder.MostDownloaded },
        selectedPeriod = runCatching { TimePeriod.valueOf(selectedPeriod) }
            .getOrElse { TimePeriod.AllTime },
        selectedBaseModels = if (selectedBaseModels.isBlank()) {
            emptySet()
        } else {
            selectedBaseModels.split(",").mapNotNull { apiValue ->
                BaseModel.entries.find { it.apiValue == apiValue }
            }.toSet()
        },
        nsfwFilterLevel = runCatching { NsfwFilterLevel.valueOf(nsfwFilterLevel) }
            .getOrElse { NsfwFilterLevel.Off },
        isFreshFindEnabled = isFreshFindEnabled != 0,
        excludedTags = if (excludedTags.isBlank()) emptyList() else excludedTags.split("\n"),
        includedTags = if (includedTags.isBlank()) emptyList() else includedTags.split("\n"),
        savedAt = savedAt,
    )

    private fun SavedSearchFilter.toEntity() = SavedSearchFilterEntity(
        id = id,
        name = name,
        query = query,
        selectedType = selectedType?.name,
        selectedSort = selectedSort.name,
        selectedPeriod = selectedPeriod.name,
        selectedBaseModels = selectedBaseModels.joinToString(",") { it.apiValue },
        nsfwFilterLevel = nsfwFilterLevel.name,
        isFreshFindEnabled = if (isFreshFindEnabled) 1 else 0,
        excludedTags = excludedTags.joinToString("\n"),
        includedTags = includedTags.joinToString("\n"),
        savedAt = savedAt,
    )
}
