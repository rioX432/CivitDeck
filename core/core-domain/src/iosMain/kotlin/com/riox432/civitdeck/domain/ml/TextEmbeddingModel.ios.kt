package com.riox432.civitdeck.domain.ml

/**
 * iOS stub for the SigLIP-2 text encoder.
 *
 * The text encoder requires SentencePiece tokenization and a Core ML conversion of
 * the text encoder model. Until both are implemented, this returns
 * [isAvailable] = false so the UI can show a graceful "coming soon" state.
 *
 * TODO(#713): Convert text encoder to Core ML, bundle tokenizer vocab, implement
 *   SentencePiece tokenization in Swift, and wire via a bridge (similar to
 *   [SigLIP2Bridge] for the image encoder).
 */
actual class TextEmbeddingModel actual constructor() {

    actual val isAvailable: Boolean = false

    actual val embeddingModelId: String = EMBEDDING_MODEL_ID

    actual suspend fun embed(text: String): FloatArray {
        throw NotImplementedError(
            "SigLIP-2 text encoder not yet available on iOS — Core ML conversion + tokenizer needed",
        )
    }

    private companion object {
        private const val EMBEDDING_MODEL_ID = "siglip2-base-p16-224"
    }
}
