package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.repository.ServerDiscoveryRepository
import kotlinx.coroutines.flow.Flow

class ScanForServersUseCase(private val repository: ServerDiscoveryRepository) {
    operator fun invoke(): Flow<List<DiscoveredServer>> = repository.scanForServers()
}
