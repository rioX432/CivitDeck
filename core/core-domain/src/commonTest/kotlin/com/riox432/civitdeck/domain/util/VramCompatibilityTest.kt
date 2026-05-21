package com.riox432.civitdeck.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class VramCompatibilityTest {

    @Test
    fun fits_when_file_well_within_vram() {
        // 2 GB file (2_097_152 KB) with 12 GB VRAM (12_288 MB)
        // File = 2048 MB, VRAM = 12288 MB, margin = 2048 MB
        // 2048 + 2048 = 4096 <= 12288 => FITS
        val result = calculateVramCompatibility(
            modelFileSizeKB = 2_097_152.0,
            vramTotalMB = 12_288L,
        )
        assertEquals(VramCompatibility.FITS, result)
    }

    @Test
    fun tight_when_file_within_vram_but_close() {
        // 6 GB file (6_291_456 KB) with 8 GB VRAM (8_192 MB)
        // File = 6144 MB, VRAM = 8192 MB, margin = 2048 MB
        // 6144 + 2048 = 8192 <= 8192 => FITS (boundary)
        // BUT 6.5 GB file should be TIGHT
        val result = calculateVramCompatibility(
            modelFileSizeKB = 6_815_744.0, // ~6.5 GB
            vramTotalMB = 8_192L,
        )
        assertEquals(VramCompatibility.TIGHT, result)
    }

    @Test
    fun needs_offloading_when_file_exceeds_vram() {
        // 7 GB file with 4 GB VRAM
        val result = calculateVramCompatibility(
            modelFileSizeKB = 7_340_032.0, // 7 GB
            vramTotalMB = 4_096L,
        )
        assertEquals(VramCompatibility.NEEDS_OFFLOADING, result)
    }

    @Test
    fun unknown_when_no_vram() {
        val result = calculateVramCompatibility(
            modelFileSizeKB = 2_097_152.0,
            vramTotalMB = 0L,
        )
        assertEquals(VramCompatibility.UNKNOWN, result)
    }

    @Test
    fun fits_boundary_exact_margin() {
        // File = 2048 MB, VRAM = 4096 MB, margin = 2048 MB
        // 2048 + 2048 = 4096 <= 4096 => FITS (exact boundary)
        val result = calculateVramCompatibility(
            modelFileSizeKB = 2_097_152.0, // 2 GB
            vramTotalMB = 4_096L,
        )
        assertEquals(VramCompatibility.FITS, result)
    }

    @Test
    fun tight_just_over_margin() {
        // File = 2049 MB, VRAM = 4096 MB
        // 2049 + 2048 = 4097 > 4096 => TIGHT (not FITS)
        // 2049 <= 4096 => TIGHT (not NEEDS_OFFLOADING)
        val result = calculateVramCompatibility(
            modelFileSizeKB = 2_098_176.0, // ~2049 MB
            vramTotalMB = 4_096L,
        )
        assertEquals(VramCompatibility.TIGHT, result)
    }

    @Test
    fun small_lora_fits_easily() {
        // 150 MB LoRA with 8 GB VRAM => FITS
        val result = calculateVramCompatibility(
            modelFileSizeKB = 153_600.0, // 150 MB
            vramTotalMB = 8_192L,
        )
        assertEquals(VramCompatibility.FITS, result)
    }
}
