package com.riox432.civitdeck.domain.ml

/**
 * Android stub for the SigLIP-2 text encoder.
 *
 * The text encoder ONNX model requires SentencePiece tokenization (vocab file +
 * BPE encoding) before inference. Until that is implemented, this returns
 * [isAvailable] = false so the UI can show a graceful "coming soon" state.
 *
 * TODO(#713): Bundle `text_model_q4f16.onnx` + `spm_tokenizer.model` from
 *   onnx-community/siglip2-base-patch16-224-ONNX, implement SentencePiece
 *   tokenization, and wire ONNX Runtime inference here.
 */
actual class TextEmbeddingModel actual constructor() {

    actual val isAvailable: Boolean = false

    actual val embeddingModelId: String = EMBEDDING_MODEL_ID

    actual suspend fun embed(text: String): FloatArray {
        throw NotImplementedError(
            "SigLIP-2 text encoder not yet available — SentencePiece tokenizer bundling needed",
        )
    }

    private companion object {
        /** Matches the SigLIP-2 base model used for image embeddings across all platforms. */
        private const val EMBEDDING_MODEL_ID = "siglip2-base-p16-224"
    }
}
