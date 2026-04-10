package com.riox432.civitdeck.feature.collections.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DatasetListViewModel(
    observeDatasetCollectionsUseCase: ObserveDatasetCollectionsUseCase,
    private val createDatasetCollectionUseCase: CreateDatasetCollectionUseCase,
    private val renameDatasetCollectionUseCase: RenameDatasetCollectionUseCase,
    private val deleteDatasetCollectionUseCase: DeleteDatasetCollectionUseCase,
) : ViewModel() {

    val datasets: StateFlow<List<DatasetCollection>> =
        observeDatasetCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createDataset(name: String) {
        viewModelScope.launch {
            suspendRunCatching { createDatasetCollectionUseCase(name) }
                .onFailure { e -> Logger.w(TAG, "Create dataset failed: ${e.message}") }
        }
    }

    fun renameDataset(id: Long, name: String) {
        viewModelScope.launch {
            suspendRunCatching { renameDatasetCollectionUseCase(id, name) }
                .onFailure { e -> Logger.w(TAG, "Rename dataset failed: ${e.message}") }
        }
    }

    fun deleteDataset(id: Long) {
        viewModelScope.launch {
            suspendRunCatching { deleteDatasetCollectionUseCase(id) }
                .onFailure { e -> Logger.w(TAG, "Delete dataset failed: ${e.message}") }
        }
    }
}

private const val TAG = "DatasetListViewModel"
private const val STOP_TIMEOUT = 5_000L
