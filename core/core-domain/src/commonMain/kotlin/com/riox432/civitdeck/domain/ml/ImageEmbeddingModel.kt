package com.riox432.civitdeck.domain.ml

/**
 * Platform-specific on-device image embedding extractor.
 *
 * Each platform supplies its own implementation:
 *  - Android: ONNX Runtime (#701)
 *  - iOS: Core ML (#700)
 *  - Desktop / JVM: no-op
 *
 * The current foundation (#699) ships stub implementations that throw
 * [NotImplementedError] so the wiring builds and tests can use a fake instead.
 *
 * Returned vectors are L2-normalized so cosine similarity reduces to a dot product.
 * Vector dimension is fixed by the chosen model — currently 768 for SigLIP-2 base
 * (see docs/research/siglip2-feasibility.md).
 */
expect class ImageEmbeddingModel() {
    /**
     * Returns true when the platform implementation can produce real embeddings.
     * Stub / no-op implementations return false so callers can short-circuit.
     */
    val isAvailable: Boolean

    /**
     * Embeds the given image bytes (encoded JPEG/PNG) into a fixed-length vector.
     *
     * @throws NotImplementedError when [isAvailable] is false.
     */
    suspend fun embed(imageBytes: ByteArray): FloatArray
}
