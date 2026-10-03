@file:OptIn(ExperimentalForeignApi::class)

package com.riox432.civitdeck.data.api.comfyui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecPolicyCreateBasicX509
import platform.Security.SecTrustCreateWithCertificates
import platform.Security.SecTrustRef
import platform.Security.SecTrustRefVar
import platform.Security.errSecSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComfyUIServerTrustEvaluatorTest {

    @Test
    fun matchingPinIsAcceptedWithTrustCredentialAndRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = TEST_CERT_SHA256)

        val decision = ComfyUIServerTrustEvaluator(trust).evaluate(testServerTrust())

        assertEquals(NSURLSessionAuthChallengeUseCredential, decision.disposition)
        assertNotNull(decision.credential)
        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun mismatchingPinIsCancelledAndPresentedFingerprintRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = OTHER_SHA256)

        val decision = ComfyUIServerTrustEvaluator(trust).evaluate(testServerTrust())

        assertEquals(NSURLSessionAuthChallengeCancelAuthenticationChallenge, decision.disposition)
        assertNull(decision.credential)
        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun missingPinIsCancelledAndPresentedFingerprintRecorded() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = null)

        val decision = ComfyUIServerTrustEvaluator(trust).evaluate(testServerTrust())

        assertEquals(NSURLSessionAuthChallengeCancelAuthenticationChallenge, decision.disposition)
        assertEquals(TEST_CERT_SHA256, trust.presentedSha256)
    }

    @Test
    fun missingServerTrustIsCancelledAndClearsEarlierFingerprint() {
        val trust = ComfyUIServerTrust.PinnedLeaf(expectedSha256 = TEST_CERT_SHA256)
        val evaluator = ComfyUIServerTrustEvaluator(trust)
        evaluator.evaluate(testServerTrust())

        val decision = evaluator.evaluate(serverTrust = null)

        assertEquals(NSURLSessionAuthChallengeCancelAuthenticationChallenge, decision.disposition)
        assertNull(trust.presentedSha256)
    }

    private fun testServerTrust(): SecTrustRef {
        val der = assertNotNull(NSData.create(base64EncodedString = TEST_CERT_DER_BASE64, options = 0u))
        val derRef = CFBridgingRetain(der)
        val certificate = assertNotNull(SecCertificateCreateWithData(null, derRef?.reinterpret()))
        CFRelease(derRef)
        val policy = SecPolicyCreateBasicX509()
        // The trust retains the certificate and policy; the trust itself lives for the test run.
        val serverTrust = memScoped {
            val out = alloc<SecTrustRefVar>()
            val status = SecTrustCreateWithCertificates(certificate, policy, out.ptr)
            assertEquals(errSecSuccess, status)
            out.value
        }
        CFRelease(policy)
        CFRelease(certificate)
        return assertNotNull(serverTrust)
    }

    private companion object {
        // DER of the self-signed P-256 certificate for CN=comfyui.test used by
        // PinnedLeafTrustManagerTest on Android.
        const val TEST_CERT_DER_BASE64 =
            "MIIBgzCCASmgAwIBAgIUPi9pwigjF/j4v99qwt1wkXJ3nd0wCgYIKoZIzj0EAwIw" +
                "FzEVMBMGA1UEAwwMY29tZnl1aS50ZXN0MB4XDTI2MTAwMzA2NDI1MloXDTM2MDkz" +
                "MDA2NDI1MlowFzEVMBMGA1UEAwwMY29tZnl1aS50ZXN0MFkwEwYHKoZIzj0CAQYI" +
                "KoZIzj0DAQcDQgAErNO8mb/3b4/BBKNgSTPwZns2laQI3HLoEbtzx5X6gwSjnRZ6" +
                "s5JERYCMUwPYIfaK/AFtqTlTGSQiunvnvy2gaqNTMFEwHQYDVR0OBBYEFFqh/3r5" +
                "9fVrZw9Prv/ZMT2iT9cuMB8GA1UdIwQYMBaAFFqh/3r59fVrZw9Prv/ZMT2iT9cu" +
                "MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIhAITU3qfhsNkz22Da" +
                "/bzBVhbngmuYudMlgbX2oxTyGG5wAiBmlfHEik+/W7OeG/LPpMQMHvS+im2XuiW7" +
                "0qJByehzSw=="

        // `openssl x509 -noout -fingerprint -sha256` of the certificate, lowercased without colons.
        const val TEST_CERT_SHA256 = "ac9394eeb2c08c4faebc4a1206d021318c79d07bf7e928f545c579726b86c30c"

        const val OTHER_SHA256 = "0000000000000000000000000000000000000000000000000000000000000000"
    }
}
