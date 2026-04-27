package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface ComfyUIGenerationRepository {
    suspend fun fetchCheckpoints(): List<String>
    suspend fun fetchLoras(): List<String>
    suspend fun fetchControlNets(): List<String>
    suspend fun submitGeneration(params: ComfyUIGenerationParams): String
    suspend fun pollGenerationResult(promptId: String): GenerationResult
    fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress>
    fun observeGenerationProgress(promptId: String, baseUrl: String, wsScheme: String): Flow<GenerationProgress>
    fun getImageUrl(filename: String, subfolder: String = "", type: String = "output"): String
    suspend fun interruptGeneration()

    /**
     * Upload a mask PNG image to the ComfyUI server.
     * @param maskPngBytes raw PNG bytes of the mask image
     * @return the filename on the server (e.g. "mask_12345.png")
     */
    suspend fun uploadMaskImage(maskPngBytes: ByteArray): String

    /**
     * Fetch the full /object_info response from the ComfyUI server.
     * Returns the raw JSON string containing schema definitions for all node types.
     */
    suspend fun fetchObjectInfo(): String
}
