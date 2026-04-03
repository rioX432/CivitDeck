package com.riox432.civitdeck.ui.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.feature.prompts.domain.usecase.DeleteSavedPromptUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ObserveTemplatesUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.SearchSavedPromptsUseCase
import com.riox432.civitdeck.feature.prompts.domain.usecase.ToggleTemplateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PromptTab { All, History, Templates }

@OptIn(ExperimentalCoroutinesApi::class)
class SavedPromptsViewModel(
    private val observeSavedPromptsUseCase: ObserveSavedPromptsUseCase,
    private val deleteSavedPromptUseCase: DeleteSavedPromptUseCase,
    private val searchSavedPromptsUseCase: SearchSavedPromptsUseCase,
    private val observeTemplatesUseCase: ObserveTemplatesUseCase,
    private val toggleTemplateUseCase: ToggleTemplateUseCase,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PromptTab.All)
    val selectedTab: StateFlow<PromptTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val prompts: StateFlow<List<SavedPrompt>> =
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                observeSavedPromptsUseCase()
            } else {
                searchSavedPromptsUseCase(query)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<SavedPrompt>> =
        observeTemplatesUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onTabSelected(tab: PromptTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteSavedPromptUseCase(id)
        }
    }

    fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String? = null) {
        viewModelScope.launch {
            toggleTemplateUseCase(id, isTemplate, templateName)
        }
    }
}
