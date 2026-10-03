package com.riox432.civitdeck.data.api.comfyui

import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TrustOnlyPinnedLeafTest {

    @Test
    fun clientTrustsOnlyThePinAndSkipsHostnameCheck() {
        val client = OkHttpClient.Builder()
            .trustOnlyPinnedLeaf(ComfyUIServerTrust.PinnedLeaf(expectedSha256 = null))
            .build()

        val trustManager = assertIs<PinnedLeafTrustManager>(client.x509TrustManager)
        assertFailsWith<CertificateException> { trustManager.checkServerTrusted(emptyArray(), "ECDHE_ECDSA") }
        assertTrue(client.hostnameVerifier.verify("not-in-the-certificate.example", null))
    }
}
