package com.riox432.civitdeck.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.UpdateResult
import com.riox432.civitdeck.domain.usecase.CheckForUpdateUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAutoUpdateCheckUseCase
import com.riox432.civitdeck.domain.usecase.SetAutoUpdateCheckUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopUpdateUiState(
    val updateResult: UpdateResult? = null,
    val isChecking: Boolean = false,
    val autoCheckEnabled: Boolean = true,
    val showBanner: Boolean = false,
)

class DesktopUpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    observeAutoUpdateCheckUseCase: ObserveAutoUpdateCheckUseCase,
    private val setAutoUpdateCheckUseCase: SetAutoUpdateCheckUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesktopUpdateUiState())
    val uiState: StateFlow<DesktopUpdateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAutoUpdateCheckUseCase().collect { enabled ->
                _uiState.update { it.copy(autoCheckEnabled = enabled) }
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            try {
                val result = checkForUpdateUseCase()
                _uiState.update {
                    it.copy(
                        updateResult = result,
                        isChecking = false,
                        showBanner = result.isUpdateAvailable,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(showBanner = false) }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        viewModelScope.launch { setAutoUpdateCheckUseCase(enabled) }
    }
}
