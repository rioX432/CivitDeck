package com.riox432.civitdeck.data.api.comfyui

import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PinnedLeafTrustManagerTest {

    @Test
    fun matchingPinIsAcceptedAndRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = TEST_CERT_SHA256)

        PinnedLeafTrustManager(trust).checkServerTrusted(arrayOf(testCertificate()), AUTH_TYPE)

        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun mismatchingPinIsRejectedAndPresentedFingerprintRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = OTHER_SHA256)

        assertFailsWith<CertificateException> {
            PinnedLeafTrustManager(trust).checkServerTrusted(arrayOf(testCertificate()), AUTH_TYPE)
        }

        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun missingPinIsRejectedAndPresentedFingerprintRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = null)

        assertFailsWith<CertificateException> {
            PinnedLeafTrustManager(trust).checkServerTrusted(arrayOf(testCertificate()), AUTH_TYPE)
        }

        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun emptyChainIsRejectedAndClearsEarlierFingerprint() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = TEST_CERT_SHA256)
        val trustManager = PinnedLeafTrustManager(trust)
        trustManager.checkServerTrusted(arrayOf(testCertificate()), AUTH_TYPE)

        assertFailsWith<CertificateException> {
            trustManager.checkServerTrusted(emptyArray(), AUTH_TYPE)
        }

        assertNull(trust.presentedSha256)
    }

    private fun testCertificate(): X509Certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(TEST_CERT_PEM.byteInputStream()) as X509Certificate

    private companion object {
        const val AUTH_TYPE = "ECDHE_ECDSA"

        // Self-signed P-256 certificate for CN=comfyui.test.
        val TEST_CERT_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIBgzCCASmgAwIBAgIUPi9pwigjF/j4v99qwt1wkXJ3nd0wCgYIKoZIzj0EAwIw
            FzEVMBMGA1UEAwwMY29tZnl1aS50ZXN0MB4XDTI2MTAwMzA2NDI1MloXDTM2MDkz
            MDA2NDI1MlowFzEVMBMGA1UEAwwMY29tZnl1aS50ZXN0MFkwEwYHKoZIzj0CAQYI
            KoZIzj0DAQcDQgAErNO8mb/3b4/BBKNgSTPwZns2laQI3HLoEbtzx5X6gwSjnRZ6
            s5JERYCMUwPYIfaK/AFtqTlTGSQiunvnvy2gaqNTMFEwHQYDVR0OBBYEFFqh/3r5
            9fVrZw9Prv/ZMT2iT9cuMB8GA1UdIwQYMBaAFFqh/3r59fVrZw9Prv/ZMT2iT9cu
            MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIhAITU3qfhsNkz22Da
            /bzBVhbngmuYudMlgbX2oxTyGG5wAiBmlfHEik+/W7OeG/LPpMQMHvS+im2XuiW7
            0qJByehzSw==
            -----END CERTIFICATE-----
        """.trimIndent()

        // `openssl x509 -noout -fingerprint -sha256` of TEST_CERT_PEM, lowercased without colons.
        const val TEST_CERT_SHA256 = "ac9394eeb2c08c4faebc4a1206d021318c79d07bf7e928f545c579726b86c30c"

        const val OTHER_SHA256 = "0000000000000000000000000000000000000000000000000000000000000000"
    }
}
