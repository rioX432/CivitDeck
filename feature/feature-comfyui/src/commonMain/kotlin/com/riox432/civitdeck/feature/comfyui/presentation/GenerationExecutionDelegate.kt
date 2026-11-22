package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.PreparedReferenceGeneration
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
    /**
     * One press of Generate. Kept per press so a submission still waiting for its id after Stop
     * cancels its own prompt on its own server, even if Generate was pressed again meanwhile.
     */
    private class Submission {
        var job: ComfyUIJobHandle? = null
        var stopped: Boolean = false
    }

    private var progressJob: Job? = null
    private var submission: Submission? = null
    private var generationStartTimeMs: Long = 0L
    private var notificationsEnabled: Boolean = true

    init {
        useCases.observeGenNotifEnabled()
            .onEach { notificationsEnabled = it }
            .launchIn(scope)
    }

    /** Submits to the connection active now, read once; later activations do not move the job. */
    fun onGenerate(params: ComfyUIGenerationParams) = start(
        resolveTarget = {
            val active = useCases.repository.getActiveConnection()
                ?: throw DomainException.ConnectionException("No active ComfyUI connection")
            ComfyUIJobTarget.from(active)
        },
        submit = { useCases.submitGeneration(params, it) },
    )

    /** Submits to [target] whichever connection is active, or fails if it cannot be reached as captured. */
    fun onGenerate(params: ComfyUIGenerationParams, target: ComfyUIJobTarget) =
        start(resolveTarget = { target }, submit = { useCases.submitGeneration(params, it) })

    /** Submits the reviewed graph unchanged to the target it was prepared for. */
    fun onGenerate(prepared: PreparedReferenceGeneration) =
        start(resolveTarget = { prepared.target }, submit = { useCases.submitGeneration(prepared) })

    private fun start(
        resolveTarget: suspend () -> ComfyUIJobTarget,
        submit: suspend (ComfyUIJobTarget) -> ComfyUIJobHandle,
    ) {
        progressJob?.cancel()
        val current = Submission().also { submission = it }
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
                // A newer Generate owns the screen, so an older failure stays silent. After Stop,
                // only a failed cancel matters (the prompt may still run); a failed submission
                // queued nothing.
                val report = submission === current && (!current.stopped || current.job != null)
                if (report) {
                    uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
                }
            },
        ) {
            val target = resolveTarget()
            // Stopped while the target was being read: nothing was queued yet.
            if (current.stopped) return@launchWithErrorHandling
            val job = submit(target)
            current.job = job
            when {
                current.stopped -> useCases.cancelJob(job)
                // A newer Generate owns the screen; like a superseded running job, this one is
                // left to finish on the server untracked.
                submission !== current -> Unit
                else -> {
                    uiState.update { it.copy(generationStatus = GenerationStatus.Running) }
                    progressJob = trackProgress(job)
                }
            }
        }
    }

    fun onInterrupt() {
        progressJob?.cancel()
        useCases.backgroundMonitorStarter.stopMonitoring()
        val current = submission
        current?.stopped = true
        val job = current?.job
        if (current == null || job == null) {
            // Still waiting for /prompt: the submission cancels the prompt once its id arrives.
            resetToIdle()
            return
        }
        // A Generate pressed while this cancel is in flight owns the state, so leave it alone then.
        launchWithErrorHandling(
            tag = "Interrupt failed",
            onError = { e -> if (submission === current) uiState.update { it.copy(error = e.message) } },
        ) {
            useCases.cancelJob(job)
            if (submission === current) resetToIdle()
        }
    }

    private fun resetToIdle() {
        uiState.update {
            it.copy(
                generationStatus = GenerationStatus.Idle,
                currentStep = 0,
                totalSteps = 0,
                previewImageBytes = null,
            )
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

    /** Polling runs in this job too, so Stop and a new Generate end it. */
    private fun trackProgress(job: ComfyUIJobHandle): Job = launchWithErrorHandling(
        tag = "Generation progress failed",
        onError = { e ->
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        },
    ) {
        useCases.backgroundMonitorStarter.startMonitoring(job)
        observeWebSocketProgress(job)
    }

    private suspend fun observeWebSocketProgress(job: ComfyUIJobHandle) {
        useCases.observeProgress(job)
            .catch { pollForResult(job) }
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
        fetchFinalResult(job)
    }

    private suspend fun fetchFinalResult(job: ComfyUIJobHandle) {
        val promptId = job.promptId
        useCases.backgroundMonitorStarter.stopMonitoring()
        val onError = { e: Exception ->
            notifyErrorIfNeeded(promptId, e.message ?: "Unknown error")
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        runCatchingLogged("Failed to fetch final result", onError) {
            val result = useCases.pollResult(job)
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

    private suspend fun pollForResult(job: ComfyUIJobHandle) {
        val promptId = job.promptId
        val onError = { e: Exception ->
            notifyErrorIfNeeded(promptId, e.message ?: "Unknown error")
            uiState.update { it.copy(generationStatus = GenerationStatus.Error, error = e.message) }
        }
        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            var done = false
            val success = runCatchingLogged("Poll for result failed", onError) {
                val result = useCases.pollResult(job)
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
    ): Job {
        return scope.launch {
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
