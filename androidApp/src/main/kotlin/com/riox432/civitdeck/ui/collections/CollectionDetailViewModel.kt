package com.riox432.civitdeck.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CollectionSortOrder
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.usecase.BulkMoveModelsUseCase
import com.riox432.civitdeck.domain.usecase.BulkRemoveModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val collectionId: Long,
    observeCollectionModelsUseCase: ObserveCollectionModelsUseCase,
    observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val bulkRemoveModelsUseCase: BulkRemoveModelsUseCase,
    private val bulkMoveModelsUseCase: BulkMoveModelsUseCase,
) : ViewModel() {

    val sortOrder = MutableStateFlow(CollectionSortOrder.DateAdded)
    val typeFilter = MutableStateFlow<ModelType?>(null)
    val selectedModelIds = MutableStateFlow<Set<Long>>(emptySet())
    val isSelectionMode = MutableStateFlow(false)

    private val rawModels: StateFlow<List<FavoriteModelSummary>> =
        observeCollectionModelsUseCase(collectionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val collections: StateFlow<List<ModelCollection>> =
        observeCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val displayModels: StateFlow<List<FavoriteModelSummary>> =
        combine(rawModels, sortOrder, typeFilter) { models, sort, filter ->
            val filtered = if (filter != null) models.filter { it.type == filter } else models
            sortModels(filtered, sort)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun toggleSelection(modelId: Long) {
        selectedModelIds.value = selectedModelIds.value.let { current ->
            if (modelId in current) current - modelId else current + modelId
        }
        if (selectedModelIds.value.isEmpty()) isSelectionMode.value = false
    }

    fun selectAll() {
        selectedModelIds.value = displayModels.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedModelIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun enterSelectionMode(modelId: Long) {
        isSelectionMode.value = true
        selectedModelIds.value = setOf(modelId)
    }

    fun removeSelected() {
        val ids = selectedModelIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                bulkRemoveModelsUseCase(collectionId, ids)
                clearSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Removal failure is non-critical
            }
        }
    }

    fun moveSelectedTo(targetId: Long) {
        val ids = selectedModelIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                bulkMoveModelsUseCase(collectionId, targetId, ids)
                clearSelection()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Move failure is non-critical
            }
        }
    }

    private fun sortModels(
        models: List<FavoriteModelSummary>,
        sort: CollectionSortOrder,
    ): List<FavoriteModelSummary> = when (sort) {
        CollectionSortOrder.DateAdded -> models.sortedByDescending { it.favoritedAt }
        CollectionSortOrder.Rating -> models.sortedByDescending { it.rating }
        CollectionSortOrder.Type -> models.sortedBy { it.type.name }
        CollectionSortOrder.Name -> models.sortedBy { it.name.lowercase() }
    }
}

private const val STOP_TIMEOUT = 5_000L
