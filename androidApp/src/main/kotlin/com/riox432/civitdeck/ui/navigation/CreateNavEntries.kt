package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.comfyhub.ComfyHubBrowserScreen
import com.riox432.civitdeck.ui.comfyhub.ComfyHubBrowserViewModel
import com.riox432.civitdeck.ui.comfyhub.ComfyHubDetailScreen
import com.riox432.civitdeck.ui.comfyhub.ComfyHubDetailViewModel
import com.riox432.civitdeck.ui.comfyui.CivitaiLinkSettingsScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIOutputDetailScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUIQueueScreen
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationScreen
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateEditorScreen
import com.riox432.civitdeck.ui.comfyui.WorkflowTemplateScreen
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.create.CreateHubScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerImageDetailScreen
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsScreen
import com.riox432.civitdeck.ui.share.ShareViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
    entry<ComfyUIGenerationRoute> {
        val viewModel: ComfyUIGenerationViewModel = koinViewModel()
        ComfyUIGenerationScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onLoadTemplate = { backStack.add(WorkflowTemplatePickerRoute) },
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
        )
    }
    workflowTemplateEntries(backStack)
    comfyHubEntries(backStack)
    comfyUIHistoryEntries(backStack)
}

private fun EntryProviderScope<Any>.workflowTemplateEntries(backStack: MutableList<Any>) {
    entry<WorkflowTemplateLibraryRoute> {
        val viewModel: WorkflowTemplateViewModel = koinViewModel()
        WorkflowTemplateScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onCreateTemplate = { backStack.add(WorkflowTemplateEditorRoute(templateId = 0L)) },
            onEditTemplate = { template -> backStack.add(WorkflowTemplateEditorRoute(templateId = template.id)) },
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
        ExternalServerGalleryScreen(
            viewModel = galleryVm,
            serverName = settingsState.activeConfig?.name ?: "Gallery",
            onBack = { backStack.removeLastOrNull() },
            onNavigateToImageDetail = { image ->
                backStack.add(ExternalServerImageDetailRoute(image.id))
            },
        )
    }
    entry<ExternalServerImageDetailRoute> { route ->
        val galleryVm: ExternalServerGalleryViewModel = koinViewModel()
        val shareVm: ShareViewModel = koinViewModel()
        val state by galleryVm.uiState.collectAsStateWithLifecycle()
        val shareHashtags by shareVm.hashtags.collectAsStateWithLifecycle()
        val image = state.images.find { it.id == route.imageId }
        if (image != null) {
            ExternalServerImageDetailScreen(
                image = image,
                onBack = { backStack.removeLastOrNull() },
                shareHashtags = shareHashtags,
                onToggleShareHashtag = shareVm::onToggle,
                onAddShareHashtag = shareVm::onAdd,
                onRemoveShareHashtag = shareVm::onRemove,
            )
        }
    }
}
