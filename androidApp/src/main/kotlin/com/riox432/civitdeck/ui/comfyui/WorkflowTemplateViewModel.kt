package com.riox432.civitdeck.ui.comfyui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.TemplateVariable
import com.riox432.civitdeck.domain.model.TemplateVariableType
import com.riox432.civitdeck.domain.model.WorkflowTemplate
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val exportedJson: String? = null,
    val importError: String? = null,
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
                    _uiState.update { it.copy(templates = templates, isLoading = false) }
                }
        }
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

        fun emptyTemplate(type: WorkflowTemplateType = WorkflowTemplateType.TXT2IMG): WorkflowTemplate =
            WorkflowTemplate(
                id = 0L,
                name = "",
                type = type,
                variables = defaultVariablesFor(type),
                isBuiltIn = false,
                createdAt = 0L,
            )

        fun defaultVariablesFor(type: WorkflowTemplateType): List<TemplateVariable> = when (type) {
            WorkflowTemplateType.TXT2IMG -> listOf(
                TemplateVariable("positive_prompt", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("negative_prompt", TemplateVariableType.TEXT, "", required = false),
                TemplateVariable("checkpoint", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("steps", TemplateVariableType.NUMBER, "20", required = false),
                TemplateVariable("cfg", TemplateVariableType.NUMBER, "7.0", required = false),
                TemplateVariable("width", TemplateVariableType.NUMBER, "512", required = false),
                TemplateVariable("height", TemplateVariableType.NUMBER, "512", required = false),
            )
            WorkflowTemplateType.IMG2IMG -> listOf(
                TemplateVariable("positive_prompt", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("negative_prompt", TemplateVariableType.TEXT, "", required = false),
                TemplateVariable("checkpoint", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("steps", TemplateVariableType.NUMBER, "20", required = false),
                TemplateVariable("cfg", TemplateVariableType.NUMBER, "7.0", required = false),
                TemplateVariable("width", TemplateVariableType.NUMBER, "512", required = false),
                TemplateVariable("height", TemplateVariableType.NUMBER, "512", required = false),
                TemplateVariable("denoise_strength", TemplateVariableType.NUMBER, "0.75", required = false),
            )
            WorkflowTemplateType.INPAINTING -> listOf(
                TemplateVariable("positive_prompt", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("negative_prompt", TemplateVariableType.TEXT, "", required = false),
                TemplateVariable("checkpoint", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("steps", TemplateVariableType.NUMBER, "20", required = false),
                TemplateVariable("cfg", TemplateVariableType.NUMBER, "7.0", required = false),
                TemplateVariable("denoise_strength", TemplateVariableType.NUMBER, "1.0", required = false),
            )
            WorkflowTemplateType.UPSCALE -> listOf(
                TemplateVariable("input_image", TemplateVariableType.TEXT, "", required = true),
                TemplateVariable("upscale_factor", TemplateVariableType.NUMBER, "2", required = false),
            )
        }
    }
}
