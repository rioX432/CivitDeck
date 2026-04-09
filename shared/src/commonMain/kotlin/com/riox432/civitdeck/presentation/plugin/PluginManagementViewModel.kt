package com.riox432.civitdeck.presentation.plugin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PluginManagementUiState(
    val plugins: List<InstalledPlugin> = emptyList(),
    override val isLoading: Boolean = true,
    override val error: String? = null,
    val selectedPluginConfig: String = "{}",
) : UiLoadingState

@Suppress("LongParameterList")
class PluginManagementViewModel(
    private val observeInstalledPluginsUseCase: ObserveInstalledPluginsUseCase,
    private val activatePluginUseCase: ActivatePluginUseCase,
    private val deactivatePluginUseCase: DeactivatePluginUseCase,
    private val uninstallPluginUseCase: UninstallPluginUseCase,
    private val getPluginConfigUseCase: GetPluginConfigUseCase,
    private val updatePluginConfigUseCase: UpdatePluginConfigUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginManagementUiState())
    val uiState: StateFlow<PluginManagementUiState> = _uiState

    init {
        observePlugins()
    }

    private fun observePlugins() {
        viewModelScope.launch {
            observeInstalledPluginsUseCase().collect { plugins ->
                _uiState.update { it.copy(plugins = plugins, isLoading = false) }
            }
        }
    }

    fun togglePlugin(pluginId: String, isActive: Boolean) {
        viewModelScope.launch {
            suspendRunCatching {
                if (isActive) {
                    activatePluginUseCase(pluginId)
                } else {
                    deactivatePluginUseCase(pluginId)
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to toggle plugin") }
            }
        }
    }

    fun loadConfig(pluginId: String) {
        viewModelScope.launch {
            suspendRunCatching { getPluginConfigUseCase(pluginId) }
                .onSuccess { config ->
                    _uiState.update { it.copy(selectedPluginConfig = config) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to load config") }
                }
        }
    }

    fun saveConfig(pluginId: String, configJson: String) {
        viewModelScope.launch {
            suspendRunCatching { updatePluginConfigUseCase(pluginId, configJson) }
                .onSuccess {
                    _uiState.update { it.copy(selectedPluginConfig = configJson) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to save config") }
                }
        }
    }

    fun uninstallPlugin(pluginId: String) {
        viewModelScope.launch {
            suspendRunCatching { uninstallPluginUseCase(pluginId) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to uninstall plugin") }
                }
        }
    }

    fun isPluginActive(plugin: InstalledPlugin): Boolean =
        plugin.state == InstalledPluginState.ACTIVE

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
