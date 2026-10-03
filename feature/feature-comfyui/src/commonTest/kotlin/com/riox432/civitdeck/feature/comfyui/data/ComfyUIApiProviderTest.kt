package com.riox432.civitdeck.feature.comfyui.data

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.data.repository.mockClient
import com.riox432.civitdeck.feature.comfyui.data.repository.okJson
import com.riox432.civitdeck.feature.comfyui.data.repository.testJson
import io.ktor.http.Url
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ComfyUIApiProviderTest {

    private val pinA = "aa".repeat(32)
    private val pinB = "bb".repeat(32)

    private val sharedRequests = mutableListOf<Url>()
    private val pinnedRequests = mutableListOf<Url>()
    private val sharedClient = mockClient { sharedRequests += it.url; okJson(QUEUE_BODY) }
    private val pinnedClient = mockClient { pinnedRequests += it.url; okJson(QUEUE_BODY) }

    /** Pins passed to the pinned-client factory, in creation order. */
    private val createdPins = mutableListOf<String?>()

    private fun provider(vararg rows: ComfyUIConnectionEntity): ComfyUIApiProvider {
        val dao = FakeComfyUIConnectionDao().apply { this.rows.addAll(rows) }
        return ComfyUIApiProvider(dao, sharedClient, testJson) { trust ->
            createdPins += trust.expectedSha256
            pinnedClient
        }
    }

    private fun row(
        id: Long,
        hostname: String = "comfy.local",
        port: Int = 8443,
        isActive: Boolean = false,
        useHttps: Boolean = true,
        acceptSelfSigned: Boolean = true,
        pin: String? = pinA,
    ) = ComfyUIConnectionEntity(
        id = id,
        name = "c$id",
        hostname = hostname,
        port = port,
        isActive = isActive,
        createdAt = id,
        useHttps = useHttps,
        acceptSelfSigned = acceptSelfSigned,
        tlsCertSha256 = pin,
    )

    @Test
    fun forActive_pinned_row_yields_a_pinned_client_on_that_rows_url() = runTest {
        val endpoint = provider(row(id = 1, isActive = true)).forActive()

        endpoint.api.getQueue()

        assertEquals("https://comfy.local:8443", endpoint.baseUrl)
        assertEquals(listOf<String?>(pinA), createdPins)
        assertSame(pinnedClient, endpoint.httpClient)
        assertEquals("https://comfy.local:8443/queue", pinnedRequests.single().toString())
        assertTrue(sharedRequests.isEmpty())
    }

    @Test
    fun forActive_non_self_signed_rows_yield_the_shared_client() = runTest {
        val https = provider(row(id = 1, isActive = true, acceptSelfSigned = false)).forActive()
        // acceptSelfSigned without HTTPS has no certificate to pin.
        val http = provider(row(id = 1, isActive = true, useHttps = false, port = 8188)).forActive()

        assertSame(sharedClient, https.httpClient)
        assertSame(sharedClient, http.httpClient)
        assertEquals("http://comfy.local:8188", http.baseUrl)
        assertTrue(createdPins.isEmpty())
    }

    @Test
    fun forActive_unconfirmed_self_signed_row_yields_a_client_that_rejects_every_certificate() = runTest {
        provider(row(id = 1, isActive = true, pin = null)).forActive()

        assertEquals(listOf<String?>(null), createdPins)
    }

    @Test
    fun forActive_throws_without_an_active_row() = runTest {
        assertFailsWith<DomainException.ConnectionException> { provider(row(id = 1)).forActive() }
    }

    @Test
    fun pinned_clients_are_cached_per_pin() = runTest {
        val p = provider(row(id = 1, isActive = true))

        p.forActive()
        p.forActive()
        p.forConnection(ComfyUIConnection(name = "x", hostname = "other", useHttps = true, acceptSelfSigned = true, tlsCertSha256 = pinB))

        assertEquals(listOf<String?>(pinA, pinB), createdPins)
    }

    @Test
    fun forConnection_binds_the_passed_connection() {
        val endpoint = provider().forConnection(
            ComfyUIConnection(name = "x", hostname = "pc", port = 9000, useHttps = true, acceptSelfSigned = true, tlsCertSha256 = pinB),
        )

        assertEquals("https://pc:9000", endpoint.baseUrl)
        assertSame(pinnedClient, endpoint.httpClient)
        assertEquals(listOf<String?>(pinB), createdPins)
    }

    @Test
    fun forUrl_uses_the_pin_of_the_matching_row_and_system_trust_otherwise() = runTest {
        val p = provider(row(id = 1))

        val pinned = p.forUrl("https://COMFY.local:8443/view?filename=a.png&type=output")
        val otherPort = p.forUrl("https://comfy.local:8444/view?filename=a.png")
        val plain = p.forUrl("http://comfy.local:8443/view?filename=a.png")
        val civitai = p.forUrl("https://image.civitai.com/x.png")

        assertSame(pinnedClient, pinned.httpClient)
        assertSame(sharedClient, otherPort.httpClient)
        assertSame(sharedClient, plain.httpClient)
        assertSame(sharedClient, civitai.httpClient)
        assertEquals("https://image.civitai.com:443", civitai.baseUrl)
    }

    @Test
    fun forUrl_keeps_an_active_unconfirmed_row_from_falling_back_to_system_trust() = runTest {
        val endpoint = provider(row(id = 1, isActive = true, pin = null), row(id = 2, pin = pinA))
            .forUrl("https://comfy.local:8443/view?filename=a.png")

        assertSame(pinnedClient, endpoint.httpClient)
        assertEquals(listOf<String?>(null), createdPins)
    }

    @Test
    fun snapshot_returns_the_pin_for_a_pinned_host_and_nothing_for_civitai() = runTest {
        val p = provider(row(id = 1))
        p.startSnapshot(backgroundScope)
        runCurrent()

        assertEquals(pinA, p.pinnedSha256For("comfy.local", 8443))
        assertNull(p.pinnedSha256For("image.civitai.com", 443))
    }

    @Test
    fun snapshot_returns_nothing_when_rows_on_one_host_port_disagree_and_none_is_active() = runTest {
        val p = provider(row(id = 1, pin = pinA), row(id = 2, pin = pinB))
        p.startSnapshot(backgroundScope)
        runCurrent()

        assertNull(p.pinnedSha256For("comfy.local", 8443))
    }

    @Test
    fun snapshot_prefers_the_active_row_and_ignores_rows_without_a_pin() = runTest {
        val activeWins = provider(row(id = 1, pin = pinA), row(id = 2, pin = pinB, isActive = true))
        val agreeing = provider(row(id = 1, pin = pinA), row(id = 2, pin = null), row(id = 3, acceptSelfSigned = false))
        activeWins.startSnapshot(backgroundScope)
        agreeing.startSnapshot(backgroundScope)
        runCurrent()

        assertEquals(pinB, activeWins.pinnedSha256For("comfy.local", 8443))
        assertEquals(pinA, agreeing.pinnedSha256For("comfy.local", 8443))
    }

    @Test
    fun snapshot_is_empty_before_it_is_started() {
        assertNull(provider(row(id = 1)).pinnedSha256For("comfy.local", 8443))
    }

    private companion object {
        const val QUEUE_BODY = """{"queue_running":[],"queue_pending":[]}"""
    }
}
