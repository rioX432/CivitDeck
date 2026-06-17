package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel

/**
 * In-memory [ImageEmbeddingModel] for use-case / ViewModel tests.
 *
 * Defaults mirror the platform stubs (unavailable, [embed] throws), so tests
 * that only need a present-but-disabled embedder can use the no-arg form.
 */
class FakeImageEmbeddingModel(
    override val isAvailable: Boolean = false,
    private val embedding: FloatArray? = null,
) : ImageEmbeddingModel {

    override suspend fun embed(imageBytes: ByteArray): FloatArray =
        embedding ?: throw NotImplementedError("FakeImageEmbeddingModel has no embedding configured")
}
