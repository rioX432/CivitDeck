package com.riox432.civitdeck.domain.ml

/**
 * Desktop / JVM target ships a no-op implementation by design — the "Find Similar"
 * feature is hidden on Desktop (see docs/research/siglip2-feasibility.md).
 */
actual class ImageEmbeddingModel actual constructor() {
    actual val isAvailable: Boolean = false

    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        throw NotImplementedError("Desktop has no on-device embedding model")
    }
}
