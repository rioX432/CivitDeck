package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.data.image.SaveGeneratedImageUseCase
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.InterruptComfyUIGenerationUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveGenerationProgressUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.PollComfyUIResultUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SubmitComfyUIGenerationUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles generation submission, progress tracking (WebSocket + polling), interruption, and
 * image saving. Extracted from [ComfyUIGenerationViewModel] to reduce function count.
 */
internal class GenerationExecutionDelegate(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val submitGeneration: SubmitComfyUIGenerationUseCase,
    private val pollResult: PollComfyUIResultUseCase,
    private val observeProgress: ObserveGenerationProgressUseCase,
    private val interruptGeneration: InterruptComfyUIGenerationUseCase,
    private val saveImage: SaveGeneratedImageUseCase,
    private val repository: ComfyUIConnectionRepository,
) {
    private var progressJob: Job? = null

    fun onGenerate(params: ComfyUIGenerationParams) {
        progressJob?.cancel()
        uiState.update {
            it.copy(
                generationStatus = GenerationStatus.Submitting,
                error = null,
                result = null,
                currentStep = 0,
                totalSteps = 0,
                previewImageBytes = null,
                currentNodeName = "",
            )
        }
        launchWithErrorHandling(
            tag = "Generation submission failed",
            onError = { e ->
                uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
            },
        ) {
            val promptId = submitGeneration(params)
            uiState.update { it.copy(generationStatus = GenerationStatus.Running) }
            val connection = repository.getActiveConnection()
            if (connection != null) {
                startWebSocketProgress(promptId, connection)
            } else {
                pollForResult(promptId)
            }
        }
    }

    fun onInterrupt() {
        progressJob?.cancel()
        launchWithErrorHandling(
            tag = "Interrupt failed",
            onError = { e -> uiState.update { it.copy(error = e.message) } },
        ) {
            interruptGeneration()
            uiState.update {
                it.copy(
                    generationStatus = GenerationStatus.Idle,
                    currentStep = 0,
                    totalSteps = 0,
                    previewImageBytes = null,
                )
            }
        }
    }

    fun onSaveImage(imageUrl: String) {
        scope.launch {
            val success = saveImage(imageUrl)
            uiState.update { it.copy(imageSaveSuccess = success) }
        }
    }

    fun onDismissSaveResult() {
        uiState.update { it.copy(imageSaveSuccess = null) }
    }

    private fun startWebSocketProgress(promptId: String, connection: ComfyUIConnection) {
        progressJob = scope.launch {
            observeProgress(promptId, connection.hostname, connection.port)
                .catch { pollForResult(promptId) }
                .collect { progress ->
                    uiState.update { state ->
                        state.copy(
                            currentStep = if (progress.currentStep > 0) {
                                progress.currentStep
                            } else {
                                state.currentStep
                            },
                            totalSteps = if (progress.totalSteps > 0) {
                                progress.totalSteps
                            } else {
                                state.totalSteps
                            },
                            previewImageBytes = progress.previewImageBytes
                                ?: state.previewImageBytes,
                            currentNodeName = progress.currentNode.ifEmpty {
                                state.currentNodeName
                            },
                        )
                    }
                }
            // WebSocket flow completed: fetch final result
            fetchFinalResult(promptId)
        }
    }

    private suspend fun fetchFinalResult(promptId: String) {
        val onError = { e: Exception ->
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        runCatchingLogged("Failed to fetch final result", onError) {
            val result = pollResult(promptId)
            uiState.update {
                if (result.status == GenerationStatus.Completed) {
                    it.copy(generationStatus = GenerationStatus.Completed, result = result)
                } else {
                    it.copy(generationStatus = GenerationStatus.Error, error = result.error)
                }
            }
        }
    }

    private suspend fun pollForResult(promptId: String) {
        val onError = { e: Exception ->
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            var done = false
            val success = runCatchingLogged("Poll for result failed", onError) {
                val result = pollResult(promptId)
                when (result.status) {
                    GenerationStatus.Completed -> {
                        uiState.update {
                            it.copy(generationStatus = GenerationStatus.Completed, result = result)
                        }
                        done = true
                    }
                    GenerationStatus.Error -> {
                        uiState.update {
                            it.copy(generationStatus = GenerationStatus.Error, error = result.error)
                        }
                        done = true
                    }
                    else -> attempts++
                }
            }
            if (!success || done) return
        }
        uiState.update {
            it.copy(generationStatus = GenerationStatus.Error, error = "Generation timed out")
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

    private suspend inline fun runCatchingLogged(
        tag: String,
        onError: (Exception) -> Unit,
        block: () -> Unit,
    ): Boolean {
        return try {
            block()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "$tag: ${e.message}")
            onError(e)
            false
        }
    }

    companion object {
        private const val TAG = "GenerationExecution"
        private const val POLL_INTERVAL_MS = 3000L
        private const val MAX_POLL_ATTEMPTS = 120
    }
}
