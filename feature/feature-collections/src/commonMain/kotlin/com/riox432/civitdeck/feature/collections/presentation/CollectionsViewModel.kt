package com.riox432.civitdeck.feature.collections.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.feature.collections.domain.usecase.DeleteCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RenameCollectionUseCase
import com.riox432.civitdeck.util.Logger
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
            suspendRunCatching { createCollectionUseCase(name) }
                .onFailure { e -> Logger.w(TAG, "Create collection failed: ${e.message}") }
        }
    }

    fun renameCollection(id: Long, name: String) {
        viewModelScope.launch {
            suspendRunCatching { renameCollectionUseCase(id, name) }
                .onFailure { e -> Logger.w(TAG, "Rename collection failed: ${e.message}") }
        }
    }

    fun deleteCollection(id: Long) {
        viewModelScope.launch {
            suspendRunCatching { deleteCollectionUseCase(id) }
                .onFailure { e -> Logger.w(TAG, "Delete collection failed: ${e.message}") }
        }
    }
}

private const val TAG = "CollectionsViewModel"
private const val STOP_TIMEOUT = 5_000L
