package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PopulateGenerationFromModelUseCase
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ConnectionOnboardingViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.ui.comfyhub.ComfyHubBrowserScreen
import com.riox432.civitdeck.ui.comfyhub.ComfyHubDetailScreen
import com.riox432.civitdeck.ui.comfyui.CivitaiLinkSettingsScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIOutputDetailScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIQueueScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.ConnectionOnboardingScreen
import com.riox432.civitdeck.ui.comfyui.MaskEditorScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.TemplateParameterScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateEditorScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateScreen
import com.riox432.civitdeck.ui.create.CreateHubCallbacks
import com.riox432.civitdeck.ui.create.CreateHubScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerImageDetailScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsScreen
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Shared images list between gallery and detail entries to avoid ViewModel re-creation. */
private var serverGalleryImagesHolder: List<ServerImage> = emptyList()

internal fun EntryProviderScope<Any>.createHubEntry(backStack: MutableList<Any>) {
    entry<CreateHubRoute> {
        CreateHubScreen(
            callbacks = CreateHubCallbacks(
                onNavigateToComfyUI = { backStack.add(ComfyUISettingsRoute) },
                onNavigateToComfyUIGeneration = { backStack.add(ComfyUIGenerationRoute) },
                onNavigateToComfyUIQueue = { backStack.add(ComfyUIQueueRoute) },
                onNavigateToComfyUIHistory = { backStack.add(ComfyUIHistoryRoute) },
                onNavigateToOnboarding = { backStack.add(ConnectionOnboardingRoute()) },
                onNavigateToSDWebUI = { backStack.add(SDWebUISettingsRoute) },
                onNavigateToSDWebUIGeneration = { backStack.add(SDWebUIGenerationRoute) },
                onNavigateToExternalServer = { backStack.add(ExternalServerSettingsRoute) },
                onNavigateToExternalServerGallery = { backStack.add(ExternalServerGalleryRoute) },
                onNavigateToModelFiles = { backStack.add(ModelFileBrowserRoute) },
            ),
        )
    }
}

internal fun EntryProviderScope<Any>.comfyUIEntries(backStack: MutableList<Any>) {
    entry<CivitaiLinkSettingsRoute> {
        val viewModel: CivitaiLinkSettingsViewModel = koinViewModel()
        CivitaiLinkSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<ComfyUISettingsRoute> {
        val viewModel: ComfyUISettingsViewModel = koinViewModel()
        ComfyUISettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToGeneration = { backStack.add(ComfyUIGenerationRoute) },
            onNavigateToHistory = { backStack.add(ComfyUIHistoryRoute) },
            onNavigateToOnboarding = { backStack.add(ConnectionOnboardingRoute()) },
            onReviewCertificate = { id -> backStack.add(ConnectionOnboardingRoute(reviewConnectionId = id)) },
        )
    }
    connectionOnboardingEntry(backStack)
    sdWebUIEntries(backStack)
    entry<ComfyUIGenerationRoute> {
        val viewModel: ComfyUIGenerationViewModel = koinViewModel()
        ComfyUIGenerationWithTemplatePicker(
            viewModel = viewModel,
            onBack = { backStack.popIfNotRoot() },
            onNavigateToMaskEditor = { url, w, h ->
                backStack.add(MaskEditorRoute(url, w, h))
            },
        )
    }
    entry<ComfyUIQueueRoute> {
        val viewModel: ComfyUIQueueViewModel = koinViewModel()
        ComfyUIQueueScreen(viewModel = viewModel, onBack = { backStack.removeLastOrNull() })
    }
    entry<ComfyUIBridgeRoute> { key ->
        val viewModel: ComfyUIGenerationViewModel = koinViewModel(
            key = "bridge_${key.modelId}_${key.versionId}",
        )
        ApplyBridgePrefillOnce(route = key, viewModel = viewModel)
        ComfyUIGenerationWithTemplatePicker(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToMaskEditor = { url, w, h ->
                backStack.add(MaskEditorRoute(url, w, h))
            },
        )
    }
    maskEditorEntry(backStack)
    workflowTemplateEntries(backStack)
    comfyHubEntries(backStack)
    comfyUIHistoryEntries(backStack)
}

