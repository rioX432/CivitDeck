package com.riox432.civitdeck.domain.ml

/**
 * Stub Android implementation. The real ONNX Runtime backend is delivered in #701.
 */
actual class ImageEmbeddingModel actual constructor() {
    actual val isAvailable: Boolean = false

    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        throw NotImplementedError("Android ImageEmbeddingModel is not implemented yet (see #701)")
    }
}
