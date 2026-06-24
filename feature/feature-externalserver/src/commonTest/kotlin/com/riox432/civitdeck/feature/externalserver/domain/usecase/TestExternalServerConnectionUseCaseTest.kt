package com.riox432.civitdeck.feature.externalserver.domain.usecase

import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [TestExternalServerConnectionUseCase] returns the connectivity result and persists it
 * to the config repository only for saved configs (id != 0), not for unsaved drafts.
 */
class TestExternalServerConnectionUseCaseTest {

    @Test
    fun persistsResultForSavedConfig() = runTest {
        val configRepo = RecordingConfigRepo()
        val imagesRepo = FakeImagesRepo(testResult = true)
        val useCase = TestExternalServerConnectionUseCase(configRepo, imagesRepo)

        val success = useCase(config(id = 7L))

        assertTrue(success)
        assertEquals(7L to true, configRepo.lastUpdate)
    }

    @Test
    fun doesNotPersistResultForUnsavedDraft() = runTest {
        val configRepo = RecordingConfigRepo()
        val imagesRepo = FakeImagesRepo(testResult = false)
        val useCase = TestExternalServerConnectionUseCase(configRepo, imagesRepo)

        val success = useCase(config(id = 0L))

        assertFalse(success)
        // id == 0 => updateTestResult must not be called.
        assertEquals(null, configRepo.lastUpdate)
    }

    private fun config(id: Long) = ExternalServerConfig(
        id = id,
        name = "Server",
        baseUrl = "http://10.0.0.1",
        createdAt = 0L,
    )

    private class RecordingConfigRepo : ExternalServerConfigRepository {
        var lastUpdate: Pair<Long, Boolean>? = null
        override suspend fun updateTestResult(id: Long, success: Boolean) {
            lastUpdate = id to success
        }
        override fun observeConfigs(): Flow<List<ExternalServerConfig>> = flowOf(emptyList())
        override fun observeActiveConfig(): Flow<ExternalServerConfig?> = flowOf(null)
        override suspend fun saveConfig(config: ExternalServerConfig): Long = 1L
        override suspend fun deleteConfig(id: Long) = Unit
        override suspend fun activateConfig(id: Long) = Unit
    }

    private class FakeImagesRepo(private val testResult: Boolean) : ExternalServerImagesRepository {
        override suspend fun testConnection(): Boolean = testResult
        override suspend fun deleteImage(cloudKey: String) = Unit
        override suspend fun deleteImages(cloudKeys: List<String>) = Unit
        override suspend fun getCapabilities(): ServerCapabilities = error("not used")
        override suspend fun getImages(
            page: Int,
            perPage: Int,
            filters: ExternalServerImageFilters,
        ): PaginatedImagesResponse = error("not used")
        override suspend fun getGenerationOptions(): List<GenerationOption> = error("not used")
        override suspend fun getDependentChoices(endpoint: String): List<GenerationChoice> = error("not used")
        override suspend fun executeGeneration(params: Map<String, String>): GenerationJob = error("not used")
        override suspend fun getGenerationStatus(jobId: String): GenerationJob = error("not used")
    }
}
