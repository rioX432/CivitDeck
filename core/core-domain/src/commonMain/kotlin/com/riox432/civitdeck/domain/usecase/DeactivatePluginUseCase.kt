package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.repository.PluginRepository

class DeactivatePluginUseCase(private val repository: PluginRepository) {
    suspend operator fun invoke(pluginId: String) {
        repository.updateState(pluginId, InstalledPluginState.INACTIVE)
    }
}
