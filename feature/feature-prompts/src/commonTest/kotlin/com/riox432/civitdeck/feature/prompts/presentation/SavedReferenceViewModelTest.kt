package com.riox432.civitdeck.feature.prompts.presentation

import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelLicenseInfo
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import com.riox432.civitdeck.domain.model.SourceResource
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import com.riox432.civitdeck.testing.testImage
import com.riox432.civitdeck.testing.testModelVersion
import com.riox432.civitdeck.testing.testSavedPrompt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedReferenceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saving_a_metadata_free_image_returns_the_local_id() = runTest {
        val references = FakeReferenceRepository()
        val vm = SavedReferenceViewModel(references, VersionLookupRepository())

        vm.saveImage(testImage().copy(meta = null, sourceReference = METADATA_FREE))

        assertEquals(SaveReferenceState.Saved(id = 1L, artworkId = "123"), vm.saveState.value)
        assertEquals(listOf(METADATA_FREE), references.saved)
    }

    @Test
    fun an_image_without_a_source_reference_is_not_saved() = runTest {
        val references = FakeReferenceRepository()
        val vm = SavedReferenceViewModel(references, VersionLookupRepository())

        vm.saveImage(testImage())

        assertEquals(SaveReferenceState.NoSource, vm.saveState.value)
        assertTrue(references.saved.isEmpty())
    }

    @Test
    fun repeated_taps_while_saving_and_after_saving_write_once() = runTest {
        val gate = CompletableDeferred<Unit>()
        val references = FakeReferenceRepository(gate = gate)
        val vm = SavedReferenceViewModel(references, VersionLookupRepository())
        val image = testImage().copy(sourceReference = METADATA_FREE)

        vm.saveImage(image)
        vm.saveImage(image)
        assertEquals(SaveReferenceState.Saving, vm.saveState.value)
        gate.complete(Unit)
        vm.saveImage(image)

        assertEquals(1, references.saved.size)
        assertEquals(SaveReferenceState.Saved(id = 1L, artworkId = "123"), vm.saveState.value)
    }

    @Test
    fun a_failed_save_is_recoverable_and_distinct_from_unavailable_sources() = runTest {
        val references = FakeReferenceRepository(failuresLeft = 1)
        val vm = SavedReferenceViewModel(references, VersionLookupRepository())
        val image = testImage().copy(sourceReference = METADATA_FREE)

        vm.saveImage(image)
        assertEquals(SaveReferenceState.Failed, vm.saveState.value)

        vm.saveImage(image)
        assertEquals(SaveReferenceState.Saved(id = 1L, artworkId = "123"), vm.saveState.value)
        assertEquals(1, references.saved.size)
    }

    @Test
    fun loading_a_reference_reports_loaded_missing_unreadable_and_failed() = runTest {
        val readable = testSavedPrompt(id = 1L)
            .copy(sourceReference = SourceReferenceDecodeResult.Decoded(METADATA_FREE))
        val unreadable = testSavedPrompt(id = 2L).copy(sourceReference = SourceReferenceDecodeResult.Malformed)
        val references = FakeReferenceRepository(stored = listOf(readable, unreadable))
        val vm = SavedReferenceViewModel(references, VersionLookupRepository())

        vm.loadReference(1L)
        val loaded = assertIs<SavedReferenceDetailState.Loaded>(vm.detailState.value)
        assertEquals(METADATA_FREE, loaded.reference)

        vm.loadReference(2L)
        val corrupt = assertIs<SavedReferenceDetailState.Loaded>(vm.detailState.value)
        assertTrue(corrupt.sourceUnavailable)

        vm.loadReference(MISSING_ID)
        assertEquals(SavedReferenceDetailState.NotFound, vm.detailState.value)

        references.failObserve = true
        vm.loadReference(1L)
        assertEquals(SavedReferenceDetailState.Failed, vm.detailState.value)
    }

    @Test
    fun inspecting_a_resource_opens_exactly_the_recorded_version() = runTest {
        val file = ModelFile(
            id = 9L, name = "detail.safetensors", sizeKB = 1.0, type = "Model", format = "SafeTensor",
            fp = null, size = null, downloadUrl = "https://civitai.com/api/download/models/42", primary = true,
            hashes = emptyMap(),
        )
        val models = VersionLookupRepository(testModelVersion(id = 42L, modelId = 7L).copy(files = listOf(file)))
        val vm = SavedReferenceViewModel(FakeReferenceRepository(), models)

        vm.inspectResource(42L)

        assertEquals(listOf(42L), models.requested)
        val available = assertIs<ResourceVersionState.Available>(vm.resources.value[42L])
        assertEquals(7L, available.modelId)
        assertEquals("v1.0", available.name)
        assertEquals("SD 1.5", available.baseModel)
        assertEquals(listOf(file), available.files)
        assertEquals("https://civitai.com/models/7?modelVersionId=42", available.url)

        vm.inspectResource(42L)
        assertEquals(listOf(42L), models.requested)
    }

    @Test
    fun a_missing_version_is_unavailable_and_never_replaced_by_another_version() = runTest {
        // The lookup answers 43 for a request of 42, the way a "latest version" fallback would.
        val models = VersionLookupRepository(testModelVersion(id = 43L, modelId = 7L), answerAnyId = true)
        val references = FakeReferenceRepository(
            stored = listOf(
                testSavedPrompt(id = 1L).copy(sourceReference = SourceReferenceDecodeResult.Decoded(METADATA_FREE)),
            ),
        )
        val vm = SavedReferenceViewModel(references, models)
        vm.loadReference(1L)

        vm.inspectResource(42L)
        vm.inspectResource(MISSING_ID)
        vm.inspectResource(0L)

        assertEquals(ResourceVersionState.Unavailable(42L), vm.resources.value[42L])
        assertEquals(ResourceVersionState.Unavailable(MISSING_ID), vm.resources.value[MISSING_ID])
        assertEquals(ResourceVersionState.Unavailable(0L), vm.resources.value[0L])
        assertEquals(listOf(42L, MISSING_ID), models.requested)
        val detail = assertIs<SavedReferenceDetailState.Loaded>(vm.detailState.value)
        assertEquals(METADATA_FREE, detail.reference)
    }

    @Test
    fun an_unavailable_version_can_be_retried_once_back_online() = runTest {
        val models = VersionLookupRepository(testModelVersion(id = 42L, modelId = 7L), offline = true)
        val vm = SavedReferenceViewModel(FakeReferenceRepository(), models)

        vm.inspectResource(42L)
        assertEquals(ResourceVersionState.Unavailable(42L), vm.resources.value[42L])

        models.offline = false
        vm.inspectResource(42L)
        assertIs<ResourceVersionState.Available>(vm.resources.value[42L])
        assertEquals(listOf(42L, 42L), models.requested)
    }

    private class FakeReferenceRepository(
        stored: List<SavedPrompt> = emptyList(),
        private val gate: CompletableDeferred<Unit>? = null,
        private var failuresLeft: Int = 0,
    ) : SavedReferenceRepository {
        val saved = mutableListOf<SourceReference>()
        var failObserve = false
        private val rows = MutableStateFlow(stored)

        override suspend fun saveReference(reference: SourceReference): Long {
            gate?.await()
            if (failuresLeft > 0) {
                failuresLeft--
                throw DomainException.DatabaseException("disk full")
            }
            saved += reference
            return saved.size.toLong()
        }

        override suspend fun getReference(id: Long): SavedPrompt? = rows.value.firstOrNull { it.id == id }

        override fun observeReference(id: Long): Flow<SavedPrompt?> =
            if (failObserve) {
                flow { throw DomainException.DatabaseException("closed") }
            } else {
                rows.map { prompts -> prompts.firstOrNull { it.id == id } }
            }
    }

    private class VersionLookupRepository(
        private val version: ModelVersion? = null,
        private val answerAnyId: Boolean = false,
        var offline: Boolean = false,
    ) : ModelRepository {
        val requested = mutableListOf<Long>()

        override suspend fun getModelVersion(id: Long): ModelVersion {
            requested += id
            if (offline) throw DomainException.NetworkException("offline")
            return version?.takeIf { answerAnyId || it.id == id }
                ?: throw DomainException.NetworkException("HTTP 404")
        }

        override suspend fun getModels(query: ModelSearchQuery): PaginatedResult<Model> = error("not used")
        override suspend fun getModel(id: Long): Model = error("not used")
        override suspend fun getModelVersionByHash(hash: String): ModelVersion = error("not used")
        override suspend fun getModelLicense(versionId: Long): ModelLicenseInfo? = null
    }

    private companion object {
        const val MISSING_ID = 9_999L
        val METADATA_FREE = SourceReference(
            provider = SourceProvider.Civitai,
            artworkId = "123",
            artworkUrl = "https://civitai.com/images/123",
            resources = listOf(SourceResource(versionId = 42L)),
        )
    }
}
