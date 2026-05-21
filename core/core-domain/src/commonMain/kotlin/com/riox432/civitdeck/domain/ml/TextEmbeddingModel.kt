package com.riox432.civitdeck.domain.ml

/**
 * Platform-specific on-device text embedding extractor (SigLIP-2 text encoder).
 *
 * SigLIP-2 text and image embeddings share the same 768-d vector space, so cosine
 * similarity between a text embedding and cached image embeddings enables cross-modal
 * "describe what you want" search.
 *
 * Platform implementations:
 *  - **Android**: ONNX Runtime + SentencePiece tokenizer. Requires model files in
 *    `assets/ml/` — generate via `scripts/export_siglip2_text_onnx.py`. Returns
 *    [isAvailable] = true only when both the ONNX model and tokenizer vocab are present.
 *  - **iOS**: Stub (TODO #805). Needs Core ML conversion + Swift tokenizer.
 *  - **Desktop / JVM**: No-op by design.
 *
 * Returned vectors are L2-normalized so cosine similarity reduces to a dot product.
 * Vector dimension is 768, matching [ImageEmbeddingModel].
 *
 * See also: [ImageEmbeddingModel] for the image-side counterpart.
 */
expect class TextEmbeddingModel() {
    /**
     * Returns true when the platform can produce real text embeddings.
     * Android: true when both the ONNX model and tokenizer vocab are loaded.
     * iOS/Desktop: false (not yet implemented).
     */
    val isAvailable: Boolean

    /**
     * The embedding model identifier, matching the one used for image embeddings
     * so cosine similarity works across modalities.
     */
    val embeddingModelId: String

    /**
     * Embeds the given text into a fixed-length vector in the same space as
     * image embeddings from [ImageEmbeddingModel].
     *
     * @throws NotImplementedError when [isAvailable] is false.
     */
    suspend fun embed(text: String): FloatArray
}
