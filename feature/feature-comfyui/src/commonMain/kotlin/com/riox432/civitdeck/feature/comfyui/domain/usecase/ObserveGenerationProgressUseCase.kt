package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import kotlinx.coroutines.flow.Flow

class ObserveGenerationProgressUseCase(private val repository: ComfyUIGenerationRepository) {
    /** Legacy overload using host + port (plain ws://). */
    operator fun invoke(promptId: String, host: String, port: Int): Flow<GenerationProgress> =
        repository.observeGenerationProgress(promptId, host, port)

    /** New overload using base URL and WS scheme (ws/wss). */
    operator fun invoke(promptId: String, baseUrl: String, wsScheme: String): Flow<GenerationProgress> =
        repository.observeGenerationProgress(promptId, baseUrl, wsScheme)
}
