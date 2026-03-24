package com.riox432.civitdeck.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContentFilterSettingsUiState(
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(),
    val hiddenModels: List<HiddenModel> = emptyList(),
    val excludedTags: List<String> = emptyList(),
)

@Suppress("LongParameterList")
class ContentFilterSettingsViewModel(
    observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val setNsfwFilterUseCase: SetNsfwFilterUseCase,
    observeNsfwBlurSettingsUseCase: ObserveNsfwBlurSettingsUseCase,
    private val setNsfwBlurSettingsUseCase: SetNsfwBlurSettingsUseCase,
    private val getHiddenModelsUseCase: GetHiddenModelsUseCase,
    private val unhideModelUseCase: UnhideModelUseCase,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(ContentFilterSettingsUiState())

    val uiState: StateFlow<ContentFilterSettingsUiState> = combine(
        observeNsfwFilterUseCase(),
        observeNsfwBlurSettingsUseCase(),
        _mutableState,
    ) { nsfw, blur, mutable ->
        mutable.copy(
            nsfwFilterLevel = nsfw,
            nsfwBlurSettings = blur,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentFilterSettingsUiState())

    init {
        viewModelScope.launch {
            val hidden = getHiddenModelsUseCase()
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden, excludedTags = tags) }
        }
    }

    fun onNsfwFilterChanged(level: NsfwFilterLevel) {
        viewModelScope.launch { setNsfwFilterUseCase(level) }
    }

    fun onNsfwBlurSettingsChanged(settings: NsfwBlurSettings) {
        viewModelScope.launch { setNsfwBlurSettingsUseCase(settings) }
    }

    fun onUnhideModel(modelId: Long) {
        viewModelScope.launch {
            unhideModelUseCase(modelId)
            val hidden = getHiddenModelsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden) }
        }
    }

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addExcludedTagUseCase(trimmed)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        viewModelScope.launch {
            removeExcludedTagUseCase(tag)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onNsfwFilterToggle() {
        val newLevel = if (uiState.value.nsfwFilterLevel == NsfwFilterLevel.Off) {
            NsfwFilterLevel.All
        } else {
            NsfwFilterLevel.Off
        }
        onNsfwFilterChanged(newLevel)
    }
}
