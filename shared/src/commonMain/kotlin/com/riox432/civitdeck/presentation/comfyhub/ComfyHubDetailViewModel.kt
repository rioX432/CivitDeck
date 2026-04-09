package com.riox432.civitdeck.presentation.comfyhub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetComfyHubWorkflowDetailUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportComfyHubWorkflowUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ComfyHubDetailUiState(
    val workflow: ComfyHubWorkflow? = null,
    override val isLoading: Boolean = true,
    override val error: String? = null,
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val importError: String? = null,
    val nodeNames: List<String> = emptyList(),
) : UiLoadingState

class ComfyHubDetailViewModel(
    private val workflowId: String,
    private val getWorkflowDetail: GetComfyHubWorkflowDetailUseCase,
    private val importWorkflow: ImportComfyHubWorkflowUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComfyHubDetailUiState())
    val uiState: StateFlow<ComfyHubDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun retry() {
        loadDetail()
    }

    fun onImport() {
        val workflow = _uiState.value.workflow ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importError = null) }
            try {
                importWorkflow(workflow.workflowJson)
                _uiState.update { it.copy(isImporting = false, importSuccess = true) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importError = e.message ?: "Import failed",
                    )
                }
            }
        }
    }

    fun dismissImportResult() {
        _uiState.update { it.copy(importSuccess = false, importError = null) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val workflow = getWorkflowDetail(workflowId)
                val nodes = parseNodeNames(workflow.workflowJson)
                _uiState.update {
                    it.copy(workflow = workflow, nodeNames = nodes, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load workflow", isLoading = false)
                }
            }
        }
    }

    @Suppress("SwallowedException")
    private fun parseNodeNames(workflowJson: String): List<String> {
        return try {
            val jsonElement = Json.parseToJsonElement(workflowJson)
            val obj = jsonElement.jsonObject
            val names = mutableSetOf<String>()
            for ((_, value) in obj) {
                val node = (value as? JsonObject) ?: continue
                val classType = node["class_type"]?.jsonPrimitive?.content ?: ""
                if (classType.isNotEmpty()) names.add(classType)
            }
            names.sorted()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            emptyList()
        }
    }
}
