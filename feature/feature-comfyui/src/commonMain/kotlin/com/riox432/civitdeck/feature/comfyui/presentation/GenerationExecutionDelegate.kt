package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles generation submission, progress tracking (WebSocket + polling), interruption, and
 * image saving. Extracted from [ComfyUIGenerationViewModel] to reduce function count.
 */
internal class GenerationExecutionDelegate(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<GenerationUiState>,
    private val useCases: GenerationExecutionUseCases,
) {
    private var progressJob: Job? = null
    private var generationStartTimeMs: Long = 0L
    private var notificationsEnabled: Boolean = true

    init {
        useCases.observeGenNotifEnabled()
            .onEach { notificationsEnabled = it }
            .launchIn(scope)
    }

    fun onGenerate(params: ComfyUIGenerationParams) {
        progressJob?.cancel()
        generationStartTimeMs = currentTimeMs()
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
            val promptId = useCases.submitGeneration(params)
            uiState.update { it.copy(generationStatus = GenerationStatus.Running) }
            val connection = useCases.repository.getActiveConnection()
            if (connection != null) {
                useCases.backgroundMonitorStarter.startMonitoring(
                    promptId,
                    connection.baseUrl,
                    connection.wsScheme,
                )
                startWebSocketProgress(promptId, connection)
            } else {
                pollForResult(promptId)
            }
        }
    }

    fun onInterrupt() {
        progressJob?.cancel()
        useCases.backgroundMonitorStarter.stopMonitoring()
        launchWithErrorHandling(
            tag = "Interrupt failed",
            onError = { e -> uiState.update { it.copy(error = e.message) } },
        ) {
            useCases.interruptGeneration()
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
            val success = useCases.saveImage(imageUrl)
            uiState.update { it.copy(imageSaveSuccess = success) }
        }
    }

    fun onDismissSaveResult() {
        uiState.update { it.copy(imageSaveSuccess = null) }
    }

    private fun startWebSocketProgress(promptId: String, connection: ComfyUIConnection) {
        progressJob = scope.launch {
            useCases.observeProgress(promptId, connection.baseUrl, connection.wsScheme)
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
        useCases.backgroundMonitorStarter.stopMonitoring()
        val onError = { e: Exception ->
            notifyErrorIfNeeded(promptId, e.message ?: "Unknown error")
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        runCatchingLogged("Failed to fetch final result", onError) {
            val result = useCases.pollResult(promptId)
            if (result.status == GenerationStatus.Completed) {
                notifyCompleteIfNeeded(promptId, result.imageUrls.size)
                uiState.update {
                    it.copy(generationStatus = GenerationStatus.Completed, result = result)
                }
            } else {
                notifyErrorIfNeeded(promptId, result.error ?: "Unknown error")
                uiState.update {
                    it.copy(generationStatus = GenerationStatus.Error, error = result.error)
                }
            }
        }
    }

    private suspend fun pollForResult(promptId: String) {
        val onError = { e: Exception ->
            notifyErrorIfNeeded(promptId, e.message ?: "Unknown error")
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            var done = false
            val success = runCatchingLogged("Poll for result failed", onError) {
                val result = useCases.pollResult(promptId)
                when (result.status) {
                    GenerationStatus.Completed -> {
                        notifyCompleteIfNeeded(promptId, result.imageUrls.size)
                        uiState.update {
                            it.copy(generationStatus = GenerationStatus.Completed, result = result)
                        }
                        done = true
                    }
                    GenerationStatus.Error -> {
                        notifyErrorIfNeeded(promptId, result.error ?: "Unknown error")
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
        notifyErrorIfNeeded(promptId, "Generation timed out")
        uiState.update {
            it.copy(generationStatus = GenerationStatus.Error, error = "Generation timed out")
        }
    }

    // region Notification helpers

    private fun notifyCompleteIfNeeded(promptId: String, imageCount: Int) {
        if (!notificationsEnabled || useCases.lifecycleTracker.isInForeground) return
        val elapsed = currentTimeMs() - generationStartTimeMs
        useCases.notificationService.notifyGenerationComplete(promptId, imageCount, elapsed)
    }

    private fun notifyErrorIfNeeded(promptId: String, errorMessage: String) {
        if (!notificationsEnabled || useCases.lifecycleTracker.isInForeground) return
        useCases.notificationService.notifyGenerationError(promptId, errorMessage)
    }

    private fun currentTimeMs(): Long = currentTimeMillis()

    // endregion

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
