package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.ml.TextEmbeddingModel

/**
 * In-memory [TextEmbeddingModel] for use-case / ViewModel tests.
 *
 * Defaults mirror the platform stubs (unavailable, [embed] throws).
 */
class FakeTextEmbeddingModel(
    override val isAvailable: Boolean = false,
    override val embeddingModelId: String = "fake-text-embedder",
    private val embedding: FloatArray? = null,
) : TextEmbeddingModel {

    override suspend fun embed(text: String): FloatArray =
        embedding ?: throw NotImplementedError("FakeTextEmbeddingModel has no embedding configured")
}
