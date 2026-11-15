package com.riox432.civitdeck.domain.model

/**
 * Exact provider identity of a [Model].
 *
 * [Model.id] is only unique within CivitAI: external providers' identifiers are hashed or
 * parsed into it, so two providers (or two Hugging Face repositories with colliding string
 * hashes) can share one numeric ID. This reference keeps the provider's original identifier
 * instead, and is the only safe source for cross-provider identity and destinations.
 *
 * @property nativeId the provider's exact identifier (CivitAI model ID, Hugging Face
 *   `owner/repository`, TensorArt model ID), or null for a legacy external model whose
 *   original identifier was not retained.
 * @property canonicalUrl the provider's HTTPS page for this model, or null when it cannot be
 *   derived from an exact identifier. Never guessed from a numeric hash.
 */
data class ModelReference(
    val source: ModelSource,
    val nativeId: String?,
    val canonicalUrl: String?,
) {
    init {
        require(nativeId == null || nativeId.isNotBlank()) { "nativeId must not be blank" }
        require(canonicalUrl == null || canonicalUrl.startsWith(HTTPS_PREFIX)) {
            "canonicalUrl must use HTTPS"
        }
    }

    /**
     * Identity that is unique across providers, or null when the exact identifier is unknown.
     * The source name contains no `:`, so the first `:` always separates it from [nativeId].
     */
    val stableKey: String?
        get() = nativeId?.let { "${source.name}:$it" }

    /** CivitAI model ID for in-app detail navigation; null for every other provider. */
    val nativeDetailId: Long?
        get() = if (source == ModelSource.CIVITAI) nativeId?.toLongOrNull() else null

    /**
     * Whether actions keyed by a CivitAI model ID (detail, favorites, history, hide, compare)
     * can target this model.
     */
    val nativeActionsSupported: Boolean
        get() = nativeDetailId != null

    companion object {
        private const val HTTPS_PREFIX = "https://"
        private const val CIVITAI_MODEL_URL = "https://civitai.com/models/"

        fun civitai(modelId: Long): ModelReference = ModelReference(
            source = ModelSource.CIVITAI,
            nativeId = modelId.toString(),
            canonicalUrl = "$CIVITAI_MODEL_URL$modelId",
        )

        /**
         * Reference for a model built without an explicit one. CivitAI IDs are exact; an
         * external model's numeric ID is a lossy derivative, so it yields no identifier.
         */
        fun legacy(source: ModelSource, id: Long): ModelReference =
            if (source == ModelSource.CIVITAI) {
                civitai(id)
            } else {
                ModelReference(source = source, nativeId = null, canonicalUrl = null)
            }
    }
}
