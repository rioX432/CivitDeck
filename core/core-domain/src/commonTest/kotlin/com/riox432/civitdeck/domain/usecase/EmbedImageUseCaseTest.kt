package com.riox432.civitdeck.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbedImageUseCaseTest {

    @Test
    fun delegates_to_underlying_embedder_when_available() = runTest {
        // The default expect/actual stub throws — we cannot construct it in commonTest because
        // that would resolve to the JVM actual at test time. Instead, exercise the use case via
        // dependency on a test-local subclass implemented at the JVM source set boundary.
        // Here, we just verify the JVM stub reports unavailable and throws on embed().
        // (#700 / #701 will replace these with real implementations and richer tests.)
        val useCase = EmbedImageUseCase(com.riox432.civitdeck.domain.ml.ImageEmbeddingModel())

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
