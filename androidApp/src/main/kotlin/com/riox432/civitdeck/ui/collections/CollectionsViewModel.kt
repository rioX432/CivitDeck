package com.riox432.civitdeck.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.RenameCollectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionsViewModel(
    observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
    private val renameCollectionUseCase: RenameCollectionUseCase,
    private val deleteCollectionUseCase: DeleteCollectionUseCase,
) : ViewModel() {

    val collections: StateFlow<List<ModelCollection>> =
        observeCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun createCollection(name: String) {
        viewModelScope.launch {
            try {
                createCollectionUseCase(name)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Collection creation failure is non-critical
            }
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch {
            try {
                renameCollectionUseCase(id, name)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Rename failure is non-critical
            }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            try {
                deleteCollectionUseCase(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Delete failure is non-critical
            }
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
