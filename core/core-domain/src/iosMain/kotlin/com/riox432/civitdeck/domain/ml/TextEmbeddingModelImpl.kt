package com.riox432.civitdeck.domain.ml

/**
 * iOS stub for the SigLIP-2 text encoder.
 *
 * The text encoder requires:
 *  1. Core ML conversion of the SigLIP-2 text model (similar to the vision encoder
 *     in `scripts/convert_siglip2_coreml.py`)
 *  2. SentencePiece tokenizer implementation in Swift
 *  3. A bridge interface (similar to [SigLIP2Bridge] for image encoding)
 *
 * Until both are implemented, this returns [isAvailable] = false so the UI can
 * show a graceful "coming soon" state.
 *
 * TODO(#805): Convert text encoder to Core ML, bundle tokenizer vocab, implement
 *   SentencePiece tokenization in Swift, and wire via a bridge. The Android
 *   implementation (using ONNX Runtime + [SigLipTokenizer]) can serve as a
 *   reference for the tokenization logic and input/output tensor format.
 */
class TextEmbeddingModelImpl : TextEmbeddingModel {

    override val isAvailable: Boolean = false

    override val embeddingModelId: String = EMBEDDING_MODEL_ID

    override suspend fun embed(text: String): FloatArray {
        throw NotImplementedError(
            "SigLIP-2 text encoder not yet available on iOS — Core ML conversion + tokenizer needed",
        )
    }

    private companion object {
        private const val EMBEDDING_MODEL_ID = "siglip2-base-p16-224"
    }
}
