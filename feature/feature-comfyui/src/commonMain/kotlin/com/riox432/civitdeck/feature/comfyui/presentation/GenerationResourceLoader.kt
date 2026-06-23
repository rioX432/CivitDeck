package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.feature.comfyui.domain.usecase.ExtractWorkflowParametersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUICheckpointsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUIControlNetsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchComfyUILorasUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchObjectInfoUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads ComfyUI server resources (checkpoints, LoRAs, ControlNets) and extracts dynamic
 * workflow parameters. Extracted from [ComfyUIGenerationViewModel] to reduce function count.
 */
@Suppress("LongParameterList")
internal class GenerationResourceLoader(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val fetchCheckpoints: FetchComfyUICheckpointsUseCase,
    private val fetchLoras: FetchComfyUILorasUseCase,
    private val fetchControlNets: FetchComfyUIControlNetsUseCase,
    private val fetchObjectInfo: FetchObjectInfoUseCase,
    private val extractParameters: ExtractWorkflowParametersUseCase,
) {

    fun loadCheckpoints() {
        uiState.update { it.copy(isLoadingCheckpoints = true) }
        launchWithErrorHandling(
            tag = "Failed to load checkpoints",
            onError = { e -> uiState.update { it.copy(isLoadingCheckpoints = false, error = e.message) } },
        ) {
            val list = fetchCheckpoints()
            uiState.update {
                it.copy(
                    checkpoints = list,
                    selectedCheckpoint = list.firstOrNull() ?: "",
                    isLoadingCheckpoints = false,
                )
            }
        }
    }

    fun loadLoras() {
        uiState.update { it.copy(isLoadingLoras = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch loras",
            onError = { uiState.update { it.copy(isLoadingLoras = false) } },
        ) {
            val list = fetchLoras()
            uiState.update { it.copy(availableLoras = list, isLoadingLoras = false) }
        }
    }

    fun loadControlNets() {
        uiState.update { it.copy(isLoadingControlNets = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch control nets",
            onError = { uiState.update { it.copy(isLoadingControlNets = false) } },
        ) {
            val list = fetchControlNets()
            uiState.update { it.copy(availableControlNets = list, isLoadingControlNets = false) }
        }
    }

    fun extractWorkflowParameters(workflowJson: String) {
        uiState.update { it.copy(isLoadingParameters = true) }
        launchWithErrorHandling(
            tag = "Failed to extract workflow parameters",
            onError = { uiState.update { it.copy(isLoadingParameters = false) } },
        ) {
            val objectInfoJson = fetchObjectInfo()
            val params = extractParameters(workflowJson, objectInfoJson)
            uiState.update { it.copy(extractedParameters = params, isLoadingParameters = false) }
        }
    }

    private inline fun launchWithErrorHandling(
        tag: String,
        crossinline onError: (Exception) -> Unit,
        crossinline block: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.e(TAG, "$tag: ${e.message}")
                onError(e)
            }
        }
    }

    companion object {
        private const val TAG = "GenerationResourceLoader"
    }
}
