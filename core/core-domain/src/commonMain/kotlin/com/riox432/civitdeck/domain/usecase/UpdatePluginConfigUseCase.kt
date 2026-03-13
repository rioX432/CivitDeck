package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.PluginRepository

class UpdatePluginConfigUseCase(private val repository: PluginRepository) {
    suspend operator fun invoke(pluginId: String, configJson: String) {
        repository.updateConfig(pluginId, configJson)
    }
}
