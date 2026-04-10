package com.riox432.civitdeck.feature.gallery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.usecase.AddShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ObserveShareHashtagsUseCase
import com.riox432.civitdeck.domain.usecase.RemoveShareHashtagUseCase
import com.riox432.civitdeck.domain.usecase.ToggleShareHashtagUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT = 5_000L

class ShareViewModel(
    observeShareHashtags: ObserveShareHashtagsUseCase,
    private val addShareHashtag: AddShareHashtagUseCase,
    private val removeShareHashtag: RemoveShareHashtagUseCase,
    private val toggleShareHashtag: ToggleShareHashtagUseCase,
) : ViewModel() {

    val hashtags: StateFlow<List<ShareHashtag>> =
        observeShareHashtags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    fun onToggle(tag: String, isEnabled: Boolean) {
        viewModelScope.launch { toggleShareHashtag(tag, isEnabled) }
    }

    fun onAdd(tag: String) {
        viewModelScope.launch { addShareHashtag(tag) }
    }

    fun onRemove(tag: String) {
        viewModelScope.launch { removeShareHashtag(tag) }
    }
}
