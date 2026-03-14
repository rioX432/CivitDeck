package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.LocalModelFile
import com.riox432.civitdeck.domain.model.ModelDirectory
import com.riox432.civitdeck.domain.model.ScanStatus
import com.riox432.civitdeck.domain.usecase.AddModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.ObserveLocalModelFilesUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelDirectoryUseCase
import com.riox432.civitdeck.domain.usecase.ScanModelDirectoriesUseCase
import com.riox432.civitdeck.domain.usecase.VerifyModelHashUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModelFileBrowserUiState(
    val directories: List<ModelDirectory> = emptyList(),
    val files: List<LocalModelFile> = emptyList(),
    val scanStatus: ScanStatus = ScanStatus.Idle,
    val scanProgress: String = "",
    val errorMessage: String? = null,
)

class ModelFileBrowserViewModel(
    observeModelDirectoriesUseCase: ObserveModelDirectoriesUseCase,
    observeLocalModelFilesUseCase: ObserveLocalModelFilesUseCase,
    private val addModelDirectoryUseCase: AddModelDirectoryUseCase,
    private val removeModelDirectoryUseCase: RemoveModelDirectoryUseCase,
    private val scanModelDirectoriesUseCase: ScanModelDirectoriesUseCase,
    private val verifyModelHashUseCase: VerifyModelHashUseCase,
) : ViewModel() {

    private val _scanState = MutableStateFlow(ScanStateHolder())

    val uiState: StateFlow<ModelFileBrowserUiState> = combine(
        observeModelDirectoriesUseCase(),
        observeLocalModelFilesUseCase(),
        _scanState,
    ) { directories, files, scanState ->
        ModelFileBrowserUiState(
            directories = directories,
            files = files,
            scanStatus = scanState.status,
            scanProgress = scanState.progress,
            errorMessage = scanState.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ModelFileBrowserUiState())

    fun onAddDirectory(path: String) {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addModelDirectoryUseCase(trimmed)
        }
    }

    fun onRemoveDirectory(id: Long) {
        viewModelScope.launch {
            removeModelDirectoryUseCase(id)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onScanAll() {
        if (_scanState.value.status == ScanStatus.Scanning || _scanState.value.status == ScanStatus.Verifying) return
        viewModelScope.launch {
            _scanState.value = ScanStateHolder(status = ScanStatus.Scanning)
            try {
                scanModelDirectoriesUseCase(onProgress = { current, total ->
                    _scanState.value = ScanStateHolder(
                        status = ScanStatus.Scanning,
                        progress = "Hashing file $current of $total...",
                    )
                })
                // Now verify hashes against CivitAI API
                _scanState.value = ScanStateHolder(status = ScanStatus.Verifying, progress = "Verifying hashes...")
                verifyAllHashes()
                _scanState.value = ScanStateHolder(status = ScanStatus.Completed)
            } catch (e: Exception) {
                Logger.e(TAG, "Scan failed: ${e.message}")
                _scanState.value = ScanStateHolder(
                    status = ScanStatus.Error,
                    error = e.message ?: "Scan failed",
                )
            }
        }
    }

    fun onDismissError() {
        _scanState.value = ScanStateHolder()
    }

    private suspend fun verifyAllHashes() {
        val files = uiState.value.files.filter { it.matchedModel == null }
        files.forEachIndexed { index, file ->
            _scanState.value = ScanStateHolder(
                status = ScanStatus.Verifying,
                progress = "Verifying ${index + 1} of ${files.size}...",
            )
            verifyModelHashUseCase(file.id, file.sha256Hash)
        }
    }

    companion object {
        private const val TAG = "ModelFileBrowserVM"
        private const val STOP_TIMEOUT = 5_000L
    }
}

private data class ScanStateHolder(
    val status: ScanStatus = ScanStatus.Idle,
    val progress: String = "",
    val error: String? = null,
)
