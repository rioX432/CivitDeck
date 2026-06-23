package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.feature.comfyui.data.repository.mockClient
import com.riox432.civitdeck.feature.comfyui.data.repository.okJson
import com.riox432.civitdeck.feature.comfyui.data.repository.testJson
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers [FetchSystemStatsUseCase]: it maps the /system_stats response into a
 * [com.riox432.civitdeck.domain.model.SystemStats] domain model (first device,
 * bytes -> MB), substitutes defaults when no device is present, and returns null
 * when the request fails.
 */
class FetchSystemStatsUseCaseTest {

    private fun api(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): ComfyUIApi = ComfyUIApi(mockClient(handler), testJson).apply { setBaseUrl("http://localhost:8188") }

    @Test
    fun maps_first_device_and_converts_bytes_to_megabytes() = runTest {
        // 1 device with 8 GiB total / 4 GiB free VRAM; system has 16 GiB / 8 GiB RAM.
        val body = """
            {
              "system": {
                "os": "Linux",
                "ram_total": 17179869184,
                "ram_free": 8589934592,
                "comfyui_version": "0.2.0",
                "pytorch_version": "2.3.0"
              },
              "devices": [
                {"name": "RTX 4090", "type": "cuda", "vram_total": 8589934592, "vram_free": 4294967296, "index": 0},
                {"name": "RTX 3060", "type": "cuda", "vram_total": 1, "vram_free": 1, "index": 1}
              ]
            }
        """.trimIndent()
        val useCase = FetchSystemStatsUseCase(api { okJson(body) })

        val stats = useCase()

        requireNotNull(stats)
        // Only the FIRST device is used.
        assertEquals("RTX 4090", stats.gpuName)
        assertEquals("cuda", stats.gpuType)
        assertEquals(8192L, stats.vramTotalMB)
        assertEquals(4096L, stats.vramFreeMB)
        assertEquals(16384L, stats.ramTotalMB)
        assertEquals(8192L, stats.ramFreeMB)
        assertEquals("Linux", stats.os)
        assertEquals("0.2.0", stats.comfyuiVersion)
        assertEquals("2.3.0", stats.pytorchVersion)
    }

    @Test
    fun substitutes_unknown_and_zero_when_no_devices_present() = runTest {
        val body = """
            {"system": {"os": "Windows", "ram_total": 1048576, "ram_free": 0}, "devices": []}
        """.trimIndent()
        val useCase = FetchSystemStatsUseCase(api { okJson(body) })

        val stats = useCase()

        requireNotNull(stats)
        assertEquals("Unknown", stats.gpuName)
        assertEquals("Unknown", stats.gpuType)
        assertEquals(0L, stats.vramTotalMB)
        assertEquals(0L, stats.vramFreeMB)
        assertEquals(1L, stats.ramTotalMB)
    }

    @Test
    fun returns_null_when_request_fails() = runTest {
        val useCase = FetchSystemStatsUseCase(
            api { respondError(HttpStatusCode.NotFound) },
        )

        assertNull(useCase())
    }
}
