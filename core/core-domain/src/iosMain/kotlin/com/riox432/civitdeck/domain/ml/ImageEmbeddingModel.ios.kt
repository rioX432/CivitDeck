package com.riox432.civitdeck.domain.ml

/**
 * Stub iOS implementation. The real Core ML backend is delivered in #700.
 */
actual class ImageEmbeddingModel actual constructor() {
    actual val isAvailable: Boolean = false

    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        throw NotImplementedError("iOS ImageEmbeddingModel is not implemented yet (see #700)")
    }
}
