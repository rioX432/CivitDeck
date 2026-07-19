package com.riox432.civitdeck.domain.ml

/**
 * Identifiers for the on-device embedding models. Image and text embeddings share the
 * same identifier because SigLIP-2 places both modalities in one 768-d vector space, so
 * cached image vectors and query vectors must be looked up under the same key.
 */
object EmbeddingModels {
    /** SigLIP-2 base, patch16, 224px — the single model bundled across platforms. */
    const val SIGLIP2_BASE = "siglip2-base-p16-224"
}
