package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BatchTagEditorViewModel(
    private val datasetId: Long,
    observeDatasetImagesUseCase: ObserveDatasetImagesUseCase,
    private val batchEditTagsUseCase: BatchEditTagsUseCase,
    private val getTagSuggestionsUseCase: GetTagSuggestionsUseCase,
) : ViewModel() {

    val images: StateFlow<List<DatasetImage>> =
        observeDatasetImagesUseCase(datasetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val tagInput = MutableStateFlow("")
    val tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val isAddMode = MutableStateFlow(true)

    fun toggleSelection(imageId: Long) {
        selectedImageIds.value = selectedImageIds.value.let { current ->
            if (imageId in current) current - imageId else current + imageId
        }
    }

    fun selectAll() {
        selectedImageIds.value = images.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedImageIds.value = emptySet()
    }

    fun setTagInput(text: String) {
        tagInput.value = text
        loadSuggestions(text)
    }

    fun toggleMode() {
        isAddMode.value = !isAddMode.value
    }

    private fun loadSuggestions(prefix: String) {
        viewModelScope.launch {
            suspendRunCatching { getTagSuggestionsUseCase(datasetId, prefix) }
                .onSuccess { tagSuggestions.value = it }
                .onFailure { e ->
                    Logger.w(TAG, "Load tag suggestions failed: ${e.message}")
                    tagSuggestions.value = emptyList()
                }
        }
    }

    fun applyTags(tags: List<String>) {
        val ids = selectedImageIds.value.toList()
        if (ids.isEmpty() || tags.isEmpty()) return
        viewModelScope.launch {
            suspendRunCatching {
                if (isAddMode.value) {
                    batchEditTagsUseCase(ids, addTags = tags, removeTags = emptyList())
                } else {
                    batchEditTagsUseCase(ids, addTags = emptyList(), removeTags = tags)
                }
            }.onFailure { e -> Logger.w(TAG, "Apply tags failed: ${e.message}") }
        }
    }
}

private const val TAG = "BatchTagEditorViewModel"
private const val STOP_TIMEOUT = 5_000L
