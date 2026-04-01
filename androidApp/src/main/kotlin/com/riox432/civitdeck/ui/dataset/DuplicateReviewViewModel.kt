package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DuplicateGroup
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DuplicateReviewViewModel(
    private val datasetId: Long,
    detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val markImageExcludedUseCase: MarkImageExcludedUseCase,
) : ViewModel() {

    val duplicateGroups: StateFlow<List<DuplicateGroup>> =
        detectDuplicatesUseCase(datasetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun keepImage(imageId: Long) {
        viewModelScope.launch {
            suspendRunCatching { markImageExcludedUseCase(imageId, false) }
                .onFailure { e -> Logger.w(TAG, "Keep image failed: ${e.message}") }
        }
    }

    fun removeImage(imageId: Long) {
        viewModelScope.launch {
            suspendRunCatching { markImageExcludedUseCase(imageId, true) }
                .onFailure { e -> Logger.w(TAG, "Remove image failed: ${e.message}") }
        }
    }
}

private const val TAG = "DuplicateReviewViewModel"
