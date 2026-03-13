package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.repository.PluginRepository
import kotlinx.coroutines.flow.Flow

class ObserveInstalledPluginsUseCase(private val repository: PluginRepository) {
    operator fun invoke(): Flow<List<InstalledPlugin>> = repository.observeAll()
}
