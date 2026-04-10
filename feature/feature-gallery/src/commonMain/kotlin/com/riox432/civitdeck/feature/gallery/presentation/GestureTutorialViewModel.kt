package com.riox432.civitdeck.feature.gallery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.usecase.ObserveSeenTutorialVersionUseCase
import com.riox432.civitdeck.domain.usecase.SetSeenTutorialVersionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GestureTutorialViewModel(
    observeSeenTutorialVersion: ObserveSeenTutorialVersionUseCase,
    private val setSeenTutorialVersion: SetSeenTutorialVersionUseCase,
) : ViewModel() {

    val shouldShowTutorial: StateFlow<Boolean> = observeSeenTutorialVersion()
        .map { it < CURRENT_TUTORIAL_VERSION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissTutorial() {
        viewModelScope.launch {
            setSeenTutorialVersion(CURRENT_TUTORIAL_VERSION)
        }
    }

    companion object {
        const val CURRENT_TUTORIAL_VERSION = 1
    }
}
