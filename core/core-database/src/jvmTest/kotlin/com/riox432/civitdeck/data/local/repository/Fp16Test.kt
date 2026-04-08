package com.riox432.civitdeck.data.local.repository

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Fp16Test {

    @Test
    fun round_trips_typical_normalized_embedding_values_within_tolerance() {
        // L2-normalized 768-d SigLIP-2 embeddings have values roughly in [-0.2, 0.2].
        val original = floatArrayOf(
            0f, 1f, -1f, 0.5f, -0.5f, 0.125f, -0.125f, 0.0625f, -0.0625f,
            0.0001f, -0.0001f, 0.999f, -0.999f, 0.333f, -0.333f, 0.123456f,
        )

        val decoded = Fp16.decode(Fp16.encode(original), original.size)

        assertEquals(original.size, decoded.size)
        for (i in original.indices) {
            // fp16 has ~3 decimal digits of precision; 1e-3 absolute is comfortably above
            // the worst-case rounding error for the magnitudes we care about
            assertTrue(
                abs(original[i] - decoded[i]) < 1e-3f,
                "value $i: expected ${original[i]} but decoded ${decoded[i]}",
            )
        }
    }

    @Test
    fun round_trips_special_values() {
        val values = floatArrayOf(0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        val decoded = Fp16.decode(Fp16.encode(values), values.size)

        // Use abs() for ±0 because Float.equals distinguishes the two bit patterns
        assertTrue(abs(decoded[0]) == 0f)
        assertEquals(Float.POSITIVE_INFINITY, decoded[1])
        assertEquals(Float.NEGATIVE_INFINITY, decoded[2])
    }

    @Test
    fun encoded_size_is_two_bytes_per_value() {
        val v = FloatArray(768) { 0.01f }
        assertEquals(768 * 2, Fp16.encode(v).size)
    }

    @Test
    fun cosine_similarity_survives_round_trip() {
        // The whole point of fp16 storage: cosine scores should be preserved within ε.
        val a = normalize(FloatArray(64) { kotlin.math.sin(it.toFloat()) })
        val b = normalize(FloatArray(64) { kotlin.math.cos(it.toFloat()) })
        val originalDot = dot(a, b)

        val aRoundTripped = Fp16.decode(Fp16.encode(a), a.size)
        val bRoundTripped = Fp16.decode(Fp16.encode(b), b.size)
        val roundTrippedDot = dot(aRoundTripped, bRoundTripped)

        assertTrue(
            abs(originalDot - roundTrippedDot) < 1e-3f,
            "cosine drift too large: $originalDot vs $roundTrippedDot",
        )
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = kotlin.math.sqrt(sum)
        return FloatArray(v.size) { v[it] / norm }
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
