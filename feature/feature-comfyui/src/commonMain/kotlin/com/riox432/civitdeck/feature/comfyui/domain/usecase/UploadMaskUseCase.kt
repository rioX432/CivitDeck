package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository

/**
 * Uploads mask PNG bytes to the ComfyUI server via /upload/image endpoint.
 * Returns the uploaded filename on success.
 */
class UploadMaskUseCase(
    private val repository: ComfyUIGenerationRepository,
) {
    suspend operator fun invoke(maskPngBytes: ByteArray): String {
        return repository.uploadMaskImage(maskPngBytes)
    }
}
