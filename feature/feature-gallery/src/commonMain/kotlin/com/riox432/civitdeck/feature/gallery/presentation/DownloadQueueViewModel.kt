package com.riox432.civitdeck.feature.gallery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.download.DownloadScheduler
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ClearCompletedDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.PauseDownloadUseCase
import com.riox432.civitdeck.domain.usecase.ResumeDownloadUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class DownloadQueueUiState(
    val activeDownloads: List<ModelDownload> = emptyList(),
    val completedDownloads: List<ModelDownload> = emptyList(),
    val failedDownloads: List<ModelDownload> = emptyList(),
    val isLoading: Boolean = true,
    val totalStorageBytes: Long = 0,
)

class DownloadQueueViewModel(
    observeDownloadsUseCase: ObserveDownloadsUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val clearCompletedDownloadsUseCase: ClearCompletedDownloadsUseCase,
    private val downloadScheduler: DownloadScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadQueueUiState())
    val uiState: StateFlow<DownloadQueueUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDownloadsUseCase().collect { downloads ->
                val active = downloads.filter {
                    it.status in listOf(
                        DownloadStatus.Pending,
                        DownloadStatus.Downloading,
                        DownloadStatus.Paused,
                    )
                }
                val completed = downloads.filter { it.status == DownloadStatus.Completed }
                val failed = downloads.filter {
                    it.status in listOf(DownloadStatus.Failed, DownloadStatus.Cancelled)
                }
                val totalBytes = completed.sumOf { it.fileSizeBytes }
                _uiState.update {
                    it.copy(
                        activeDownloads = active,
                        completedDownloads = completed,
                        failedDownloads = failed,
                        isLoading = false,
                        totalStorageBytes = totalBytes,
                    )
                }
            }
        }
    }

    fun pauseDownload(downloadId: Long) {
        viewModelScope.launch {
            safeCall("Pause") {
                downloadScheduler.cancel(downloadId)
                pauseDownloadUseCase(downloadId)
            }
        }
    }

    fun resumeDownload(downloadId: Long) {
        viewModelScope.launch {
            safeCall("Resume") {
                resumeDownloadUseCase(downloadId)
                downloadScheduler.enqueue(downloadId)
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            safeCall("Cancel") {
                downloadScheduler.cancel(downloadId)
                cancelDownloadUseCase(downloadId)
            }
        }
    }

    fun retryDownload(downloadId: Long) {
        viewModelScope.launch {
            safeCall("Retry") {
                resumeDownloadUseCase(downloadId)
                downloadScheduler.enqueue(downloadId)
            }
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            safeCall("Delete") { deleteDownloadUseCase(downloadId) }
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            safeCall("ClearCompleted") { clearCompletedDownloadsUseCase() }
        }
    }

    private suspend fun safeCall(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "$tag failed: ${e.message}")
        }
    }
}

private const val TAG = "DownloadQueueVM"
