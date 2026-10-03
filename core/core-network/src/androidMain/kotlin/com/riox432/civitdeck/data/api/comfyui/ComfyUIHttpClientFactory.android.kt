package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.data.api.TimeoutConfig
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun createComfyUIHttpClient(
    trust: ComfyUIServerTrust,
    timeoutConfig: TimeoutConfig,
): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            if (trust is ComfyUIServerTrust.PinnedLeaf) {
                config { trustOnlyPinnedLeaf(trust) }
            }
        }
        installComfyUIPlugins(timeoutConfig)
    }
}

/**
 * Makes the client accept only a server whose leaf certificate matches [trust]'s pin. The host
 * name is not checked, so a client built this way must only be sent to the host:port the pin
 * belongs to.
 */
fun OkHttpClient.Builder.trustOnlyPinnedLeaf(trust: ComfyUIServerTrust.PinnedLeaf): OkHttpClient.Builder {
    val trustManager = PinnedLeafTrustManager(trust)
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    }
    sslSocketFactory(sslContext.socketFactory, trustManager)
    // The pin identifies the server, so the certificate need not name the host the user typed
    // (a self-signed certificate rarely lists a LAN IP).
    hostnameVerifier { _, _ -> true }
    return this
}

/**
 * Accepts only a server whose leaf certificate matches [trust]'s pin, and records the presented
 * fingerprint on [trust] before deciding. The chain, issuer and validity dates are deliberately
 * not checked: a pinned self-signed certificate is trusted for exactly its own bytes.
 */
@Suppress("CustomX509TrustManager")
internal class PinnedLeafTrustManager(
    private val trust: ComfyUIServerTrust.PinnedLeaf,
) : X509TrustManager {

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        // Cleared first so a failure while encoding the certificate cannot leave the value of an
        // earlier handshake behind.
        trust.presentedSha256 = null
        val presented = chain?.firstOrNull()?.let { sha256Hex(it.encoded) }
        trust.presentedSha256 = presented
        if (presented == null) throw CertificateException("Server presented no certificate")
        val expected = trust.expectedSha256
            ?: throw CertificateException("No pinned certificate; server presented $presented")
        if (presented != expected) {
            throw CertificateException("Certificate $presented does not match pin $expected")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates are not accepted")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

private fun HttpClientConfig<*>.installComfyUIPlugins(timeoutConfig: TimeoutConfig) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            },
        )
    }
    install(HttpTimeout) {
        connectTimeoutMillis = timeoutConfig.connectTimeoutMs
        requestTimeoutMillis = timeoutConfig.requestTimeoutMs
        socketTimeoutMillis = timeoutConfig.socketTimeoutMs
    }
    install(Logging) { level = LogLevel.NONE }
    install(WebSockets)
    defaultRequest { header(HttpHeaders.Accept, "application/json") }
}
