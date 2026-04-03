package com.riox432.civitdeck.ui.comfyui.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
import com.riox432.civitdeck.domain.model.WorkflowTemplateCategory
import com.riox432.civitdeck.domain.model.WorkflowTemplateType
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetWorkflowTemplatesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportWorkflowTemplateUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveWorkflowTemplateUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopWorkflowTemplateUiState(
    val templates: List<WorkflowTemplate> = emptyList(),
    val filteredTemplates: List<WorkflowTemplate> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val exportedJson: String? = null,
    val importError: String? = null,
    val searchQuery: String = "",
    val selectedCategory: WorkflowTemplateCategory? = null,
    val selectedType: WorkflowTemplateType? = null,
)

@Suppress("TooManyFunctions")
class DesktopWorkflowTemplateViewModel(
    private val getTemplates: GetWorkflowTemplatesUseCase,
    private val saveTemplate: SaveWorkflowTemplateUseCase,
    private val deleteTemplate: DeleteWorkflowTemplateUseCase,
    private val exportTemplate: ExportWorkflowTemplateUseCase,
    private val importTemplate: ImportWorkflowTemplateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesktopWorkflowTemplateUiState())
    val uiState: StateFlow<DesktopWorkflowTemplateUiState> = _uiState

    init {
        observeTemplates()
    }

    private fun observeTemplates() {
        viewModelScope.launch {
            getTemplates()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { templates ->
                    _uiState.update {
                        it.copy(
                            templates = templates,
                            isLoading = false,
                        ).applyFilters()
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query).applyFilters() }
    }

    fun onCategorySelected(category: WorkflowTemplateCategory?) {
        _uiState.update { it.copy(selectedCategory = category).applyFilters() }
    }

    fun onTypeSelected(type: WorkflowTemplateType?) {
        _uiState.update { it.copy(selectedType = type).applyFilters() }
    }

    fun onDeleteTemplate(id: Long) {
        viewModelScope.launch {
            try {
                deleteTemplate(id)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "Failed to delete template $id: ${e.message}")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onExportTemplate(template: WorkflowTemplate) {
        val json = exportTemplate(template)
        _uiState.update { it.copy(exportedJson = json) }
    }

    fun onDismissExport() {
        _uiState.update { it.copy(exportedJson = null) }
    }

    fun onImportTemplate(jsonString: String) {
        viewModelScope.launch {
            try {
                importTemplate(jsonString)
                _uiState.update { it.copy(importError = null) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "Failed to import template: ${e.message}")
                _uiState.update { it.copy(importError = e.message) }
            }
        }
    }

    fun onDismissImportError() {
        _uiState.update { it.copy(importError = null) }
    }

    fun onSaveTemplate(template: WorkflowTemplate) {
        viewModelScope.launch {
            try {
                saveTemplate(template)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "Failed to save template: ${e.message}")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "DesktopWorkflowTemplateVM"

        fun emptyTemplate(
            type: WorkflowTemplateType = WorkflowTemplateType.TXT2IMG,
        ): WorkflowTemplate =
            WorkflowTemplate(
                id = 0L,
                name = "",
                type = type,
                variables = defaultVariablesFor(type),
                isBuiltIn = false,
                createdAt = 0L,
            )

        fun defaultVariablesFor(type: WorkflowTemplateType): List<TemplateVariable> =
            when (type) {
                WorkflowTemplateType.TXT2IMG -> txt2imgVars()
                WorkflowTemplateType.IMG2IMG -> txt2imgVars() + denoiseVar()
                WorkflowTemplateType.INPAINTING -> inpaintingVars()
                WorkflowTemplateType.UPSCALE -> upscaleVars()
                WorkflowTemplateType.LORA -> loraVars()
            }

        private fun txt2imgVars() = listOf(
            promptVar(), negativePromptVar(), checkpointVar(),
            stepsVar(), cfgVar(), widthVar(), heightVar(),
        )

        private fun inpaintingVars() = listOf(
            promptVar(), negativePromptVar(), checkpointVar(),
            stepsVar(), cfgVar(), denoiseVar(default = "1.0"),
        )

        private fun upscaleVars() = listOf(
            TemplateVariable("input_image", "Input Image", "", TemplateVariableType.TEXT, ""),
            TemplateVariable(
                "upscale_factor", "Upscale Factor", "",
                TemplateVariableType.SLIDER, "2", 1.0, 4.0, 0.5,
            ),
        )

        private fun loraVars() = listOf(
            promptVar(), negativePromptVar(), checkpointVar(),
            TemplateVariable("lora_name", "LoRA Model", "", TemplateVariableType.TEXT, "", required = true),
            TemplateVariable(
                "lora_strength", "LoRA Strength", "",
                TemplateVariableType.SLIDER, "0.8", 0.0, 2.0, 0.05,
            ),
            stepsVar(), cfgVar(), widthVar(), heightVar(),
        )

        private fun promptVar() = TemplateVariable(
            "positive_prompt", "Prompt", "", TemplateVariableType.TEXT, "", required = true,
        )

        private fun negativePromptVar() = TemplateVariable(
            "negative_prompt", "Negative Prompt", "", TemplateVariableType.TEXT, "", required = false,
        )

        private fun checkpointVar() = TemplateVariable(
            "checkpoint", "Checkpoint", "", TemplateVariableType.TEXT, "", required = true,
        )

        private fun stepsVar() = TemplateVariable(
            "steps", "Steps", "", TemplateVariableType.SLIDER, "20", 1.0, 150.0, 1.0,
        )

        private fun cfgVar() = TemplateVariable(
            "cfg", "CFG Scale", "", TemplateVariableType.SLIDER, "7.0", 1.0, 30.0, 0.5,
        )

        private fun widthVar() = TemplateVariable(
            "width", "Width", "", TemplateVariableType.SELECT, "512",
            options = listOf("256", "384", "512", "640", "768", "832", "896", "1024", "1280"),
        )

        private fun heightVar() = TemplateVariable(
            "height", "Height", "", TemplateVariableType.SELECT, "512",
            options = listOf("256", "384", "512", "640", "768", "832", "896", "1024", "1280"),
        )

        private fun denoiseVar(default: String = "0.75") = TemplateVariable(
            "denoise_strength", "Denoise Strength", "",
            TemplateVariableType.SLIDER, default, 0.0, 1.0, 0.05,
        )
    }
}

private fun DesktopWorkflowTemplateUiState.applyFilters(): DesktopWorkflowTemplateUiState {
    val filtered = templates.filter { template ->
        val matchesSearch = searchQuery.isBlank() ||
            template.name.contains(searchQuery, ignoreCase = true) ||
            template.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || template.category == selectedCategory
        val matchesType = selectedType == null || template.type == selectedType
        matchesSearch && matchesCategory && matchesType
    }
    return copy(filteredTemplates = filtered)
}
