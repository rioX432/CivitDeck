package com.riox432.civitdeck.ui.dataset

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.usecase.CreateDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteDatasetCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.RenameDatasetCollectionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
            try {
                createDatasetCollectionUseCase(name)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Dataset creation failure is non-critical
            }
        }
    }

    fun renameDataset(id: Long, name: String) {
        scope.launch {
            try {
                renameDatasetCollectionUseCase(id, name)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Rename failure is non-critical
            }
        }
    }

    fun deleteDataset(id: Long) {
        scope.launch {
            try {
                deleteDatasetCollectionUseCase(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Delete failure is non-critical
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}

private const val STOP_TIMEOUT = 5_000L
