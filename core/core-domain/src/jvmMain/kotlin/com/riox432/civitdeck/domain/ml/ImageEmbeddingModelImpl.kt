package com.riox432.civitdeck.domain.ml

/**
 * Desktop / JVM target ships a no-op implementation by design — the "Find Similar"
 * feature is hidden on Desktop (see docs/research/siglip2-feasibility.md).
 */
class ImageEmbeddingModelImpl : ImageEmbeddingModel {
    override val isAvailable: Boolean = false

    override suspend fun embed(imageBytes: ByteArray): FloatArray {
        throw NotImplementedError("Desktop has no on-device embedding model")
    }
}