// The entry recomposes when the user returns from a pushed screen, so the flag is saveable to keep
// a second prefill from overwriting their edits.
@Composable
private fun ApplyBridgePrefillOnce(route: ComfyUIBridgeRoute, viewModel: ComfyUIGenerationViewModel) {
    val populateFromModel: PopulateGenerationFromModelUseCase = koinInject()
    var prefillApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (prefillApplied) return@LaunchedEffect
        viewModel.applyPrefill(
            populateFromModel(
                prompt = route.prompt,
                negativePrompt = route.negativePrompt,
                steps = route.steps,
                cfgScale = route.cfgScale,
                seed = route.seed,
                sampler = route.sampler,
                checkpointName = route.checkpointFileName ?: "",
            ),
        )
        prefillApplied = true
    }
}

// The picker lives inside the generation entry because Navigation3 1.0.0 has no way to return a
// result from another entry, and separate entries do not share a ViewModelStore.
@Composable
private fun ComfyUIGenerationWithTemplatePicker(
    viewModel: ComfyUIGenerationViewModel,
    onBack: () -> Unit,
    onNavigateToMaskEditor: (String, Int, Int) -> Unit,
) {
    var showTemplatePicker by rememberSaveable { mutableStateOf(false) }
    val behaviorViewModel: AppBehaviorSettingsViewModel = koinViewModel()
    val behaviorState by behaviorViewModel.uiState.collectAsStateWithLifecycle()
    ComfyUIGenerationScreen(
        viewModel = viewModel,
        onBack = onBack,
        generationNotificationsEnabled = behaviorState.generationNotificationsEnabled,
        onLoadTemplate = { showTemplatePicker = true },
        onNavigateToMaskEditor = onNavigateToMaskEditor,
    )
    if (showTemplatePicker) {
        WorkflowTemplatePickerDialog(
            onSelectTemplate = { template ->
                viewModel.onTemplateApplied(template, emptyMap())
                showTemplatePicker = false
            },
            onDismiss = { showTemplatePicker = false },
        )
    }
}

@Composable
private fun WorkflowTemplatePickerDialog(
    onSelectTemplate: (WorkflowTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val templateViewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = templateViewModel,
            onBack = onDismiss,
            onCreateTemplate = {},
            onEditTemplate = {},
            onSelectTemplate = onSelectTemplate,
        )
    }
}

private fun EntryProviderScope<Any>.connectionOnboardingEntry(backStack: MutableList<Any>) {
    entry<ConnectionOnboardingRoute> { key ->
        val viewModel: ConnectionOnboardingViewModel = koinViewModel()
        key.reviewConnectionId?.let { ReviewCertificateEffect(it, viewModel) }
        ConnectionOnboardingScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onConnected = { backStack.removeLastOrNull() },
        )
    }
}

// Settings offers the review only for the active connection, so the active row is resolved
// instead of a lookup by id. If another row became active meanwhile, onboarding stays on the
// method picker rather than testing the wrong server.
@Composable
private fun ReviewCertificateEffect(connectionId: Long, viewModel: ConnectionOnboardingViewModel) {
    val observeActiveConnection = koinInject<ObserveActiveComfyUIConnectionUseCase>()
    // Saved across configuration changes so the review is not restarted over the ViewModel's result.
    var started by rememberSaveable(connectionId) { mutableStateOf(false) }
    LaunchedEffect(connectionId) {
        if (started) return@LaunchedEffect
        val active = observeActiveConnection().first()
        started = true
        if (active?.id == connectionId) viewModel.onReviewCertificate(active)
    }
}

