package com.riox432.civitdeck.feature.externalserver.domain.usecase

import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies [DeleteServerImagesUseCase] routes a single key to the singular delete endpoint and
 * multiple keys to the batch endpoint.
 */
class DeleteServerImagesUseCaseTest {

    @Test
    fun singleKeyUsesSingularDeleteEndpoint() = runTest {
        val repo = RecordingImagesRepo()
        val useCase = DeleteServerImagesUseCase(repo)

        useCase(listOf("key-1"))

        assertEquals("key-1", repo.deletedSingle)
        assertEquals(null, repo.deletedBatch)
    }

    @Test
    fun multipleKeysUseBatchDeleteEndpoint() = runTest {
        val repo = RecordingImagesRepo()
        val useCase = DeleteServerImagesUseCase(repo)

        useCase(listOf("key-1", "key-2", "key-3"))

        assertEquals(null, repo.deletedSingle)
        assertEquals(listOf("key-1", "key-2", "key-3"), repo.deletedBatch)
    }

    @Test
    fun emptyKeysUseBatchEndpoint() = runTest {
        val repo = RecordingImagesRepo()
        val useCase = DeleteServerImagesUseCase(repo)

        useCase(emptyList())

        // size != 1 takes the batch branch.
        assertEquals(null, repo.deletedSingle)
        assertEquals(emptyList(), repo.deletedBatch)
    }

    private class RecordingImagesRepo : ExternalServerImagesRepository {
        var deletedSingle: String? = null
        var deletedBatch: List<String>? = null

        override suspend fun deleteImage(cloudKey: String) {
            deletedSingle = cloudKey
        }
        override suspend fun deleteImages(cloudKeys: List<String>) {
            deletedBatch = cloudKeys
        }
        override suspend fun getCapabilities(): ServerCapabilities = error("not used")
        override suspend fun getImages(
            page: Int,
            perPage: Int,
            filters: ExternalServerImageFilters,
        ): PaginatedImagesResponse = error("not used")
        override suspend fun testConnection(): Boolean = error("not used")
        override suspend fun getGenerationOptions(): List<GenerationOption> = error("not used")
        override suspend fun getDependentChoices(endpoint: String): List<GenerationChoice> = error("not used")
        override suspend fun executeGeneration(params: Map<String, String>): GenerationJob = error("not used")
        override suspend fun getGenerationStatus(jobId: String): GenerationJob = error("not used")
    }
}
