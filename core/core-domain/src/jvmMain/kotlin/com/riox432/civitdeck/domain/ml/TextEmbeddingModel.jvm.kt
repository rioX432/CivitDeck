package com.riox432.civitdeck.domain.ml

/**
 * Desktop / JVM target ships a no-op implementation by design — the text-to-image
 * search feature is hidden on Desktop (same as the image embedding feature).
 */
actual class TextEmbeddingModel actual constructor() {

    actual val isAvailable: Boolean = false

    actual val embeddingModelId: String = "siglip2-base-p16-224"

    actual suspend fun embed(text: String): FloatArray {
        throw NotImplementedError("Desktop has no on-device text embedding model")
    }
}