private fun EntryProviderScope<Any>.sdWebUIEntries(backStack: MutableList<Any>) {
    entry<SDWebUISettingsRoute> {
        val viewModel: SDWebUISettingsViewModel = koinViewModel()
        SDWebUISettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToGeneration = { backStack.add(SDWebUIGenerationRoute) },
        )
    }
    entry<SDWebUIGenerationRoute> {
        val viewModel: SDWebUIGenerationViewModel = koinViewModel()
        SDWebUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.maskEditorEntry(backStack: MutableList<Any>) {
    entry<MaskEditorRoute> { key ->
        val viewModel: MaskEditorViewModel = koinViewModel()
        MaskEditorScreen(
            viewModel = viewModel,
            sourceImageUrl = key.sourceImageUrl,
            imageWidth = key.imageWidth,
            imageHeight = key.imageHeight,
            onBack = { backStack.removeLastOrNull() },
            onMaskReady = { filename ->
                // Pop back and let the generation screen know
                backStack.removeLastOrNull()
            },
        )
    }
}

private fun EntryProviderScope<Any>.workflowTemplateEntries(backStack: MutableList<Any>) {
    entry<WorkflowTemplateLibraryRoute> {
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onCreateTemplate = { backStack.add(WorkflowTemplateEditorRoute(templateId = 0L)) },
            onEditTemplate = { template -> backStack.add(WorkflowTemplateEditorRoute(templateId = template.id)) },
            onSelectTemplate = { template -> backStack.add(TemplateParameterRoute(templateId = template.id)) },
        )
    }
    entry<WorkflowTemplateEditorRoute> { key ->
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        val template = if (key.templateId == 0L) {
            WorkflowTemplateViewModel.emptyTemplate()
        } else {
            viewModel.uiState.value.templates.find { it.id == key.templateId }
                ?: WorkflowTemplateViewModel.emptyTemplate()
        }
        WorkflowTemplateEditorScreen(
            initialTemplate = template,
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<TemplateParameterRoute> { key ->
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        val template = viewModel.uiState.value.templates.find { it.id == key.templateId }
            ?: WorkflowTemplateViewModel.emptyTemplate()
        TemplateParameterScreen(
            template = template,
            onBack = { backStack.removeLastOrNull() },
            onApply = {
                // Applied template values will be forwarded to ComfyUI generation
                backStack.removeLastOrNull()
            },
        )
    }
}

private fun EntryProviderScope<Any>.comfyHubEntries(backStack: MutableList<Any>) {
    entry<ComfyHubBrowserRoute> {
        val viewModel: ComfyHubBrowserViewModel = koinViewModel()
        ComfyHubBrowserScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onWorkflowClick = { workflowId -> backStack.add(ComfyHubDetailRoute(workflowId)) },
        )
    }
    entry<ComfyHubDetailRoute> { key ->
        val viewModel: ComfyHubDetailViewModel = koinViewModel(
            key = "comfyhub_${key.workflowId}",
        ) { parametersOf(key.workflowId) }
        ComfyHubDetailScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.comfyUIHistoryEntries(backStack: MutableList<Any>) {
    entry<ComfyUIHistoryRoute> {
        val viewModel: ComfyUIHistoryViewModel = koinViewModel()
        ComfyUIHistoryScreen(
            viewModel = viewModel,
            onBack = { backStack.popIfNotRoot() },
            onImageClick = { image ->
                // The detail pager indexes into this list, so it must never be empty.
                val images = viewModel.filteredImages().ifEmpty { listOf(image) }
                backStack.add(ComfyUIOutputDetailRoute(image.id, images))
            },
        )
    }
    // The default content key is the route's toString(), which would serialize the whole list.
    entry<ComfyUIOutputDetailRoute>(clazzContentKey = { "comfyui_output_${it.imageId}" }) { key ->
        // Display comes from the route's snapshot; this VM serves only the save, dataset and hashtag actions.
        val actionsViewModel: ComfyUIHistoryViewModel = koinViewModel()
        ComfyUIOutputDetailScreen(
            images = key.images,
            initialIndex = key.images.indexOfFirst { it.id == key.imageId }.coerceAtLeast(0),
            viewModel = actionsViewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

internal fun EntryProviderScope<Any>.externalServerEntries(backStack: MutableList<Any>) {
    entry<ExternalServerSettingsRoute> {
        val viewModel: ExternalServerSettingsViewModel = koinViewModel()

        @Suppress("UnusedPrivateProperty")
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        ExternalServerSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToGallery = {
                backStack.add(ExternalServerGalleryRoute)
            },
        )
    }
    entry<ExternalServerGalleryRoute> {
        val settingsVm: ExternalServerSettingsViewModel = koinViewModel()
        val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
        val galleryVm: ExternalServerGalleryViewModel = koinViewModel()
        val state by galleryVm.uiState.collectAsStateWithLifecycle()
        // Share images with the detail entry via file-level holder
        LaunchedEffect(state.images) { serverGalleryImagesHolder = state.images }
        ExternalServerGalleryScreen(
            viewModel = galleryVm,
            serverName = settingsState.activeConfig?.name ?: "Gallery",
            onBack = { backStack.popIfNotRoot() },
            onNavigateToImageDetail = { image ->
                val index = state.images.indexOf(image)
                backStack.add(ExternalServerImageDetailRoute(index.coerceAtLeast(0)))
            },
        )
    }
    entry<ExternalServerImageDetailRoute> { route ->
        ExternalServerImageDetailScreen(
            images = serverGalleryImagesHolder,
            initialIndex = route.initialIndex,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}
