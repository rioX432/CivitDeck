package com.riox432.civitdeck.ui.comfyui

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

data class WorkflowTemplateUiState(
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
class WorkflowTemplateViewModel(
    private val getTemplates: GetWorkflowTemplatesUseCase,
    private val saveTemplate: SaveWorkflowTemplateUseCase,
    private val deleteTemplate: DeleteWorkflowTemplateUseCase,
    private val exportTemplate: ExportWorkflowTemplateUseCase,
    private val importTemplate: ImportWorkflowTemplateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkflowTemplateUiState())
    val uiState: StateFlow<WorkflowTemplateUiState> = _uiState

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
        private const val TAG = "WorkflowTemplateVM"

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

        @Suppress("LongMethod")
        fun defaultVariablesFor(type: WorkflowTemplateType): List<TemplateVariable> =
            when (type) {
                WorkflowTemplateType.TXT2IMG -> txt2imgVariables()
                WorkflowTemplateType.IMG2IMG -> img2imgVariables()
                WorkflowTemplateType.INPAINTING -> inpaintingVariables()
                WorkflowTemplateType.UPSCALE -> upscaleVariables()
                WorkflowTemplateType.LORA -> loraVariables()
            }

        private fun txt2imgVariables() = listOf(
            promptVariable(),
            negativePromptVariable(),
            checkpointVariable(),
            stepsVariable(),
            cfgVariable(),
            widthVariable(),
            heightVariable(),
        )

        private fun img2imgVariables() = txt2imgVariables() + denoiseVariable()

        private fun inpaintingVariables() = listOf(
            promptVariable(),
            negativePromptVariable(),
            checkpointVariable(),
            stepsVariable(),
            cfgVariable(),
            denoiseVariable(default = "1.0"),
        )

        private fun upscaleVariables() = listOf(
            TemplateVariable(
                name = "input_image",
                label = "Input Image",
                type = TemplateVariableType.TEXT,
                defaultValue = "",
                required = true,
            ),
            TemplateVariable(
                name = "upscale_factor",
                label = "Upscale Factor",
                type = TemplateVariableType.SLIDER,
                defaultValue = "2",
                min = 1.0,
                max = 4.0,
                step = 0.5,
            ),
        )

        private fun loraVariables() = listOf(
            promptVariable(), negativePromptVariable(), checkpointVariable(),
            TemplateVariable(
                name = "lora_name",
                label = "LoRA Model",
                type = TemplateVariableType.TEXT,
                defaultValue = "",
                required = true,
            ),
            TemplateVariable(
                name = "lora_strength",
                label = "LoRA Strength",
                type = TemplateVariableType.SLIDER,
                defaultValue = "0.8",
                min = 0.0,
                max = 2.0,
                step = 0.05,
            ),
            stepsVariable(), cfgVariable(), widthVariable(), heightVariable(),
        )

        private fun promptVariable() = TemplateVariable(
            name = "positive_prompt",
            label = "Prompt",
            type = TemplateVariableType.TEXT,
            defaultValue = "",
            required = true,
        )

        private fun negativePromptVariable() = TemplateVariable(
            name = "negative_prompt",
            label = "Negative Prompt",
            type = TemplateVariableType.TEXT,
            defaultValue = "",
            required = false,
        )

        private fun checkpointVariable() = TemplateVariable(
            name = "checkpoint",
            label = "Checkpoint",
            type = TemplateVariableType.TEXT,
            defaultValue = "",
            required = true,
        )

        private fun stepsVariable() = TemplateVariable(
            name = "steps",
            label = "Steps",
            type = TemplateVariableType.SLIDER,
            defaultValue = "20",
            min = 1.0,
            max = 150.0,
            step = 1.0,
        )

        private fun cfgVariable() = TemplateVariable(
            name = "cfg",
            label = "CFG Scale",
            type = TemplateVariableType.SLIDER,
            defaultValue = "7.0",
            min = 1.0,
            max = 30.0,
            step = 0.5,
        )

        private fun widthVariable() = TemplateVariable(
            name = "width",
            label = "Width",
            type = TemplateVariableType.SELECT,
            defaultValue = "512",
            options = listOf("256", "384", "512", "640", "768", "832", "896", "1024", "1280"),
        )

        private fun heightVariable() = TemplateVariable(
            name = "height",
            label = "Height",
            type = TemplateVariableType.SELECT,
            defaultValue = "512",
            options = listOf("256", "384", "512", "640", "768", "832", "896", "1024", "1280"),
        )

        private fun denoiseVariable(default: String = "0.75") = TemplateVariable(
            name = "denoise_strength",
            label = "Denoise Strength",
            type = TemplateVariableType.SLIDER,
            defaultValue = default,
            min = 0.0,
            max = 1.0,
            step = 0.05,
        )
    }
}

private fun WorkflowTemplateUiState.applyFilters(): WorkflowTemplateUiState {
    val filtered = templates.filter { template ->
        val matchesSearch = searchQuery.isBlank() ||
            template.name.contains(searchQuery, ignoreCase = true) ||
            template.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null ||
            template.category == selectedCategory
        val matchesType = selectedType == null || template.type == selectedType
        matchesSearch && matchesCategory && matchesType
    }
    return copy(filteredTemplates = filtered)
}
