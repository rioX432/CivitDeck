package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.domain.model.SystemStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptimizationSuggestionsTest {

    private fun stats(
        vramTotalMB: Long = 8_192L,
        vramFreeMB: Long = 4_096L,
    ) = SystemStats(
        gpuName = "RTX 3060",
        gpuType = "cuda",
        vramTotalMB = vramTotalMB,
        vramFreeMB = vramFreeMB,
        ramTotalMB = 32_768L,
        ramFreeMB = 16_384L,
        os = "Linux",
        comfyuiVersion = "0.2.0",
        pytorchVersion = "2.3.0",
    )

    @Test
    fun suggests_fp16_tiling_for_low_vram() {
        val suggestions = generateOptimizationSuggestions(stats(vramTotalMB = 4_096L))
        assertTrue(suggestions.any { it.id == "fp16_tiling" })
    }

    @Test
    fun suggests_fp16_for_mid_vram() {
        val suggestions = generateOptimizationSuggestions(stats(vramTotalMB = 7_168L))
        assertTrue(suggestions.any { it.id == "fp16" })
    }

    @Test
    fun no_precision_suggestion_for_high_vram() {
        val suggestions = generateOptimizationSuggestions(stats(vramTotalMB = 24_576L))
        assertTrue(suggestions.none { it.id == "fp16_tiling" || it.id == "fp16" })
    }

    @Test
    fun suggests_low_free_vram() {
        // 10% free = below 15% threshold
        val suggestions = generateOptimizationSuggestions(
            stats(vramTotalMB = 12_288L, vramFreeMB = 1_228L),
        )
        assertTrue(suggestions.any { it.id == "low_free_vram" })
    }

    @Test
    fun no_low_free_vram_when_plenty_available() {
        // 50% free
        val suggestions = generateOptimizationSuggestions(
            stats(vramTotalMB = 12_288L, vramFreeMB = 6_144L),
        )
        assertTrue(suggestions.none { it.id == "low_free_vram" })
    }

    @Test
    fun empty_suggestions_for_high_vram_with_plenty_free() {
        val suggestions = generateOptimizationSuggestions(
            stats(vramTotalMB = 24_576L, vramFreeMB = 20_000L),
        )
        assertEquals(0, suggestions.size)
    }
}
