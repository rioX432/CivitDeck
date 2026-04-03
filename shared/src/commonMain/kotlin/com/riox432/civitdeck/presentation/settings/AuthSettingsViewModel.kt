package com.riox432.civitdeck.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthSettingsUiState(
    val apiKey: String? = null,
    val connectedUsername: String? = null,
    val isValidatingApiKey: Boolean = false,
    val apiKeyError: String? = null,
)

class AuthSettingsViewModel(
    private val observeApiKeyUseCase: ObserveApiKeyUseCase,
    private val setApiKeyUseCase: SetApiKeyUseCase,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(AuthSettingsUiState())

    val uiState: StateFlow<AuthSettingsUiState> = combine(
        observeApiKeyUseCase(),
        _mutableState,
    ) { apiKey, mutable ->
        mutable.copy(apiKey = apiKey)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthSettingsUiState())

    init {
        viewModelScope.launch {
            val key = observeApiKeyUseCase().first() ?: return@launch
            validateApiKeyUseCase(key).onSuccess { username ->
                _mutableState.update { it.copy(connectedUsername = username) }
            }
            // Silently ignore failure — key might be expired
        }
    }

    fun onValidateAndSaveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        _mutableState.update { it.copy(isValidatingApiKey = true, apiKeyError = null) }
        viewModelScope.launch {
            validateApiKeyUseCase(apiKey)
                .onSuccess { username ->
                    setApiKeyUseCase(apiKey)
                    _mutableState.update {
                        it.copy(connectedUsername = username, isValidatingApiKey = false)
                    }
                }
                .onFailure { e ->
                    _mutableState.update {
                        it.copy(
                            isValidatingApiKey = false,
                            apiKeyError = e.message ?: "Invalid API key",
                        )
                    }
                }
        }
    }

    fun onClearApiKey() {
        viewModelScope.launch {
            setApiKeyUseCase(null)
            _mutableState.update {
                it.copy(connectedUsername = null, apiKeyError = null)
            }
        }
    }

    fun onRefreshUsername() {
        viewModelScope.launch {
            val key = observeApiKeyUseCase().first() ?: return@launch
            validateApiKeyUseCase(key).onSuccess { username ->
                _mutableState.update { it.copy(connectedUsername = username) }
            }
            // Silently ignore failure — key might be expired
        }
    }
}
