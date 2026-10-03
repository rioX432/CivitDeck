package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.DiffusionModelFamily
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads ComfyUI server resources (checkpoints, LoRAs, ControlNets, split-loader model files)
 * and extracts dynamic workflow parameters. Extracted from [ComfyUIGenerationViewModel] to
 * reduce function count.
 */
internal class GenerationResourceLoader(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val useCases: GenerationResourceUseCases,
    private val modelSelector: GenerationModelSelector,
) {

    // A prefill model requested before both server lists have settled; a failed load settles
    // its list as empty, so the request is never held forever.
    private var pendingModel: PendingModel? = null
    private var checkpointsSettled = false
    private var diffusionModelsSettled = false

    fun loadCheckpoints() {
        uiState.update { it.copy(isLoadingCheckpoints = true) }
        launchWithErrorHandling(
            tag = "Failed to load checkpoints",
            onError = { e ->
                uiState.update { it.copy(isLoadingCheckpoints = false, error = e.message) }
                checkpointsSettled = true
                selectPendingModel()
            },
        ) {
            val list = useCases.fetchCheckpoints()
            uiState.update {
                it.copy(
                    checkpoints = list,
                    selectedCheckpoint = list.firstOrNull() ?: "",
                    isLoadingCheckpoints = false,
                )
            }
            checkpointsSettled = true
            selectPendingModel()
        }
    }

    /**
     * Selects the prefill file [fileName] for [family] through
     * [GenerationModelSelector.selectPrefilledModel]. The checkpoint and diffusion model lists
     * decide which loader opens the file, so the request waits until both have settled.
     */
    fun requestModel(fileName: String, family: DiffusionModelFamily?) {
        pendingModel = PendingModel(fileName, family)
        selectPendingModel()
    }

    private fun selectPendingModel() {
        val request = pendingModel ?: return
        if (!checkpointsSettled || !diffusionModelsSettled) return
        pendingModel = null
        modelSelector.selectPrefilledModel(request.fileName, request.family)
    }

    fun loadLoras() {
        uiState.update { it.copy(isLoadingLoras = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch loras",
            onError = { uiState.update { it.copy(isLoadingLoras = false) } },
        ) {
            val list = useCases.fetchLoras()
            uiState.update { it.copy(availableLoras = list, isLoadingLoras = false) }
        }
    }

    fun loadControlNets() {
        uiState.update { it.copy(isLoadingControlNets = true) }
        launchWithErrorHandling(
            tag = "Failed to fetch control nets",
            onError = { uiState.update { it.copy(isLoadingControlNets = false) } },
        ) {
            val list = useCases.fetchControlNets()
            uiState.update { it.copy(availableControlNets = list, isLoadingControlNets = false) }
        }
    }

    // Errors are ignored like LoRAs: a server without the split loaders must still run checkpoints.
    fun loadDiffusionModelResources() {
        launchWithErrorHandling(
            tag = "Failed to fetch diffusion model resources",
            onError = {
                diffusionModelsSettled = true
                selectPendingModel()
            },
        ) {
            val resources = useCases.fetchDiffusionModelResources()
            uiState.update {
                it.copy(
                    diffusionModels = resources.diffusionModels,
                    textEncoders = resources.textEncoders,
                    vaes = resources.vaes,
                    serverClipTypes = resources.clipTypes,
                )
            }
            diffusionModelsSettled = true
            selectPendingModel()
        }
    }

    fun extractWorkflowParameters(workflowJson: String) {
        uiState.update { it.copy(isLoadingParameters = true) }
        launchWithErrorHandling(
            tag = "Failed to extract workflow parameters",
            onError = { uiState.update { it.copy(isLoadingParameters = false) } },
        ) {
            val objectInfoJson = useCases.fetchObjectInfo()
            val params = useCases.extractParameters(workflowJson, objectInfoJson)
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

    private class PendingModel(val fileName: String, val family: DiffusionModelFamily?)

    companion object {
        private const val TAG = "GenerationResourceLoader"
    }
}
