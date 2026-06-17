package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.ModelRepository

/**
 * Configurable in-memory [ModelRepository] for ViewModel/loader tests.
 *
 * `getModels` returns successive [pages] (falling back to the last page once
 * exhausted) and records the last [ModelSearchQuery] so callers can assert
 * request parameters (e.g. the resolved `nsfw` flag). `getModel` returns
 * [singleModel] when set.
 */
class FakeModelRepository(
    private val pages: List<PaginatedResult<Model>> =
        listOf(PaginatedResult(emptyList(), PageMetadata(null, null))),
    private val singleModel: Model? = null,
) : ModelRepository {

    var getModelsCallCount: Int = 0
    var lastQuery: ModelSearchQuery? = null

    override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> {
        lastQuery = query
        val result = pages.getOrElse(getModelsCallCount) { pages.last() }
        getModelsCallCount++
        return result
    }

    override suspend fun getModel(id: Long): Model =
        singleModel ?: error("singleModel not configured")

    override suspend fun getModelVersion(id: Long): ModelVersion = error("not used")
    override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
    override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? = null
}
