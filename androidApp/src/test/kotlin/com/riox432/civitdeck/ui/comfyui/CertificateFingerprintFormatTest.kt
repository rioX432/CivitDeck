package com.riox432.civitdeck.ui.comfyui

import kotlin.test.Test
import kotlin.test.assertEquals

class CertificateFingerprintFormatTest {

    @Test
    fun fingerprintMatchesOpensslOutputForTheSameCertificate() {
        // Pair taken from one self-signed certificate: `shasum -a 256` of its DER encoding (the
        // form the trust manager records) and `openssl x509 -noout -fingerprint -sha256` (OpenSSL 3.6).
        val presented = "63ebc75128b2d54403ff4b648c28dbabc504117f6c3f5098b02befbd2145b123"
        val openssl = "63:EB:C7:51:28:B2:D5:44:03:FF:4B:64:8C:28:DB:AB:" +
            "C5:04:11:7F:6C:3F:50:98:B0:2B:EF:BD:21:45:B1:23"

        assertEquals(openssl, formatSha256Fingerprint(presented))
    }

    @Test
    fun fingerprintHas32ColonSeparatedBytes() {
        val formatted = formatSha256Fingerprint("ab".repeat(32))

        assertEquals(95, formatted.length)
        assertEquals(32, formatted.split(":").size)
    }
}
