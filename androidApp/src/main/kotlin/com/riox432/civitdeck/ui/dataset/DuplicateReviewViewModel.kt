package com.riox432.civitdeck.ui.dataset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DuplicateGroup
import com.riox432.civitdeck.domain.usecase.DetectDuplicatesUseCase
import com.riox432.civitdeck.domain.usecase.MarkImageExcludedUseCase
import kotlinx.coroutines.CancellationException
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
            try {
                markImageExcludedUseCase(imageId, false)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Keep failure is non-critical
            }
        }
    }

    fun removeImage(imageId: Long) {
        viewModelScope.launch {
            try {
                markImageExcludedUseCase(imageId, true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Remove failure is non-critical
            }
        }
    }
}
