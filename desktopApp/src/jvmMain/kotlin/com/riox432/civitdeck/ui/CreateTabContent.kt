package com.riox432.civitdeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.ui.comfyui.ComfyUIGenerationViewModel
import com.riox432.civitdeck.ui.comfyui.ComfyUIHistoryViewModel
import com.riox432.civitdeck.ui.comfyui.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.comfyui.SDWebUIGenerationViewModel
import com.riox432.civitdeck.ui.comfyui.SDWebUISettingsViewModel
import com.riox432.civitdeck.ui.comfyui.template.DesktopTemplateParameterScreen
import com.riox432.civitdeck.ui.comfyui.template.DesktopWorkflowTemplateEditorScreen
import com.riox432.civitdeck.ui.comfyui.template.DesktopWorkflowTemplateScreen
import com.riox432.civitdeck.ui.comfyui.template.DesktopWorkflowTemplateViewModel
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryViewModel
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.create.DesktopCreateHubScreen
import com.riox432.civitdeck.ui.settings.ComfyUIGenerationSection
import com.riox432.civitdeck.ui.settings.ComfyUIHistorySection
import com.riox432.civitdeck.ui.settings.ComfyUISettingsSection
import com.riox432.civitdeck.ui.settings.ExternalServerGallerySection
import com.riox432.civitdeck.ui.settings.ExternalServerSettingsSection
import com.riox432.civitdeck.ui.settings.SDWebUIGenerationSection
import com.riox432.civitdeck.ui.settings.SDWebUISettingsSection
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private enum class CreateSection(val title: String) {
    ComfyUI("ComfyUI"),
    SDWebUI("SD WebUI"),
    ExternalServer("External Server"),
}

/** Nested route for workflow template navigation within Create tab */
private sealed class CreateRoute {
    data object Hub : CreateRoute()
    data class Section(val section: CreateSection) : CreateRoute()
    data object WorkflowTemplates : CreateRoute()
    data class WorkflowTemplateEditor(val template: WorkflowTemplate) : CreateRoute()
    data class TemplateParameter(val template: WorkflowTemplate) : CreateRoute()
}

@Composable
fun CreateTabContent(
    modifier: Modifier = Modifier,
) {
    var route by remember { mutableStateOf<CreateRoute>(CreateRoute.Hub) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val currentRoute = route) {
            is CreateRoute.Hub -> {
                DesktopCreateHubScreen(
                    onComfyUIClick = { route = CreateRoute.Section(CreateSection.ComfyUI) },
                    onSDWebUIClick = { route = CreateRoute.Section(CreateSection.SDWebUI) },
                    onExternalServerClick = { route = CreateRoute.Section(CreateSection.ExternalServer) },
                    onWorkflowTemplatesClick = { route = CreateRoute.WorkflowTemplates },
                )
            }
            is CreateRoute.Section -> {
                CreateSectionDetail(
                    section = currentRoute.section,
                    onBack = { route = CreateRoute.Hub },
                )
            }
            is CreateRoute.WorkflowTemplates -> {
                val vm: DesktopWorkflowTemplateViewModel = koinViewModel()
                DesktopWorkflowTemplateScreen(
                    viewModel = vm,
                    onBack = { route = CreateRoute.Hub },
                    onCreateTemplate = {
                        route = CreateRoute.WorkflowTemplateEditor(
                            DesktopWorkflowTemplateViewModel.emptyTemplate(),
                        )
                    },
                    onEditTemplate = { template ->
                        route = CreateRoute.WorkflowTemplateEditor(template)
                    },
                    onSelectTemplate = { template ->
                        route = CreateRoute.TemplateParameter(template)
                    },
                )
            }
            is CreateRoute.WorkflowTemplateEditor -> {
                val vm: DesktopWorkflowTemplateViewModel = koinViewModel()
                DesktopWorkflowTemplateEditorScreen(
                    initialTemplate = currentRoute.template,
                    viewModel = vm,
                    onBack = { route = CreateRoute.WorkflowTemplates },
                )
            }
            is CreateRoute.TemplateParameter -> {
                DesktopTemplateParameterScreen(
                    template = currentRoute.template,
                    onBack = { route = CreateRoute.WorkflowTemplates },
                    onApply = { /* Apply params to generation - handled by ComfyUI integration */ },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateSectionDetail(
    section: CreateSection,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(section.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (section) {
                CreateSection.ComfyUI -> {
                    val settingsVm: ComfyUISettingsViewModel = koinViewModel()
                    val genVm: ComfyUIGenerationViewModel = koinViewModel()
                    val historyVm: ComfyUIHistoryViewModel = koinViewModel()
                    ComfyUISettingsSection(settingsVm)
                    ComfyUIGenerationSection(genVm)
                    ComfyUIHistorySection(historyVm)
                }
                CreateSection.SDWebUI -> {
                    val settingsVm: SDWebUISettingsViewModel = koinViewModel()
                    val genVm: SDWebUIGenerationViewModel = koinViewModel()
                    SDWebUISettingsSection(settingsVm)
                    SDWebUIGenerationSection(genVm)
                }
                CreateSection.ExternalServer -> {
                    val settingsVm: ExternalServerSettingsViewModel = koinViewModel()
                    val galleryVm: ExternalServerGalleryViewModel = koinViewModel()
                    ExternalServerSettingsSection(settingsVm)
                    ExternalServerGallerySection(galleryVm)
                }
            }
        }
    }
}
