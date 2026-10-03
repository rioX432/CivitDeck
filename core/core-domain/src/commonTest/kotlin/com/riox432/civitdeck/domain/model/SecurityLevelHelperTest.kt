package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityLevelHelperTest {

    private fun level(
        hostname: String,
        useHttps: Boolean = false,
        acceptSelfSigned: Boolean = false,
    ) = SecurityLevelHelper.getSecurityLevel(
        ComfyUIConnection(
            name = "test",
            hostname = hostname,
            useHttps = useHttps,
            acceptSelfSigned = acceptSelfSigned,
        ),
    )

    @Test
    fun http_to_tailscale_ipv4_range_is_local() {
        listOf("100.64.0.1", "100.101.102.103", "100.127.255.254").forEach { host ->
            assertEquals(ConnectionSecurityLevel.LocalInsecure, level(host), host)
        }
    }

    @Test
    fun http_to_magicdns_name_is_local_regardless_of_case() {
        listOf("pc.tail1234.ts.net", "PC.TAIL1234.TS.NET").forEach { host ->
            assertEquals(ConnectionSecurityLevel.LocalInsecure, level(host), host)
        }
    }

    @Test
    fun http_to_rfc1918_address_is_local() {
        assertEquals(ConnectionSecurityLevel.LocalInsecure, level("192.168.1.5"))
    }

    @Test
    fun http_just_outside_tailscale_range_is_remote() {
        listOf("100.63.0.1", "100.128.0.1").forEach { host ->
            assertEquals(ConnectionSecurityLevel.RemoteInsecure, level(host), host)
        }
    }

    @Test
    fun http_to_lookalike_or_public_domain_is_remote() {
        listOf("evil-ts.net", "example.com").forEach { host ->
            assertEquals(ConnectionSecurityLevel.RemoteInsecure, level(host), host)
        }
    }

    @Test
    fun https_with_trusted_certificate_is_secure() {
        assertEquals(ConnectionSecurityLevel.Secure, level("pc.tail1234.ts.net", useHttps = true))
        assertEquals(ConnectionSecurityLevel.Secure, level("example.com", useHttps = true))
    }

    @Test
    fun https_accepting_self_signed_certificate_is_self_signed() {
        assertEquals(
            ConnectionSecurityLevel.SelfSigned,
            level("100.101.102.103", useHttps = true, acceptSelfSigned = true),
        )
        assertEquals(
            ConnectionSecurityLevel.SelfSigned,
            level("example.com", useHttps = true, acceptSelfSigned = true),
        )
    }
}
