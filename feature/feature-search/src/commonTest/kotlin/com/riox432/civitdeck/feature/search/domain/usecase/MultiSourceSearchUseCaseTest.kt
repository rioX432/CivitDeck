package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import com.riox432.civitdeck.testing.testModel
import com.riox432.civitdeck.testing.testPaginatedResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MultiSourceSearchUseCaseTest {

    // --- Fake repositories ---

    private class FakeModelRepository(
        private val result: PaginatedResult<Model> = testPaginatedResult(),
        private val error: Throwable? = null,
    ) : ModelRepository {
        var callCount = 0
        var lastQuery: ModelSearchQuery? = null

        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> {
            callCount++
            lastQuery = query
            error?.let { throw it }
            return result
        }

        override suspend fun getModel(id: Long): Model = error("not used")
        override suspend fun getModelVersion(id: Long) = error("not used")
        override suspend fun getModelVersionByHash(hash: String) = error("not used")
        override suspend fun getModelLicense(versionId: Long) = null
    }

    private class FakeHuggingFaceRepository(
        private val result: List<Model> = emptyList(),
        private val error: Throwable? = null,
    ) : HuggingFaceRepository {
        var callCount = 0

        override suspend fun searchModels(query: String?, limit: Int, offset: Int): List<Model> {
            callCount++
            error?.let { throw it }
            return result
        }
    }

    private class FakeTensorArtRepository(
        private val result: List<Model> = emptyList(),
        private val error: Throwable? = null,
    ) : TensorArtRepository {
        var callCount = 0

        override suspend fun searchModels(query: String, page: Int, pageSize: Int): List<Model> {
            callCount++
            error?.let { throw it }
            return result
        }
    }

    // --- Tests ---

    @Test
    fun invoke_onlyCivitaiSelected_returnsOnlyCivitaiModels() = runTest {
        // Arrange: CivitAI returns 2 models; secondary sources return models too but are NOT selected
        val civitaiModels = listOf(testModel(id = 1L), testModel(id = 2L))
        val civitaiRepo = FakeModelRepository(testPaginatedResult(items = civitaiModels))
        val hfRepo = FakeHuggingFaceRepository(result = listOf(testModel(id = 10L)))
        val taRepo = FakeTensorArtRepository(result = listOf(testModel(id = 20L)))
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            query = "anime",
            selectedSources = setOf(ModelSource.CIVITAI),
        )

        // Assert: only CivitAI results are included; secondary repos are never called
        assertEquals(civitaiModels, result.models)
        assertEquals(0, hfRepo.callCount)
        assertEquals(0, taRepo.callCount)
        assertFalse(result.hasPartialFailure)
    }

    @Test
    fun invoke_allSourcesSelected_mergesCivitaiFirstThenInterleaved() = runTest {
        // Arrange: each source returns distinct models
        val civitaiModels = listOf(testModel(id = 1L), testModel(id = 2L))
        val hfModels = listOf(testModel(id = 10L), testModel(id = 11L))
        val taModels = listOf(testModel(id = 20L), testModel(id = 21L))
        val civitaiRepo = FakeModelRepository(testPaginatedResult(items = civitaiModels))
        val hfRepo = FakeHuggingFaceRepository(result = hfModels)
        val taRepo = FakeTensorArtRepository(result = taModels)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: CivitAI items lead, then HF and TA interleaved (HF[0], TA[0], HF[1], TA[1])
        val expected = listOf(
            testModel(id = 1L),
            testModel(id = 2L),
            testModel(id = 10L), // HF first (interleave)
            testModel(id = 20L), // TA second
            testModel(id = 11L),
            testModel(id = 21L),
        )
        assertEquals(expected.map { it.id }, result.models.map { it.id })
        assertFalse(result.hasPartialFailure)
    }

    @Test
    fun invoke_secondarySourceFails_errorCapturedAndOtherSourcesStillReturn() = runTest {
        // Arrange: HuggingFace throws; CivitAI and TensorArt succeed
        val civitaiModels = listOf(testModel(id = 1L))
        val taModels = listOf(testModel(id = 20L))
        val hfError = RuntimeException("HF network error")
        val civitaiRepo = FakeModelRepository(testPaginatedResult(items = civitaiModels))
        val hfRepo = FakeHuggingFaceRepository(error = hfError)
        val taRepo = FakeTensorArtRepository(result = taModels)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: results still contain CivitAI + TensorArt; HF error is recorded
        assertEquals(listOf(1L, 20L), result.models.map { it.id })
        assertTrue(result.hasPartialFailure)
        assertEquals(hfError, result.sourceErrors[ModelSource.HUGGING_FACE])
        assertNull(result.sourceErrors[ModelSource.CIVITAI])
        assertNull(result.sourceErrors[ModelSource.TENSOR_ART])
    }

    @Test
    fun invoke_civitaiSourceFails_errorCapturedAndSecondarySourcesStillReturn() = runTest {
        // Arrange: CivitAI throws; HuggingFace and TensorArt succeed
        val hfModels = listOf(testModel(id = 10L))
        val taModels = listOf(testModel(id = 20L))
        val civitaiError = RuntimeException("CivitAI 503")
        val civitaiRepo = FakeModelRepository(error = civitaiError)
        val hfRepo = FakeHuggingFaceRepository(result = hfModels)
        val taRepo = FakeTensorArtRepository(result = taModels)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: secondary results are still returned; CivitAI error is captured
        assertEquals(listOf(10L, 20L), result.models.map { it.id })
        assertTrue(result.hasPartialFailure)
        assertEquals(civitaiError, result.sourceErrors[ModelSource.CIVITAI])
        assertNull(result.nextCursor) // no cursor when CivitAI fails
    }

    @Test
    fun invoke_onlySecondarySourcesSelected_civitaiIsNotQueried() = runTest {
        // Arrange: only HuggingFace and TensorArt are selected
        val hfModels = listOf(testModel(id = 10L))
        val taModels = listOf(testModel(id = 20L))
        val civitaiRepo = FakeModelRepository()
        val hfRepo = FakeHuggingFaceRepository(result = hfModels)
        val taRepo = FakeTensorArtRepository(result = taModels)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: CivitAI is never called; secondary results are interleaved
        assertEquals(0, civitaiRepo.callCount)
        assertEquals(listOf(10L, 20L), result.models.map { it.id })
        assertNull(result.nextCursor) // cursor is always sourced from CivitAI
        assertFalse(result.hasPartialFailure)
    }

    @Test
    fun invoke_noSourcesSelected_returnsEmptyResult() = runTest {
        // Arrange: empty source set — all deferred are null
        val civitaiRepo = FakeModelRepository()
        val hfRepo = FakeHuggingFaceRepository()
        val taRepo = FakeTensorArtRepository()
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(selectedSources = emptySet())

        // Assert: no calls made; empty result with no errors
        assertEquals(0, civitaiRepo.callCount)
        assertEquals(0, hfRepo.callCount)
        assertEquals(0, taRepo.callCount)
        assertTrue(result.models.isEmpty())
        assertFalse(result.hasPartialFailure)
    }

    @Test
    fun invoke_nextCursorPropagatdFromCivitai() = runTest {
        // Arrange: CivitAI response carries a next cursor for pagination
        val civitaiResult = PaginatedResult(
            items = listOf(testModel(id = 1L)),
            metadata = PageMetadata(nextCursor = "cursor-abc", nextPage = null),
        )
        val civitaiRepo = FakeModelRepository(result = civitaiResult)
        val hfRepo = FakeHuggingFaceRepository()
        val taRepo = FakeTensorArtRepository()
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(selectedSources = setOf(ModelSource.CIVITAI))

        // Assert: cursor from CivitAI metadata is forwarded
        assertEquals("cursor-abc", result.nextCursor)
    }

    @Test
    fun invoke_passesQueryAndCursorToCivitai() = runTest {
        // Arrange
        val civitaiRepo = FakeModelRepository(testPaginatedResult())
        val hfRepo = FakeHuggingFaceRepository()
        val taRepo = FakeTensorArtRepository()
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        useCase(
            query = "checkpoint",
            selectedSources = setOf(ModelSource.CIVITAI),
            cursor = "cursor-xyz",
            limit = 10,
        )

        // Assert: the exact query parameters are forwarded to the repository
        val q = civitaiRepo.lastQuery
        assertEquals("checkpoint", q?.query)
        assertEquals("cursor-xyz", q?.cursor)
        assertEquals(10, q?.limit)
    }

    @Test
    fun invoke_allSourcesFail_resultHasAllErrorsAndEmptyModels() = runTest {
        // Arrange: every source throws
        val civitaiError = RuntimeException("CivitAI error")
        val hfError = RuntimeException("HF error")
        val taError = RuntimeException("TA error")
        val civitaiRepo = FakeModelRepository(error = civitaiError)
        val hfRepo = FakeHuggingFaceRepository(error = hfError)
        val taRepo = FakeTensorArtRepository(error = taError)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: models list is empty; all three errors are captured
        assertTrue(result.models.isEmpty())
        assertTrue(result.hasPartialFailure)
        assertEquals(civitaiError, result.sourceErrors[ModelSource.CIVITAI])
        assertEquals(hfError, result.sourceErrors[ModelSource.HUGGING_FACE])
        assertEquals(taError, result.sourceErrors[ModelSource.TENSOR_ART])
    }

    @Test
    fun invoke_unevenSecondaryLists_interleavesFully() = runTest {
        // Arrange: HuggingFace returns 3 items, TensorArt returns 1 — interleave should not drop items
        val hfModels = listOf(testModel(id = 10L), testModel(id = 11L), testModel(id = 12L))
        val taModels = listOf(testModel(id = 20L))
        val civitaiRepo = FakeModelRepository(testPaginatedResult())
        val hfRepo = FakeHuggingFaceRepository(result = hfModels)
        val taRepo = FakeTensorArtRepository(result = taModels)
        val useCase = MultiSourceSearchUseCase(civitaiRepo, hfRepo, taRepo)

        // Act
        val result = useCase(
            selectedSources = setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE, ModelSource.TENSOR_ART),
        )

        // Assert: interleave produces HF[0], TA[0], HF[1], HF[2] (remainder of longer list appended)
        assertEquals(listOf(10L, 20L, 11L, 12L), result.models.map { it.id })
    }
}
