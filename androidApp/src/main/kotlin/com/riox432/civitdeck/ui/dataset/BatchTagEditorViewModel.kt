package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.usecase.BatchEditTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetTagSuggestionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDatasetImagesUseCase
import kotlinx.coroutines.CancellationException
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
            try {
                tagSuggestions.value = getTagSuggestionsUseCase(datasetId, prefix)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                tagSuggestions.value = emptyList()
            }
        }
    }

    fun applyTags(tags: List<String>) {
        val ids = selectedImageIds.value.toList()
        if (ids.isEmpty() || tags.isEmpty()) return
        viewModelScope.launch {
            try {
                if (isAddMode.value) {
                    batchEditTagsUseCase(ids, addTags = tags, removeTags = emptyList())
                } else {
                    batchEditTagsUseCase(ids, addTags = emptyList(), removeTags = tags)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Tag edit failure is non-critical
            }
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
