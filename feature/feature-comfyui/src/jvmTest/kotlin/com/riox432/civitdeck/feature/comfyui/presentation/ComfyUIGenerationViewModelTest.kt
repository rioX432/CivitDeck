package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.domain.service.AppLifecycleTracker
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarter
import com.riox432.civitdeck.domain.usecase.ObserveGenerationNotificationsEnabledUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ApplyWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExtractWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchObjectInfoUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InjectWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ParseAppModeMetadataUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.testing.FakeAppBehaviorPreferencesRepository
import com.riox432.civitdeck.testing.FakeGenerationNotificationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Covers [ComfyUIGenerationViewModel.applyPrefill], including the race between a requested
 * checkpoint and the server checkpoint list loaded from `init`, and
 * [ComfyUIGenerationViewModel.onTemplateApplied].
 *
 * Lives in jvmTest because the loader logs through `Logger`, which needs `android.util.Log`
 * unmocked on the Android host target.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComfyUIGenerationViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGenerationRepo(
        private val checkpoints: CompletableDeferred<List<String>>,
    ) : ComfyUIGenerationRepository {
        override suspend fun fetchCheckpoints(): List<String> = checkpoints.await()
        override suspend fun fetchLoras(): List<String> = emptyList()
        override suspend fun fetchControlNets(): List<String> = emptyList()
        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String = ""
        override suspend fun pollGenerationResult(promptId: String): GenerationResult =
            error("not used")
        override fun observeGenerationProgress(
            promptId: String,
            host: String,
            port: Int,
        ): Flow<GenerationProgress> = emptyFlow()
        override fun observeGenerationProgress(
            promptId: String,
            baseUrl: String,
            wsScheme: String,
        ): Flow<GenerationProgress> = emptyFlow()
        override fun getImageUrl(filename: String, subfolder: String, type: String): String = ""
        override suspend fun interruptGeneration() = Unit
        override suspend fun uploadMaskImage(maskPngBytes: ByteArray): String = ""
        override suspend fun fetchObjectInfo(): String = "{}"
    }

    private class FakeConnectionRepo : ComfyUIConnectionRepository {
        override fun observeConnections(): Flow<List<ComfyUIConnection>> = flowOf(emptyList())
        override fun observeActiveConnection(): Flow<ComfyUIConnection?> = flowOf(null)
        override suspend fun getActiveConnection(): ComfyUIConnection? = null
        override suspend fun saveConnection(connection: ComfyUIConnection): Long = 0L
        override suspend fun deleteConnection(id: Long) = Unit
        override suspend fun activateConnection(id: Long) = Unit
        override suspend fun testConnection(connection: ComfyUIConnection): Boolean = true
        override suspend fun updateTestResult(id: Long, success: Boolean) = Unit
    }

    private class NoCacheDao : CachedApiResponseDao {
        override suspend fun getByKey(key: String): CachedApiResponseEntity? = null
        override suspend fun insert(entity: CachedApiResponseEntity) = Unit
        override suspend fun deleteByKey(key: String): Int = 0
        override suspend fun deleteExpired(expiryTime: Long): Int = 0
        override suspend fun deleteAll(): Int = 0
        override suspend fun setPinned(key: String, pinned: Boolean): Int = 0
        override suspend fun getTotalCacheSizeBytes(): Long? = null
        override suspend fun getEntryCount(): Int = 0
        override suspend fun deleteOldestUnpinned(count: Int): Int = 0
    }

    private object NoOpImageSaver : ImageSaver {
        override suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean = false
    }

    private object ForegroundTracker : AppLifecycleTracker {
        override val isInForeground: Boolean = true
    }

    private object NoOpMonitorStarter : BackgroundMonitorStarter {
        override fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) = Unit
        override fun stopMonitoring() = Unit
    }

    private fun TestScope.createViewModel(
        checkpoints: CompletableDeferred<List<String>>,
    ): ComfyUIGenerationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeGenerationRepo(checkpoints)
        val httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        return ComfyUIGenerationViewModel(
            executionUseCases = GenerationExecutionUseCases(
                submitGeneration = SubmitComfyUIGenerationUseCase(repo),
                pollResult = PollComfyUIResultUseCase(repo),
                observeProgress = ObserveGenerationProgressUseCase(repo),
                interruptGeneration = InterruptComfyUIGenerationUseCase(repo),
                saveImage = SaveGeneratedImageUseCase(httpClient, NoOpImageSaver),
                observeGenNotifEnabled = ObserveGenerationNotificationsEnabledUseCase(
                    FakeAppBehaviorPreferencesRepository(),
                ),
                repository = FakeConnectionRepo(),
                notificationService = FakeGenerationNotificationService(),
                lifecycleTracker = ForegroundTracker,
                backgroundMonitorStarter = NoOpMonitorStarter,
            ),
            resourceUseCases = GenerationResourceUseCases(
                fetchCheckpoints = FetchComfyUICheckpointsUseCase(repo),
                fetchLoras = FetchComfyUILorasUseCase(repo),
                fetchControlNets = FetchComfyUIControlNetsUseCase(repo),
                fetchObjectInfo = FetchObjectInfoUseCase(repo, LocalCacheDataSource(NoCacheDao())),
                extractParameters = ExtractWorkflowParametersUseCase(ParseAppModeMetadataUseCase()),
            ),
            importWorkflow = ImportWorkflowUseCase(),
            injectParameters = InjectWorkflowParametersUseCase(),
            applyTemplate = ApplyWorkflowTemplateUseCase(),
        )
    }

    private fun loaded(vararg names: String) = CompletableDeferred(names.toList())

    @Test
    fun applies_prompt_negative_steps_cfg_seed_and_size() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()

        vm.applyPrefill(
            ComfyUIGenerationParams(
                checkpoint = "",
                prompt = "a cat",
                negativePrompt = "blurry",
                steps = 30,
                cfgScale = 5.5,
                seed = 42L,
                width = 832,
                height = 1216,
            ),
        )

        val state = vm.uiState.value
        assertEquals("a cat", state.prompt)
        assertEquals("blurry", state.negativePrompt)
        assertEquals(30, state.steps)
        assertEquals(5.5, state.cfgScale)
        assertEquals(42L, state.seed)
        assertEquals(832, state.width)
        assertEquals(1216, state.height)
    }

    @Test
    fun blank_prompt_keeps_the_existing_prompt() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()
        vm.onPromptChanged("typed by user")
        vm.onNegativePromptChanged("typed negative")

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "", prompt = " ", negativePrompt = ""))

        assertEquals("typed by user", vm.uiState.value.prompt)
        assertEquals("typed negative", vm.uiState.value.negativePrompt)
    }

    @Test
    fun checkpoint_requested_before_load_is_selected_by_file_name_after_load() = runTest {
        val checkpoints = CompletableDeferred<List<String>>()
        val vm = createViewModel(checkpoints)
        advanceUntilIdle()

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "FOO.safetensors", prompt = "p"))
        checkpoints.complete(listOf("SD15/bar.safetensors", "SDXL/foo.safetensors"))
        advanceUntilIdle()

        assertEquals("SDXL/foo.safetensors", vm.uiState.value.selectedCheckpoint)
    }

    @Test
    fun checkpoint_requested_after_load_is_selected_immediately() = runTest {
        val vm = createViewModel(loaded("SD15/bar.safetensors", "SDXL\\foo.safetensors"))
        advanceUntilIdle()

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "foo.safetensors", prompt = "p"))

        assertEquals("SDXL\\foo.safetensors", vm.uiState.value.selectedCheckpoint)
    }

    @Test
    fun unknown_checkpoint_requested_before_load_falls_back_to_first_entry() = runTest {
        val checkpoints = CompletableDeferred<List<String>>()
        val vm = createViewModel(checkpoints)
        advanceUntilIdle()

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "missing.safetensors", prompt = "p"))
        checkpoints.complete(listOf("SD15/bar.safetensors", "SDXL/foo.safetensors"))
        advanceUntilIdle()

        assertEquals("SD15/bar.safetensors", vm.uiState.value.selectedCheckpoint)
    }

    @Test
    fun unknown_checkpoint_requested_after_load_falls_back_to_first_entry() = runTest {
        val vm = createViewModel(loaded("SD15/bar.safetensors", "SDXL/foo.safetensors"))
        advanceUntilIdle()
        vm.onCheckpointSelected("SDXL/foo.safetensors")

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "missing.safetensors", prompt = "p"))

        assertEquals("SD15/bar.safetensors", vm.uiState.value.selectedCheckpoint)
    }

    @Test
    fun sampler_and_scheduler_are_not_applied() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()

        vm.applyPrefill(
            ComfyUIGenerationParams(
                checkpoint = "",
                prompt = "p",
                samplerName = "dpm++_2m_karras",
                scheduler = "karras",
            ),
        )

        assertEquals("euler", vm.uiState.value.samplerName)
        assertEquals("normal", vm.uiState.value.scheduler)
    }

    private fun template(
        vararg variableNames: String,
        rawWorkflowJson: String? = null,
    ) = WorkflowTemplate(
        id = 1L,
        name = "t",
        type = WorkflowTemplateType.TXT2IMG,
        variables = variableNames.map {
            TemplateVariable(name = it, type = TemplateVariableType.TEXT, defaultValue = "")
        },
        isBuiltIn = false,
        createdAt = 0L,
        rawWorkflowJson = rawWorkflowJson,
    )

    @Test
    fun template_values_fill_the_form() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()

        vm.onTemplateApplied(
            template("positive_prompt", "steps", "width"),
            mapOf("positive_prompt" to "cat", "steps" to "30", "width" to "768"),
        )

        val state = vm.uiState.value
        assertEquals("cat", state.prompt)
        assertEquals(30, state.steps)
        assertEquals(768, state.width)
        assertNull(state.customWorkflowJson)
    }

    @Test
    fun raw_workflow_template_loads_it_as_the_custom_workflow() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()

        vm.onTemplateApplied(template(rawWorkflowJson = RAW_WORKFLOW))
        advanceUntilIdle()

        assertEquals(RAW_WORKFLOW, vm.uiState.value.customWorkflowJson)
    }

    @Test
    fun variables_template_clears_a_loaded_custom_workflow() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()
        vm.onImportWorkflow(RAW_WORKFLOW)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.customWorkflowJson)

        vm.onTemplateApplied(template("positive_prompt"), mapOf("positive_prompt" to "cat"))

        assertNull(vm.uiState.value.customWorkflowJson)
        assertEquals("cat", vm.uiState.value.prompt)
    }

    private companion object {
        const val RAW_WORKFLOW =
            """{"3":{"class_type":"KSampler","inputs":{"seed":1,"steps":20}}}"""
    }
}
