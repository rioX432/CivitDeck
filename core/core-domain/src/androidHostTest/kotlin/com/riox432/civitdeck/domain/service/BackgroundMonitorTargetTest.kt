package com.riox432.civitdeck.domain.service

import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import com.riox432.civitdeck.domain.model.ComfyUIJobTarget
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarterImpl.Companion.EXTRA_BASE_URL
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarterImpl.Companion.EXTRA_PROMPT_ID
import com.riox432.civitdeck.domain.service.BackgroundMonitorStarterImpl.Companion.EXTRA_WS_SCHEME
import com.riox432.civitdeck.domain.service.MonitorJobExtras.Decoded
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class BackgroundMonitorTargetTest {

    private val pinnedA = ComfyUIJobHandle(
        target = ComfyUIJobTarget(
            connectionId = 41L,
            hostname = "Studio-PC.local",
            port = 8443,
            useHttps = true,
            acceptSelfSigned = true,
            tlsCertSha256 = "AB:CD:EF:01:23:45:67:89",
        ),
        promptId = "3f2a-prompt",
    )

    private val plainA = ComfyUIJobHandle(
        target = ComfyUIJobTarget(7L, "192.168.1.20", 8188, useHttps = false, acceptSelfSigned = false, null),
        promptId = "p1",
    )

    private fun roundTrip(job: ComfyUIJobHandle): Decoded =
        MonitorJobExtras.decode(MonitorJobExtras.read(MonitorJobExtras.encode(job)::get))

    @Test
    fun roundtrip_keeps_connection_id_prompt_id_original_host_and_tls_pin_exactly() {
        val decoded = assertIs<Decoded.Bound>(roundTrip(pinnedA))

        assertEquals(pinnedA, decoded.job)
        assertEquals(41L, decoded.job.target.connectionId)
        assertEquals("3f2a-prompt", decoded.job.promptId)
        assertEquals("Studio-PC.local", decoded.job.target.hostname)
        assertEquals("AB:CD:EF:01:23:45:67:89", decoded.job.target.tlsCertSha256)
    }

    @Test
    fun https_target_still_opens_wss_on_its_own_address_after_the_handoff() {
        val job = assertIs<Decoded.Bound>(roundTrip(pinnedA)).job

        assertEquals("https://Studio-PC.local:8443", job.target.baseUrl)
        assertEquals("wss", job.target.wsScheme)
    }

    @Test
    fun target_without_pin_sends_no_pin_and_comes_back_without_one() {
        val extras = MonitorJobExtras.encode(plainA)

        assertFalse(MonitorJobExtras.EXTRA_TLS_CERT_SHA256 in extras)
        val job = assertIs<Decoded.Bound>(roundTrip(plainA)).job
        assertEquals(plainA, job)
        assertEquals("ws", job.target.wsScheme)
    }

    @Test
    fun prompt_id_and_base_url_only_is_the_legacy_start() {
        val legacy = mapOf(EXTRA_PROMPT_ID to "p1", EXTRA_BASE_URL to "http://a:8188", EXTRA_WS_SCHEME to "ws")

        assertEquals(Decoded.Legacy, MonitorJobExtras.decode(legacy))
    }

    @Test
    fun bound_payload_missing_any_required_field_is_malformed() {
        val full = MonitorJobExtras.encode(pinnedA)
        val required = full.keys - MonitorJobExtras.EXTRA_TLS_CERT_SHA256

        required.forEach { key ->
            assertIs<Decoded.Malformed>(MonitorJobExtras.decode(full - key), "without $key")
        }
    }

    @Test
    fun pin_alone_is_still_a_bound_payload_and_never_falls_back_to_legacy() {
        val pinOnly = mapOf(EXTRA_PROMPT_ID to "p1", MonitorJobExtras.EXTRA_TLS_CERT_SHA256 to "AB:CD")

        assertIs<Decoded.Malformed>(MonitorJobExtras.decode(pinOnly))
    }

    @Test
    fun bound_payload_with_invalid_values_is_malformed() {
        val full = MonitorJobExtras.encode(pinnedA)
        val invalid = listOf(
            MonitorJobExtras.EXTRA_CONNECTION_ID to "not-a-number",
            MonitorJobExtras.EXTRA_HOSTNAME to " ",
            MonitorJobExtras.EXTRA_PORT to "0",
            MonitorJobExtras.EXTRA_PORT to "65536",
            MonitorJobExtras.EXTRA_PORT to "8188x",
            MonitorJobExtras.EXTRA_USE_HTTPS to "yes",
            MonitorJobExtras.EXTRA_ACCEPT_SELF_SIGNED to "1",
            MonitorJobExtras.EXTRA_TLS_CERT_SHA256 to "",
            EXTRA_PROMPT_ID to "",
        )

        invalid.forEach { (key, value) ->
            assertIs<Decoded.Malformed>(MonitorJobExtras.decode(full + (key to value)), "$key=$value")
        }
    }

    @Test
    fun read_takes_only_the_keys_the_codec_knows() {
        val source = MonitorJobExtras.encode(pinnedA) + ("unrelated_extra" to "x")

        val read = MonitorJobExtras.read(source::get)

        assertEquals(MonitorJobExtras.encode(pinnedA), read)
    }
}
