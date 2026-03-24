package com.riox432.civitdeck.ui.plugin

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.usecase.ActivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.DeactivatePluginUseCase
import com.riox432.civitdeck.domain.usecase.GetPluginConfigUseCase
import com.riox432.civitdeck.domain.usecase.ObserveInstalledPluginsUseCase
import com.riox432.civitdeck.domain.usecase.UninstallPluginUseCase
import com.riox432.civitdeck.domain.usecase.UpdatePluginConfigUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesktopPluginUiState(
    val plugins: List<InstalledPlugin> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedPluginConfig: String = "{}",
)

class DesktopPluginViewModel(
    private val observeInstalledPluginsUseCase: ObserveInstalledPluginsUseCase,
    private val activatePluginUseCase: ActivatePluginUseCase,
    private val deactivatePluginUseCase: DeactivatePluginUseCase,
    private val uninstallPluginUseCase: UninstallPluginUseCase,
    private val getPluginConfigUseCase: GetPluginConfigUseCase,
    private val updatePluginConfigUseCase: UpdatePluginConfigUseCase,
) : ViewModel() {

    private val scope = viewModelScope

    private val _uiState = MutableStateFlow(DesktopPluginUiState())
    val uiState: StateFlow<DesktopPluginUiState> = _uiState

    init {
        observePlugins()
    }

    private fun observePlugins() {
        scope.launch {
            observeInstalledPluginsUseCase().collect { plugins ->
                _uiState.update { it.copy(plugins = plugins, isLoading = false) }
            }
        }
    }

    fun togglePlugin(pluginId: String, isActive: Boolean) {
        scope.launch {
            try {
                if (isActive) activatePluginUseCase(pluginId) else deactivatePluginUseCase(pluginId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to toggle plugin") }
            }
        }
    }

    fun loadConfig(pluginId: String) {
        scope.launch {
            try {
                val config = getPluginConfigUseCase(pluginId)
                _uiState.update { it.copy(selectedPluginConfig = config) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load config") }
            }
        }
    }

    fun saveConfig(pluginId: String, configJson: String) {
        scope.launch {
            try {
                updatePluginConfigUseCase(pluginId, configJson)
                _uiState.update { it.copy(selectedPluginConfig = configJson) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save config") }
            }
        }
    }

    fun uninstallPlugin(pluginId: String) {
        scope.launch {
            try {
                uninstallPluginUseCase(pluginId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to uninstall plugin") }
            }
        }
    }

    fun isPluginActive(plugin: InstalledPlugin): Boolean =
        plugin.state == InstalledPluginState.ACTIVE

    public override fun onCleared() {
        super.onCleared()
    }
}
