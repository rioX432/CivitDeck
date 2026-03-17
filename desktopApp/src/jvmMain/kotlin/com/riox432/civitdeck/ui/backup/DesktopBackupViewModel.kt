package com.riox432.civitdeck.ui.backup

import com.riox432.civitdeck.domain.model.BackupCategory
import com.riox432.civitdeck.domain.model.RestoreStrategy
import com.riox432.civitdeck.domain.usecase.CreateBackupUseCase
import com.riox432.civitdeck.domain.usecase.ParseBackupUseCase
import com.riox432.civitdeck.domain.usecase.RestoreBackupUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val selectedCategories: Set<BackupCategory> = BackupCategory.entries.toSet(),
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportedJson: String? = null,
    val importJson: String? = null,
    val importCategories: Set<BackupCategory> = emptySet(),
    val restoreStrategy: RestoreStrategy = RestoreStrategy.MERGE,
    val showImportConfirmation: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class DesktopBackupViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val parseBackupUseCase: ParseBackupUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onToggleCategory(category: BackupCategory) {
        _uiState.update { state ->
            val updated = state.selectedCategories.toMutableSet()
            if (category in updated) updated.remove(category) else updated.add(category)
            state.copy(selectedCategories = updated)
        }
    }

    fun onSelectAll() {
        _uiState.update { it.copy(selectedCategories = BackupCategory.entries.toSet()) }
    }

    fun onDeselectAll() {
        _uiState.update { it.copy(selectedCategories = emptySet()) }
    }

    fun onExport() {
        val categories = _uiState.value.selectedCategories
        if (categories.isEmpty()) return
        _uiState.update { it.copy(isExporting = true, error = null) }
        scope.launch {
            try {
                val json = createBackupUseCase(categories)
                _uiState.update { it.copy(isExporting = false, exportedJson = json) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, error = "Export failed: ${e.message}")
                }
            }
        }
    }

    fun onExportHandled() {
        _uiState.update { it.copy(exportedJson = null) }
    }

    fun onImportFileLoaded(json: String) {
        scope.launch {
            try {
                val categories = parseBackupUseCase(json)
                _uiState.update {
                    it.copy(
                        importJson = json,
                        importCategories = categories,
                        selectedCategories = categories,
                        showImportConfirmation = true,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Invalid backup file: ${e.message}")
                }
            }
        }
    }

    fun onRestoreStrategyChanged(strategy: RestoreStrategy) {
        _uiState.update { it.copy(restoreStrategy = strategy) }
    }

    fun onConfirmImport() {
        val state = _uiState.value
        val json = state.importJson ?: return
        val categories = state.selectedCategories
        if (categories.isEmpty()) return
        _uiState.update { it.copy(isImporting = true, showImportConfirmation = false, error = null) }
        scope.launch {
            try {
                restoreBackupUseCase(json, state.restoreStrategy, categories)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importJson = null,
                        importCategories = emptySet(),
                        message = "Restore completed successfully",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isImporting = false, error = "Restore failed: ${e.message}")
                }
            }
        }
    }

    fun onDismissImportConfirmation() {
        _uiState.update {
            it.copy(showImportConfirmation = false, importJson = null, importCategories = emptySet())
        }
    }

    fun onMessageDismissed() {
        _uiState.update { it.copy(message = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
