package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import kotlinx.coroutines.flow.Flow

interface SDWebUIGenerationRepository {
    fun generateImage(params: SDWebUIGenerationParams): Flow<SDWebUIGenerationProgress>
    suspend fun interruptGeneration()
}
