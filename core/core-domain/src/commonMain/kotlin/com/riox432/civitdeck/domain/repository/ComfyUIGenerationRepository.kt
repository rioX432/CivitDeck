package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIGenerationParams
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.model.DiffusionModelResources
import com.riox432.civitdeck.domain.model.GenerationProgress
import com.riox432.civitdeck.domain.model.GenerationResult
import com.riox432.civitdeck.domain.model.PreparedReferenceGeneration
import kotlinx.coroutines.flow.Flow

interface ComfyUIGenerationRepository {
    suspend fun fetchCheckpoints(): List<String>
    suspend fun fetchLoras(): List<String>
    suspend fun fetchControlNets(): List<String>

    /** Fetch the diffusion-model, text-encoder, VAE and CLIP-type choices of the split loader nodes. */
    suspend fun fetchDiffusionModelResources(): DiffusionModelResources

    suspend fun submitGeneration(params: ComfyUIGenerationParams): String
    suspend fun pollGenerationResult(promptId: String): GenerationResult
    fun observeGenerationProgress(promptId: String, host: String, port: Int): Flow<GenerationProgress>
    fun observeGenerationProgress(promptId: String, baseUrl: String, wsScheme: String): Flow<GenerationProgress>
    fun getImageUrl(filename: String, subfolder: String = "", type: String = "output"): String

    // Bound overloads run against the job's captured target. The defaults fail instead of
    // delegating to the active-server methods above, which could reach a different server.

    /** Submit [params] to [target] and return a handle bound to that same server. */
    suspend fun submitGeneration(params: ComfyUIGenerationParams, target: ComfyUIJobTarget): ComfyUIJobHandle =
        throw UnsupportedOperationException("Bound submitGeneration is not implemented")

    /** Poll [job] on its own target; result image URLs point at that target. */
    suspend fun pollGenerationResult(job: ComfyUIJobHandle): GenerationResult =
        throw UnsupportedOperationException("Bound pollGenerationResult is not implemented")

    /** Observe the progress of [job] over its own target's socket. */
    fun observeGenerationProgress(job: ComfyUIJobHandle): Flow<GenerationProgress> =
        throw UnsupportedOperationException("Bound observeGenerationProgress is not implemented")

    /**
     * Build the built-in text-to-image graph for saved reference [referenceId] once, with a
     * concrete seed, and bind it to [target]. Custom workflows, ControlNet and inpainting inputs
     * are rejected; [target] must still match its saved connection.
     */
    suspend fun prepareReferenceGeneration(
        referenceId: Long,
        target: ComfyUIJobTarget,
        params: ComfyUIGenerationParams,
    ): PreparedReferenceGeneration =
        throw UnsupportedOperationException("prepareReferenceGeneration is not implemented")

    /** Send [prepared]'s stored graph, unchanged, to its own target. */
    suspend fun submitPreparedReference(prepared: PreparedReferenceGeneration): ComfyUIJobHandle =
        throw UnsupportedOperationException("submitPreparedReference is not implemented")

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
