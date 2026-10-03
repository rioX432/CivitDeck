package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiffusionModelFamilyTest {

    @Test
    fun base_model_matches_exactly_after_trimming_and_ignoring_case() {
        assertEquals(DiffusionModelFamily.KREA_2, DiffusionModelFamily.forBaseModel(" krea 2 "))
        assertEquals(DiffusionModelFamily.ANIMA, DiffusionModelFamily.forBaseModel("Anima"))
    }

    @Test
    fun unknown_partial_or_missing_base_model_has_no_family() {
        assertNull(DiffusionModelFamily.forBaseModel("SDXL 1.0"))
        assertNull(DiffusionModelFamily.forBaseModel("Krea 2 Turbo"))
        assertNull(DiffusionModelFamily.forBaseModel(""))
        assertNull(DiffusionModelFamily.forBaseModel(null))
    }

    @Test
    fun family_is_supported_only_when_the_server_lists_its_clip_type() {
        assertFalse(DiffusionModelFamily.KREA_2.isSupportedBy(listOf("stable_diffusion", "flux")))
        assertTrue(DiffusionModelFamily.KREA_2.isSupportedBy(listOf("stable_diffusion", "krea2")))
        assertTrue(DiffusionModelFamily.ANIMA.isSupportedBy(listOf("stable_diffusion")))
        assertFalse(DiffusionModelFamily.ANIMA.isSupportedBy(emptyList()))
    }
}
