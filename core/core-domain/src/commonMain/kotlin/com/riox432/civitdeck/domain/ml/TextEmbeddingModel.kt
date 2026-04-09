package com.riox432.civitdeck.domain.ml

/**
 * Platform-specific on-device text embedding extractor (SigLIP-2 text encoder).
 *
 * SigLIP-2 text and image embeddings share the same 768-d vector space, so cosine
 * similarity between a text embedding and cached image embeddings enables cross-modal
 * "describe what you want" search.
 *
 * The text encoder requires SentencePiece tokenization before inference, which adds
 * non-trivial complexity (vocab file bundling, BPE encoding). Until tokenization is
 * implemented, all platforms return [isAvailable] = false.
 *
 * See also: [ImageEmbeddingModel] for the image-side counterpart.
 */
expect class TextEmbeddingModel() {
    /**
     * Returns true when the platform can produce real text embeddings.
     * Currently false on all platforms — tokenizer bundling is TODO.
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
