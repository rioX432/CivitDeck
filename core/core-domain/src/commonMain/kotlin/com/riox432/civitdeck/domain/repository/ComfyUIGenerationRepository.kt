package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import kotlinx.coroutines.flow.Flow

interface ComfyUIGenerationRepository {
    suspend fun fetchCheckpoints(): List<String>
    suspend fun fetchLoras(): List<String>
    suspend fun fetchControlNets(): List<String>
    suspend fun submitGeneration(params: ComfyUIGenerationParams): String
    suspend fun pollGenerationResult(promptId: String): GenerationResult
    fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress>
    fun getImageUrl(filename: String, subfolder: String = "", type: String = "output"): String
    suspend fun interruptGeneration()
}
