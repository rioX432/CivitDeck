package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.ImageSaver
import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.DiffusionModelFamily
import com.riox432.civitdeck.domain.model.DiffusionModelResources
import com.riox432.civitdeck.domain.model.DiffusionModelSelection
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
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.feature.comfyui.data.repository.ComfyUIQueueRepositoryImpl
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ApplyWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelComfyUIJobUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExtractWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchDiffusionModelResourcesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchObjectInfoUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InjectWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ParseAppModeMetadataUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PopulateGenerationFromModelUseCase
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
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIGenerationViewModel.applyPrefill], including the race between a requested
 * checkpoint and the server checkpoint list loaded from `init`,
 * [ComfyUIGenerationViewModel.onTemplateApplied], the split-loader lists loaded from `init`, and
 * diffusion model selection.
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
        private val diffusionModelResources: suspend () -> DiffusionModelResources,
        private val submitted: MutableList<ComfyUIGenerationParams>,
    ) : ComfyUIGenerationRepository {
        override suspend fun fetchCheckpoints(): List<String> = checkpoints.await()
        override suspend fun fetchLoras(): List<String> = emptyList()
        override suspend fun fetchControlNets(): List<String> = emptyList()
        override suspend fun fetchDiffusionModelResources(): DiffusionModelResources = diffusionModelResources()
        override suspend fun submitGeneration(params: ComfyUIGenerationParams): String {
            submitted += params
            return "prompt-1"
        }
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
        diffusionModelResources: suspend () -> DiffusionModelResources = { DiffusionModelResources() },
        submitted: MutableList<ComfyUIGenerationParams> = mutableListOf(),
    ): ComfyUIGenerationViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeGenerationRepo(checkpoints, diffusionModelResources, submitted)
        val httpClient = HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })
        return ComfyUIGenerationViewModel(
            executionUseCases = GenerationExecutionUseCases(
                submitGeneration = SubmitComfyUIGenerationUseCase(repo),
                pollResult = PollComfyUIResultUseCase(repo),
                observeProgress = ObserveGenerationProgressUseCase(repo),
                cancelJob = CancelComfyUIJobUseCase(
                    ComfyUIQueueRepositoryImpl(ComfyUIApiProvider(FakeComfyUIConnectionDao(), httpClient, Json)),
                ),
                saveImage = SaveGeneratedImageUseCase(
                    ComfyUIApiProvider(FakeComfyUIConnectionDao(), httpClient, Json),
                    NoOpImageSaver,
                ),
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
                fetchDiffusionModelResources = FetchDiffusionModelResourcesUseCase(repo),
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

    @Test
    fun diffusion_model_lists_load_into_state() = runTest {
        val vm = createViewModel(
            loaded("SD15/bar.safetensors", "SDXL/foo.safetensors"),
            diffusionModelResources = {
                DiffusionModelResources(
                    diffusionModels = listOf("flux1-dev.safetensors"),
                    textEncoders = listOf("t5xxl_fp16.safetensors", "clip_l.safetensors"),
                    vaes = listOf("ae.safetensors"),
                    clipTypes = listOf("stable_diffusion", "flux"),
                )
            },
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("flux1-dev.safetensors"), state.diffusionModels)
        assertEquals(listOf("t5xxl_fp16.safetensors", "clip_l.safetensors"), state.textEncoders)
        assertEquals(listOf("ae.safetensors"), state.vaes)
        assertEquals(listOf("stable_diffusion", "flux"), state.serverClipTypes)
        assertEquals("SD15/bar.safetensors", state.selectedCheckpoint)
    }

    @Test
    fun failing_diffusion_model_fetch_leaves_lists_empty_without_error() = runTest {
        val vm = createViewModel(
            loaded("SD15/bar.safetensors", "SDXL/foo.safetensors"),
            diffusionModelResources = { error("server unreachable") },
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(emptyList(), state.diffusionModels)
        assertEquals(emptyList(), state.textEncoders)
        assertEquals(emptyList(), state.vaes)
        assertEquals(emptyList(), state.serverClipTypes)
        assertNull(state.error)
        assertEquals(listOf("SD15/bar.safetensors", "SDXL/foo.safetensors"), state.checkpoints)
        assertEquals("SD15/bar.safetensors", state.selectedCheckpoint)
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

    // -- Diffusion model selection --

    private fun TestScope.createDiffusionViewModel(
        clipTypes: List<String> = listOf("stable_diffusion", "krea2"),
        textEncoders: List<String> = listOf("qwen3vl_4b_fp8_scaled.safetensors", "DiT/Qwen_3_06B_base.safetensors"),
        vaes: List<String> = listOf("ae.safetensors", "qwen_image_vae.safetensors"),
        submitted: MutableList<ComfyUIGenerationParams> = mutableListOf(),
    ): ComfyUIGenerationViewModel {
        val vm = createViewModel(
            loaded("SD15/bar.safetensors", "SDXL/foo.safetensors"),
            diffusionModelResources = {
                DiffusionModelResources(
                    diffusionModels = listOf(KREA_MODEL, ANIMA_MODEL),
                    textEncoders = textEncoders,
                    vaes = vaes,
                    clipTypes = clipTypes,
                )
            },
            submitted = submitted,
        )
        advanceUntilIdle()
        vm.onPromptChanged("a cat")
        return vm
    }

    @Test
    fun checkpoint_enablement_needs_a_prompt_and_a_checkpoint_or_a_custom_workflow() = runTest {
        val vm = createViewModel(loaded("a.safetensors"))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canGenerate)

        vm.onPromptChanged("a cat")
        assertTrue(vm.uiState.value.canGenerate)

        vm.onPromptChanged("")
        vm.onImportWorkflow(RAW_WORKFLOW)
        assertTrue(vm.uiState.value.canGenerate)
    }

    @Test
    fun krea_2_is_unsupported_without_its_clip_type_on_the_server() = runTest {
        val vm = createDiffusionViewModel(clipTypes = listOf("stable_diffusion", "flux"))
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        val state = vm.uiState.value
        assertFalse(DiffusionModelFamily.KREA_2.isSupportedBy(state.serverClipTypes))
        assertTrue(DiffusionModelFamily.ANIMA.isSupportedBy(state.serverClipTypes))
        assertEquals("qwen3vl_4b_fp8_scaled.safetensors", state.selectedTextEncoder)
        assertEquals("qwen_image_vae.safetensors", state.selectedVae)
        assertFalse(state.canGenerate)
    }

    @Test
    fun picking_anima_applies_its_defaults_and_preselects_files_by_hint() = runTest {
        val vm = createDiffusionViewModel()
        vm.onDiffusionModelSelected(ANIMA_MODEL)

        vm.onModelFamilySelected(DiffusionModelFamily.ANIMA)

        val state = vm.uiState.value
        assertEquals(DiffusionModelFamily.ANIMA, state.selectedFamily)
        assertEquals(30, state.steps)
        assertEquals(4.0, state.cfgScale)
        assertEquals("euler", state.samplerName)
        assertEquals("simple", state.scheduler)
        assertEquals(1024, state.width)
        assertEquals(1024, state.height)
        assertEquals("DiT/Qwen_3_06B_base.safetensors", state.selectedTextEncoder)
        assertEquals("qwen_image_vae.safetensors", state.selectedVae)
        assertTrue(state.canGenerate)
    }

    @Test
    fun can_generate_only_once_family_text_encoder_and_vae_are_all_set() = runTest {
        // No file matches a hint, so nothing is preselected.
        val vm = createDiffusionViewModel(textEncoders = listOf("t5xxl.safetensors"), vaes = listOf("ae.safetensors"))

        vm.onDiffusionModelSelected(ANIMA_MODEL)
        assertFalse(vm.uiState.value.canGenerate)
        vm.onModelFamilySelected(DiffusionModelFamily.ANIMA)
        assertFalse(vm.uiState.value.canGenerate)
        vm.onTextEncoderSelected("t5xxl.safetensors")
        assertFalse(vm.uiState.value.canGenerate)
        vm.onVaeSelected("ae.safetensors")
        assertTrue(vm.uiState.value.canGenerate)
    }

    @Test
    fun generate_submits_the_selected_diffusion_model() = runTest {
        val submitted = mutableListOf<ComfyUIGenerationParams>()
        val vm = createDiffusionViewModel(submitted = submitted)
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        vm.onGenerate()
        advanceUntilIdle()

        val params = submitted.single()
        assertEquals(
            DiffusionModelSelection(
                unetName = KREA_MODEL,
                textEncoderName = "qwen3vl_4b_fp8_scaled.safetensors",
                clipType = "krea2",
                vaeName = "qwen_image_vae.safetensors",
            ),
            params.diffusionModel,
        )
        assertEquals("", params.checkpoint)
        assertEquals(8, params.steps)
        assertEquals(1.0, params.cfgScale)
        assertEquals("simple", params.scheduler)
    }

    @Test
    fun generate_from_a_checkpoint_submits_no_diffusion_model() = runTest {
        val submitted = mutableListOf<ComfyUIGenerationParams>()
        val vm = createDiffusionViewModel(submitted = submitted)

        vm.onGenerate()
        advanceUntilIdle()

        assertEquals("SD15/bar.safetensors", submitted.single().checkpoint)
        assertNull(submitted.single().diffusionModel)
    }

    @Test
    fun picking_a_different_checkpoint_clears_the_family_and_restores_euler_normal() = runTest {
        val vm = createDiffusionViewModel()
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        vm.onCheckpointSelected("SDXL/foo.safetensors")

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.CHECKPOINT, state.modelSource)
        assertEquals("SDXL/foo.safetensors", state.selectedCheckpoint)
        assertNull(state.selectedFamily)
        assertEquals("euler", state.samplerName)
        assertEquals("normal", state.scheduler)
        // Visible fields stay as the family left them.
        assertEquals(8, state.steps)
        assertEquals(1024, state.width)
    }

    @Test
    fun picking_a_different_diffusion_model_clears_the_family() = runTest {
        val vm = createDiffusionViewModel()
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        vm.onDiffusionModelSelected(ANIMA_MODEL)

        val state = vm.uiState.value
        assertEquals(ANIMA_MODEL, state.selectedDiffusionModel)
        assertNull(state.selectedFamily)
        assertEquals("normal", state.scheduler)
        assertFalse(state.canGenerate)
    }

    @Test
    fun reselecting_the_current_diffusion_model_changes_nothing() = runTest {
        val vm = createDiffusionViewModel()
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)
        vm.onStepsChanged(12)
        val before = vm.uiState.value

        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        assertSame(before, vm.uiState.value)
    }

    @Test
    fun reselecting_the_current_checkpoint_keeps_its_family() = runTest {
        val vm = createDiffusionViewModel()
        vm.onModelFamilySelected(DiffusionModelFamily.ANIMA)
        val before = vm.uiState.value

        vm.onCheckpointSelected("SD15/bar.safetensors")

        assertSame(before, vm.uiState.value)
        assertEquals(DiffusionModelFamily.ANIMA, vm.uiState.value.selectedFamily)
        assertEquals("simple", vm.uiState.value.scheduler)
    }

    // -- Prefill with a CivitAI base model --

    private fun bridgePrefill(fileName: String, baseModel: String, steps: Int? = null) =
        PopulateGenerationFromModelUseCase()(
            prompt = "a cat",
            negativePrompt = null,
            steps = steps,
            cfgScale = null,
            seed = null,
            sampler = "DPM++ 2M Karras",
            checkpointName = fileName,
            baseModel = baseModel,
        )

    private fun ComfyUIGenerationViewModel.prefill(fileName: String, baseModel: String, steps: Int? = null) =
        applyPrefill(bridgePrefill(fileName, baseModel, steps), baseModel)

    @Test
    fun krea_2_file_in_diffusion_models_is_selected_with_the_family_and_its_defaults() = runTest {
        val vm = createDiffusionViewModel()

        vm.prefill(KREA_MODEL, KREA_BASE_MODEL)

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.DIFFUSION_MODEL, state.modelSource)
        assertEquals(KREA_MODEL, state.selectedDiffusionModel)
        assertEquals(DiffusionModelFamily.KREA_2, state.selectedFamily)
        assertEquals(8, state.steps)
        assertEquals(1.0, state.cfgScale)
        assertEquals(1024, state.width)
        assertEquals("euler", state.samplerName)
        assertEquals("simple", state.scheduler)
        assertEquals("qwen3vl_4b_fp8_scaled.safetensors", state.selectedTextEncoder)
        assertEquals("qwen_image_vae.safetensors", state.selectedVae)
        assertTrue(state.canGenerate)
    }

    @Test
    fun metadata_steps_win_over_the_family_default() = runTest {
        val vm = createDiffusionViewModel()

        vm.prefill(KREA_MODEL, KREA_BASE_MODEL, steps = 12)

        assertEquals(12, vm.uiState.value.steps)
        assertEquals(1.0, vm.uiState.value.cfgScale)
        assertEquals(DiffusionModelFamily.KREA_2, vm.uiState.value.selectedFamily)
    }

    @Test
    fun krea_2_file_only_in_checkpoints_is_generated_as_a_checkpoint_with_the_family_defaults() = runTest {
        val submitted = mutableListOf<ComfyUIGenerationParams>()
        val vm = createDiffusionViewModel(submitted = submitted)

        vm.prefill("foo.safetensors", KREA_BASE_MODEL)
        vm.onGenerate()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.CHECKPOINT, state.modelSource)
        assertEquals("SDXL/foo.safetensors", state.selectedCheckpoint)
        assertEquals(DiffusionModelFamily.KREA_2, state.selectedFamily)
        val params = submitted.single()
        assertEquals("SDXL/foo.safetensors", params.checkpoint)
        assertNull(params.diffusionModel)
        assertEquals(8, params.steps)
        assertEquals(1.0, params.cfgScale)
        assertEquals("simple", params.scheduler)
    }

    @Test
    fun krea_2_file_on_neither_list_selects_no_model() = runTest {
        val vm = createDiffusionViewModel()
        vm.onDiffusionModelSelected(ANIMA_MODEL)

        vm.prefill("missing.safetensors", KREA_BASE_MODEL)

        val state = vm.uiState.value
        assertEquals("", state.selectedCheckpoint)
        assertEquals("", state.selectedDiffusionModel)
        assertEquals(DiffusionModelFamily.KREA_2, state.selectedFamily)
        assertFalse(state.canGenerate)
    }

    @Test
    fun unknown_base_model_with_a_requested_checkpoint_is_matched_after_load() = runTest {
        val checkpoints = CompletableDeferred<List<String>>()
        val vm = createViewModel(checkpoints)
        advanceUntilIdle()

        vm.prefill("FOO.safetensors", SDXL_BASE_MODEL)
        checkpoints.complete(listOf("SD15/bar.safetensors", "SDXL/foo.safetensors"))
        advanceUntilIdle()

        assertEquals("SDXL/foo.safetensors", vm.uiState.value.selectedCheckpoint)
        assertNull(vm.uiState.value.selectedFamily)
        assertEquals("normal", vm.uiState.value.scheduler)
        assertEquals(20, vm.uiState.value.steps)
    }

    @Test
    fun unknown_base_model_with_an_unknown_checkpoint_falls_back_to_first_entry() = runTest {
        val vm = createDiffusionViewModel()
        vm.onCheckpointSelected("SDXL/foo.safetensors")

        vm.prefill("missing.safetensors", SDXL_BASE_MODEL)

        assertEquals(GenerationModelSource.CHECKPOINT, vm.uiState.value.modelSource)
        assertEquals("SD15/bar.safetensors", vm.uiState.value.selectedCheckpoint)
    }

    @Test
    fun unknown_base_model_file_in_diffusion_models_is_selected_without_a_family() = runTest {
        val vm = createDiffusionViewModel()

        vm.prefill(ANIMA_MODEL, "Illustrious")

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.DIFFUSION_MODEL, state.modelSource)
        assertEquals(ANIMA_MODEL, state.selectedDiffusionModel)
        assertNull(state.selectedFamily)
        assertFalse(state.canGenerate)
    }

    @Test
    fun checkpoint_prefill_switches_away_from_a_selected_diffusion_model() = runTest {
        val submitted = mutableListOf<ComfyUIGenerationParams>()
        val vm = createDiffusionViewModel(submitted = submitted)
        vm.onDiffusionModelSelected(KREA_MODEL)
        vm.onModelFamilySelected(DiffusionModelFamily.KREA_2)

        vm.applyPrefill(ComfyUIGenerationParams(checkpoint = "foo.safetensors", prompt = "p"))
        vm.onGenerate()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.CHECKPOINT, state.modelSource)
        assertNull(state.selectedFamily)
        assertEquals("normal", state.scheduler)
        assertEquals("SDXL/foo.safetensors", submitted.single().checkpoint)
        assertNull(submitted.single().diffusionModel)
    }

    @Test
    fun prefill_waits_for_the_diffusion_model_list() = runTest {
        val resources = CompletableDeferred<DiffusionModelResources>()
        val vm = createViewModel(
            loaded("SD15/bar.safetensors", "SDXL/foo.safetensors"),
            diffusionModelResources = { resources.await() },
        )
        advanceUntilIdle()

        vm.prefill(KREA_MODEL, KREA_BASE_MODEL)
        assertEquals(GenerationModelSource.CHECKPOINT, vm.uiState.value.modelSource)
        assertNull(vm.uiState.value.selectedFamily)

        resources.complete(
            DiffusionModelResources(diffusionModels = listOf(KREA_MODEL), clipTypes = listOf("krea2")),
        )
        advanceUntilIdle()

        assertEquals(GenerationModelSource.DIFFUSION_MODEL, vm.uiState.value.modelSource)
        assertEquals(KREA_MODEL, vm.uiState.value.selectedDiffusionModel)
        assertEquals(DiffusionModelFamily.KREA_2, vm.uiState.value.selectedFamily)
    }

    @Test
    fun failed_diffusion_model_load_counts_as_an_empty_list() = runTest {
        val checkpoints = CompletableDeferred<List<String>>()
        val vm = createViewModel(checkpoints, diffusionModelResources = { error("server unreachable") })
        advanceUntilIdle()

        vm.prefill("foo.safetensors", KREA_BASE_MODEL)
        checkpoints.complete(listOf("SD15/bar.safetensors", "SDXL/foo.safetensors"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(GenerationModelSource.CHECKPOINT, state.modelSource)
        assertEquals("SDXL/foo.safetensors", state.selectedCheckpoint)
        assertEquals(DiffusionModelFamily.KREA_2, state.selectedFamily)
    }

    private companion object {
        const val KREA_BASE_MODEL = "Krea 2"
        const val SDXL_BASE_MODEL = "SDXL 1.0"
        const val KREA_MODEL = "krea2_turbo_fp8_scaled.safetensors"
        const val ANIMA_MODEL = "novaAnimeAM_v5029B.safetensors"
        const val RAW_WORKFLOW =
            """{"3":{"class_type":"KSampler","inputs":{"seed":1,"steps":20}}}"""
    }
}
