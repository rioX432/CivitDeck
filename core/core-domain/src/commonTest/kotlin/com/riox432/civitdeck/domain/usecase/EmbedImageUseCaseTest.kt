package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbedImageUseCaseTest {

    /** Test embedder mirroring the platform stubs: unavailable, [embed] throws. */
    private class StubImageEmbeddingModel : ImageEmbeddingModel {
        override val isAvailable: Boolean = false
        override suspend fun embed(imageBytes: ByteArray): FloatArray =
            throw NotImplementedError("stub")
    }

    @Test
    fun delegates_to_underlying_embedder_when_available() = runTest {
        // Verify the use case mirrors the embedder's availability and propagates the
        // NotImplementedError thrown by an unavailable embedder. Now that the embedder is a
        // commonMain interface, the test uses a local stub instead of the platform actual.
        // (#700 / #701 will replace these with real implementations and richer tests.)
        val useCase = EmbedImageUseCase(StubImageEmbeddingModel())

        assertFalse(useCase.isAvailable)
        assertFailsWith<NotImplementedError> {
            useCase(byteArrayOf(1, 2, 3))
        }
    }

    @Test
    fun float_array_helpers_round_trip() {
        // Sanity: contentEquals assertions used in repo tests behave as expected
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(1f, 2f, 3f)
        assertContentEquals(a, b)
        assertEquals(a.size, b.size)
        assertTrue(a.contentEquals(b))
    }
}
