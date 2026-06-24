package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Verifies [ParseConnectionUrlUseCase] extracts host/port/scheme from QR or manual strings,
 * applies scheme-aware default ports, and rejects malformed input.
 */
class ParseConnectionUrlUseCaseTest {

    private val useCase = ParseConnectionUrlUseCase()

    @Test
    fun parsesHttpUrlWithExplicitPort() {
        val conn = useCase("http://192.168.1.20:8188")

        assertEquals("192.168.1.20", conn?.hostname)
        assertEquals(8188, conn?.port)
        assertEquals(false, conn?.useHttps)
    }

    @Test
    fun parsesHttpsUrlAndDefaultsToPort443() {
        val conn = useCase("https://comfy.example.com")

        assertEquals("comfy.example.com", conn?.hostname)
        assertEquals(443, conn?.port)
        assertEquals(true, conn?.useHttps)
    }

    @Test
    fun bareHostDefaultsToComfyuiPort() {
        val conn = useCase("192.168.1.20")

        assertEquals("192.168.1.20", conn?.hostname)
        assertEquals(ComfyUIConnection.DEFAULT_COMFYUI_PORT, conn?.port)
        assertEquals(false, conn?.useHttps)
    }

    @Test
    fun stripsPathAndQueryKeepingAuthorityOnly() {
        val conn = useCase("http://host:8188/prompt?foo=bar")

        assertEquals("host", conn?.hostname)
        assertEquals(8188, conn?.port)
    }

    @Test
    fun returnsNullForBlankInput() {
        assertNull(useCase("   "))
    }

    @Test
    fun returnsNullForOutOfRangePort() {
        // 70000 exceeds the valid 1..65535 range.
        assertNull(useCase("host:70000"))
    }

    @Test
    fun returnsNullForNonNumericPort() {
        assertNull(useCase("host:abc"))
    }

    @Test
    fun usesHostAsConnectionName() {
        val conn = useCase("http://10.0.0.5:8188")

        assertEquals("10.0.0.5", conn?.name)
    }
}
