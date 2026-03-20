package com.riox432.civitdeck.ui.comfyhub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.feature.comfyui.domain.usecase.GetComfyHubWorkflowDetailUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ImportComfyHubWorkflowUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ComfyHubDetailUiState(
    val workflow: ComfyHubWorkflow? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val importError: String? = null,
    val nodeNames: List<String> = emptyList(),
)

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
            val obj = JSONObject(workflowJson)
            val names = mutableSetOf<String>()
            for (key in obj.keys()) {
                val node = obj.optJSONObject(key) ?: continue
                val classType = node.optString("class_type", "")
                if (classType.isNotEmpty()) names.add(classType)
            }
            names.sorted()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            emptyList()
        }
    }
}
