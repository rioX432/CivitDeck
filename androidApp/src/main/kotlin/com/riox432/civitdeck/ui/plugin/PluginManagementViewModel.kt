package com.riox432.civitdeck.ui.plugin

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PluginManagementUiState(
    val plugins: List<InstalledPlugin> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPluginConfig: String = "{}",
)

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
                _uiState.value = _uiState.value.copy(
                    plugins = plugins,
                    isLoading = false,
                )
            }
        }
    }

    fun togglePlugin(pluginId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                if (isActive) {
                    activatePluginUseCase(pluginId)
                } else {
                    deactivatePluginUseCase(pluginId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to toggle plugin",
                )
            }
        }
    }

    fun loadConfig(pluginId: String) {
        viewModelScope.launch {
            try {
                val config = getPluginConfigUseCase(pluginId)
                _uiState.value = _uiState.value.copy(selectedPluginConfig = config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load config",
                )
            }
        }
    }

    fun saveConfig(pluginId: String, configJson: String) {
        viewModelScope.launch {
            try {
                updatePluginConfigUseCase(pluginId, configJson)
                _uiState.value = _uiState.value.copy(selectedPluginConfig = configJson)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to save config",
                )
            }
        }
    }

    fun uninstallPlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                uninstallPluginUseCase(pluginId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to uninstall plugin",
                )
            }
        }
    }

    fun isPluginActive(plugin: InstalledPlugin): Boolean =
        plugin.state == InstalledPluginState.ACTIVE

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
