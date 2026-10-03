package com.riox432.civitdeck.feature.comfyui.presentation

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
internal class GenerationResourceLoader(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val useCases: GenerationResourceUseCases,
) {

    // A checkpoint requested before the server list arrives; consumed by loadCheckpoints().
    private var pendingCheckpoint: String? = null

    fun loadCheckpoints() {
        uiState.update { it.copy(isLoadingCheckpoints = true) }
        launchWithErrorHandling(
            tag = "Failed to load checkpoints",
            onError = { e -> uiState.update { it.copy(isLoadingCheckpoints = false, error = e.message) } },
        ) {
            val list = useCases.fetchCheckpoints()
            val requested = pendingCheckpoint
            pendingCheckpoint = null
            uiState.update {
                it.copy(
                    checkpoints = list,
                    selectedCheckpoint = requested?.let { name -> findCheckpoint(list, name) }
                        ?: list.firstOrNull()
                        ?: "",
                    isLoadingCheckpoints = false,
                )
            }
        }
    }

    /**
     * Selects the server checkpoint matching [requested]. Before the list has loaded, the
     * request is kept and applied when it arrives. A request with no match selects the
     * first entry, as a fresh load does.
     */
    fun requestCheckpoint(requested: String) {
        val loaded = uiState.value.checkpoints
        if (loaded.isEmpty()) {
            pendingCheckpoint = requested
            return
        }
        pendingCheckpoint = null
        val selected = findCheckpoint(loaded, requested) ?: loaded.first()
        uiState.update { it.copy(selectedCheckpoint = selected) }
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

    companion object {
        private const val TAG = "GenerationResourceLoader"
    }
}

/**
 * ComfyUI lists checkpoints with their subfolder (`SDXL/foo.safetensors`), while CivitAI
 * metadata and templates carry a bare file name, so a full-path match is tried first and
 * then the file name alone, ignoring case.
 */
internal fun findCheckpoint(available: List<String>, requested: String): String? {
    available.firstOrNull { it.equals(requested, ignoreCase = true) }?.let { return it }
    val requestedName = checkpointFileName(requested)
    return available.firstOrNull { checkpointFileName(it).equals(requestedName, ignoreCase = true) }
}

private fun checkpointFileName(path: String): String =
    path.substringAfterLast('/').substringAfterLast('\\')
