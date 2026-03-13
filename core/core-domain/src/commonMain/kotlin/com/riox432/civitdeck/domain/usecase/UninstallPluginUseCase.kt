package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.PluginRepository

class UninstallPluginUseCase(private val repository: PluginRepository) {
    suspend operator fun invoke(pluginId: String) {
        repository.uninstall(pluginId)
    }
}
