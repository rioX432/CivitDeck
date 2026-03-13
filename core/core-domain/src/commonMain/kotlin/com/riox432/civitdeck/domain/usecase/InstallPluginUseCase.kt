package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.repository.PluginRepository

class InstallPluginUseCase(private val repository: PluginRepository) {
    suspend operator fun invoke(plugin: InstalledPlugin) {
        val existing = repository.getById(plugin.id)
        if (existing != null) return
        repository.install(plugin.copy(state = InstalledPluginState.INSTALLED))
    }
}
