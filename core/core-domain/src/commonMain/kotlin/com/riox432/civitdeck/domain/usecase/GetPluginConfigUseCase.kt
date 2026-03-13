package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.PluginRepository

class GetPluginConfigUseCase(private val repository: PluginRepository) {
    suspend operator fun invoke(pluginId: String): String = repository.getConfig(pluginId)
}
