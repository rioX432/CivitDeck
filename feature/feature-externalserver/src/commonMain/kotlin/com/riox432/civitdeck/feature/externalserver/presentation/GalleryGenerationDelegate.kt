package com.riox432.civitdeck.feature.externalserver.presentation

import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJobStatus
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ExecuteGenerationUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetDependentChoicesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationOptionsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationStatusUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "GalleryGenerationDelegate"
private const val POLL_INTERVAL_MS = 2000L
private const val MAX_POLL_TIMEOUT_MS = 600_000L

internal class GalleryGenerationDelegate(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ExternalServerGalleryUiState>,
    private val getGenerationOptions: GetGenerationOptionsUseCase,
    private val getDependentChoices: GetDependentChoicesUseCase,
    private val executeGeneration: ExecuteGenerationUseCase,
    private val getGenerationStatus: GetGenerationStatusUseCase,
    private val onGenerationCompleted: () -> Unit,
) {

    private var pollJob: Job? = null

    fun onShowGenerationSheet() {
        uiState.update { it.copy(showGenerationSheet = true, generationError = null) }
        if (uiState.value.generationOptions.isEmpty()) {
            loadGenerationOptions()
        }
    }

    fun onDismissGenerationSheet() {
        uiState.update { it.copy(showGenerationSheet = false) }
    }

    fun onGenerationParamChanged(key: String, value: String) {
        val newParams = uiState.value.generationParams.toMutableMap()
        newParams[key] = value
        uiState.update { it.copy(generationParams = newParams) }

        val dependents = uiState.value.generationOptions.filter { it.dependsOn == key }
        dependents.forEach { option ->
            option.choicesEndpoint?.let { endpoint ->
                loadDependentChoices(option.key, endpoint.replace("{$key}", value))
            }
        }
    }

    fun onSubmitGeneration() {
        scope.launch {
            uiState.update { it.copy(isSubmittingGeneration = true, generationError = null) }
            suspendRunCatching {
                executeGeneration(uiState.value.generationParams)
            }.onSuccess { job ->
                uiState.update {
                    it.copy(
                        activeJob = job,
                        isSubmittingGeneration = false,
                        showGenerationSheet = false,
                    )
                }
                startPollingJobStatus(job.jobId)
            }.onFailure { e ->
                uiState.update {
                    it.copy(
                        isSubmittingGeneration = false,
                        generationError = e.message ?: "Generation failed",
                    )
                }
            }
        }
    }

    fun onDismissJobStatus() {
        pollJob?.cancel()
        uiState.update { it.copy(activeJob = null) }
    }

    fun cancelPolling() {
        pollJob?.cancel()
    }

    private fun loadGenerationOptions() {
        scope.launch {
            uiState.update { it.copy(isLoadingOptions = true) }
            suspendRunCatching { getGenerationOptions() }
                .onSuccess { options ->
                    val defaults = options.mapNotNull { option ->
                        option.defaultValue?.let { option.key to it }
                    }.toMap()
                    uiState.update {
                        it.copy(
                            generationOptions = options,
                            generationParams = defaults,
                            isLoadingOptions = false,
                        )
                    }
                }.onFailure { e ->
                    uiState.update {
                        it.copy(
                            isLoadingOptions = false,
                            generationError = e.message ?: "Failed to load options",
                        )
                    }
                }
        }
    }

    private fun loadDependentChoices(key: String, endpoint: String) {
        scope.launch {
            suspendRunCatching { getDependentChoices(endpoint) }
                .onSuccess { choices ->
                    uiState.update {
                        it.copy(
                            dependentChoices = it.dependentChoices + (key to choices),
                        )
                    }
                }
                .onFailure { e ->
                    Logger.w(TAG, "Load dependent choices failed for '$key': ${e.message}")
                }
        }
    }

    @Suppress("MagicNumber")
    private fun startPollingJobStatus(jobId: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            val result = withTimeoutOrNull(MAX_POLL_TIMEOUT_MS) {
                while (true) {
                    delay(POLL_INTERVAL_MS)
                    suspendRunCatching { getGenerationStatus(jobId) }
                        .onSuccess { job ->
                            uiState.update { it.copy(activeJob = job) }
                            if (job.status == GenerationJobStatus.COMPLETED ||
                                job.status == GenerationJobStatus.ERROR
                            ) {
                                if (job.status == GenerationJobStatus.COMPLETED) {
                                    onGenerationCompleted()
                                }
                                return@withTimeoutOrNull
                            }
                        }
                        .onFailure { return@withTimeoutOrNull }
                }
            }
            if (result == null) {
                uiState.update {
                    it.copy(
                        activeJob = null,
                        generationError = "Generation timed out after 10 minutes",
                    )
                }
            }
        }
    }
}
