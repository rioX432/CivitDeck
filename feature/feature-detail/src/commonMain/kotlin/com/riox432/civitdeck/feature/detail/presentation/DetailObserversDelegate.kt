package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Collects the reactive preference/state flows (favorite, NSFW filter, power-user mode, note,
 * personal tags, downloads) and reflects them into [ModelDetailUiState].
 * Extracted from [ModelDetailViewModel] to reduce its function count.
 */
internal class DetailObserversDelegate(
    private val modelId: Long,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<ModelDetailUiState>,
    private val observeIsFavorite: ObserveIsFavoriteUseCase,
    private val observeNsfwFilter: ObserveNsfwFilterUseCase,
    private val observePowerUserMode: ObservePowerUserModeUseCase,
    private val observeModelNote: ObserveModelNoteUseCase,
    private val observePersonalTags: ObservePersonalTagsUseCase,
    private val observeModelDownloads: ObserveModelDownloadsUseCase,
) {

    fun start() {
        observeFavorite()
        observeNsfwFilterLevel()
        observePowerUser()
        observeNote()
        observeTags()
        observeDownloads()
    }

    private fun observeFavorite() {
        scope.launch {
            observeIsFavorite(modelId).collect { isFavorite ->
                uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    private fun observeNsfwFilterLevel() {
        scope.launch {
            observeNsfwFilter().collect { level ->
                uiState.update { it.copy(nsfwFilterLevel = level) }
            }
        }
    }

    private fun observePowerUser() {
        scope.launch {
            observePowerUserMode().collect { enabled ->
                uiState.update { it.copy(powerUserMode = enabled) }
            }
        }
    }

    private fun observeNote() {
        scope.launch {
            observeModelNote(modelId).collect { note ->
                uiState.update { it.copy(note = note) }
            }
        }
    }

    private fun observeTags() {
        scope.launch {
            observePersonalTags(modelId).collect { tags ->
                uiState.update { it.copy(personalTags = tags) }
            }
        }
    }

    private fun observeDownloads() {
        scope.launch {
            observeModelDownloads(modelId).collect { downloads ->
                uiState.update { it.copy(downloads = downloads) }
            }
        }
    }
}
