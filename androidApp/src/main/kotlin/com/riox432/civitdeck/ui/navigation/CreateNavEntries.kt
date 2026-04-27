package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyHubDetailViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.MaskEditorViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.comfyhub.ComfyHubBrowserScreen
import com.riox432.civitdeck.ui.comfyhub.ComfyHubDetailScreen
import com.riox432.civitdeck.ui.comfyui.CivitaiLinkSettingsScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIOutputDetailScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIQueueScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.MaskEditorScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.TemplateParameterScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateEditorScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateScreen
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.create.CreateHubScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerImageDetailScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Shared images list between gallery and detail entries to avoid ViewModel re-creation. */
private var serverGalleryImagesHolder: List<ServerImage> = emptyList()

internal fun EntryProviderScope<Any>.createHubEntry(backStack: MutableList<Any>) {
    entry<CreateHubRoute> {
        CreateHubScreen(
            onNavigateToComfyUI = { backStack.add(ComfyUISettingsRoute) },
            onNavigateToSDWebUI = { backStack.add(SDWebUISettingsRoute) },
            onNavigateToExternalServer = { backStack.add(ExternalServerSettingsRoute) },
            onNavigateToModelFiles = { backStack.add(ModelFileBrowserRoute) },
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
        )
    }
    sdWebUIEntries(backStack)
    entry<ComfyUIGenerationRoute> {
        val viewModel: ComfyUIGenerationViewModel = koinViewModel()
        ComfyUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onLoadTemplate = { backStack.add(WorkflowTemplatePickerRoute) },
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
        ComfyUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onLoadTemplate = { backStack.add(WorkflowTemplatePickerRoute) },
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
    entry<WorkflowTemplatePickerRoute> {
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onCreateTemplate = {},
            onEditTemplate = {},
            onSelectTemplate = { backStack.removeLastOrNull() },
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
            onBack = { backStack.removeLastOrNull() },
            onImageClick = { image -> backStack.add(ComfyUIOutputDetailRoute(image.id)) },
        )
    }
    entry<ComfyUIOutputDetailRoute> { key ->
        val historyViewModel: ComfyUIHistoryViewModel = koinViewModel()
        val state by historyViewModel.uiState.collectAsStateWithLifecycle()
        val images = historyViewModel.filteredImages()
        val initialIndex = images.indexOfFirst { it.id == key.imageId }.coerceAtLeast(0)
        when {
            state.isLoading && images.isEmpty() -> LoadingStateOverlay()
            images.isNotEmpty() -> ComfyUIOutputDetailScreen(
                images = images,
                initialIndex = initialIndex,
                viewModel = historyViewModel,
                onBack = { backStack.removeLastOrNull() },
            )
        }
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
            onBack = { backStack.removeLastOrNull() },
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
