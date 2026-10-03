package com.riox432.civitdeck.feature.comfyui.data

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.data.repository.mockClient
import com.riox432.civitdeck.feature.comfyui.data.repository.okJson
import com.riox432.civitdeck.feature.comfyui.data.repository.testJson
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComfyUIApiProviderPinMapTest {

    private val pinA = "aa".repeat(32)
    private val pinB = "bb".repeat(32)
    private val dao = FakeComfyUIConnectionDao()
    private val client = mockClient { okJson("{}") }
    private val provider = ComfyUIApiProvider(dao, client, testJson) { client }

    private fun row(
        id: Long,
        hostname: String = "Comfy.local",
        isActive: Boolean = false,
        acceptSelfSigned: Boolean = true,
        pin: String? = pinA,
    ) = ComfyUIConnectionEntity(
        id = id,
        name = "c$id",
        hostname = hostname,
        port = 8443,
        isActive = isActive,
        createdAt = id,
        useHttps = true,
        acceptSelfSigned = acceptSelfSigned,
        tlsCertSha256 = pin,
    )

    @Test
    fun emits_nothing_until_the_snapshot_is_loaded_then_the_lowercased_host_port_pins() = runTest {
        dao.rows += listOf(row(id = 1), row(id = 2, hostname = "plain.local", acceptSelfSigned = false))
        val emissions = mutableListOf<Map<String, String>>()
        backgroundScope.launch { provider.pinnedSha256ByHostPort.collect { emissions += it } }
        runCurrent()
        assertTrue(emissions.isEmpty())

        provider.startSnapshot(backgroundScope)
        runCurrent()

        assertEquals(listOf(mapOf("comfy.local:8443" to pinA)), emissions)
    }

    @Test
    fun emits_again_only_when_the_effective_pin_changes() = runTest {
        dao.rows += listOf(row(id = 1, pin = pinA), row(id = 2, pin = pinB, isActive = true))
        val emissions = mutableListOf<Map<String, String>>()
        provider.startSnapshot(backgroundScope)
        backgroundScope.launch { provider.pinnedSha256ByHostPort.collect { emissions += it } }
        runCurrent()

        dao.updateTestResult(id = 2, testedAt = 1, success = true)
        runCurrent()
        // The active row decides its host:port, so switching rows changes the pin in effect.
        dao.deactivateAll()
        dao.activate(id = 1)
        runCurrent()
        dao.update(dao.rows.first { it.id == 1L }.copy(tlsCertSha256 = null))
        runCurrent()

        assertEquals(
            listOf(mapOf("comfy.local:8443" to pinB), mapOf("comfy.local:8443" to pinA), emptyMap()),
            emissions,
        )
    }
}
