package com.riox432.civitdeck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val setNsfwFilterUseCase: SetNsfwFilterUseCase,
) : ViewModel() {

    val nsfwFilterLevel: StateFlow<NsfwFilterLevel> =
        observeNsfwFilterUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NsfwFilterLevel.Off)

    fun onNsfwFilterChanged(level: NsfwFilterLevel) {
        viewModelScope.launch {
            setNsfwFilterUseCase(level)
        }
    }
}
