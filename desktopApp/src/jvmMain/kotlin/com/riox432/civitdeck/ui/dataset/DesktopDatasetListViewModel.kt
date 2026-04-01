package com.riox432.civitdeck.ui.dataset

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DesktopDatasetListViewModel(
    observeDatasetCollectionsUseCase: ObserveDatasetCollectionsUseCase,
    private val createDatasetCollectionUseCase: CreateDatasetCollectionUseCase,
    private val renameDatasetCollectionUseCase: RenameDatasetCollectionUseCase,
    private val deleteDatasetCollectionUseCase: DeleteDatasetCollectionUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    val datasets: StateFlow<List<DatasetCollection>> =
        observeDatasetCollectionsUseCase()
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun createDataset(name: String) {
        scope.launch {
            suspendRunCatching { createDatasetCollectionUseCase(name) }
                .onFailure { e -> Logger.w(TAG, "Create dataset failed: ${e.message}") }
        }
    }

    fun renameDataset(id: Long, name: String) {
        scope.launch {
            suspendRunCatching { renameDatasetCollectionUseCase(id, name) }
                .onFailure { e -> Logger.w(TAG, "Rename dataset failed: ${e.message}") }
        }
    }

    fun deleteDataset(id: Long) {
        scope.launch {
            suspendRunCatching { deleteDatasetCollectionUseCase(id) }
                .onFailure { e -> Logger.w(TAG, "Delete dataset failed: ${e.message}") }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}

private const val TAG = "DesktopDatasetListViewModel"
private const val STOP_TIMEOUT = 5_000L
