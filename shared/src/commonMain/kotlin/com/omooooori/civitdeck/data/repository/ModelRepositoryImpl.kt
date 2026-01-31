package com.omooooori.civitdeck.data.repository

import com.omooooori.civitdeck.data.api.CivitAiApi
import com.omooooori.civitdeck.data.api.dto.toDomain
import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.ModelType
import com.omooooori.civitdeck.domain.model.ModelVersion
import com.omooooori.civitdeck.domain.model.PaginatedResult
import com.omooooori.civitdeck.domain.model.SortOrder
import com.omooooori.civitdeck.domain.model.TimePeriod
import com.omooooori.civitdeck.domain.repository.ModelRepository

class ModelRepositoryImpl(
    private val api: CivitAiApi,
) : ModelRepository {

    override suspend fun getModels(
        query: String?,
        tag: String?,
        type: ModelType?,
        sort: SortOrder?,
        period: TimePeriod?,
        page: Int?,
        limit: Int?,
    ): PaginatedResult<Model> {
        val response = api.getModels(
            query = query,
            tag = tag,
            type = type?.name,
            sort = sort?.toApiParam(),
            period = period?.toApiParam(),
            page = page,
            limit = limit,
        )
        return PaginatedResult(
            items = response.items.map { it.toDomain() },
            metadata = response.metadata.toDomain(),
        )
    }

    override suspend fun getModel(id: Long): Model {
        return api.getModel(id).toDomain()
    }

    override suspend fun getModelVersion(id: Long): ModelVersion {
        return api.getModelVersion(id).toDomain()
    }

    override suspend fun getModelVersionByHash(hash: String): ModelVersion {
        return api.getModelVersionByHash(hash).toDomain()
    }
}

private fun SortOrder.toApiParam(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    SortOrder.Newest -> "Newest"
}

private fun TimePeriod.toApiParam(): String = when (this) {
    TimePeriod.AllTime -> "AllTime"
    TimePeriod.Year -> "Year"
    TimePeriod.Month -> "Month"
    TimePeriod.Week -> "Week"
    TimePeriod.Day -> "Day"
}
