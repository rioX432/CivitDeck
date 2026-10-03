package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIServerTrust
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionTester
import com.riox432.civitdeck.feature.comfyui.domain.usecase.fetchSystemStats
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

private const val TAG = "ComfyUIConnectionTester"

private val AUTH_REQUIRED_STATUSES = setOf(HttpStatusCode.Unauthorized.value, HttpStatusCode.Forbidden.value)

// Numeric dotted quads only, so a DNS name such as `127.example.com` is not treated as loopback.
private val IPV4_LOOPBACK = Regex("""^127\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
private const val IPV4_UNSPECIFIED = "0.0.0.0"
private const val LOCALHOST = "localhost"

/**
 * Tests a connection on a transient [ComfyUIApi]. A fresh [ComfyUIApi] per test avoids the
 * mutable base-URL state of the singleton [ComfyUIApi], so concurrent tests/probes do not race.
 *
 * A connection that accepts self-signed certificates over HTTPS is tested on a one-off client
 * built by [createPinnedClient] with the connection's stored pin. That client both enforces the
 * pin and records the fingerprint the server presented; the recorded value is per-client state,
 * so each test builds and closes its own client.
 */
class ComfyUIConnectionTesterImpl(
    private val normalClient: HttpClient,
    private val createPinnedClient: (ComfyUIServerTrust.PinnedLeaf) -> HttpClient,
    private val json: Json,
) : ComfyUIConnectionTester {

    override suspend fun test(connection: ComfyUIConnection): ConnectionTestResult {
        if (!(connection.useHttps && connection.acceptSelfSigned)) {
            return probe(connection, normalClient, trust = null)
        }
        val trust = ComfyUIServerTrust.PinnedLeaf(connection.tlsCertSha256)
        val client = createPinnedClient(trust)
        return try {
            probe(connection, client, trust)
        } finally {
            client.close()
        }
    }

    private suspend fun probe(
        connection: ComfyUIConnection,
        client: HttpClient,
        trust: ComfyUIServerTrust.PinnedLeaf?,
    ): ConnectionTestResult {
        val api = ComfyUIApi(client, json)
        api.setBaseUrl(connection.baseUrl)
        return try {
            val probe = api.probeQueue()
            when {
                probe.status in AUTH_REQUIRED_STATUSES ->
                    failure(connection, ConnectionFailureCause.AuthRequired, "HTTP ${probe.status}", probe.status)
                !probe.isSuccessStatus ->
                    failure(connection, ConnectionFailureCause.Http, "HTTP ${probe.status}", probe.status)
                // Something other than ComfyUI answered with 2xx (e.g. another JSON service on the port).
                !probe.hasQueueRunning ->
                    failure(connection, ConnectionFailureCause.NotComfyUI, "/queue response lacks queue_running")
                // Health check passed; fetch optional stats (best-effort, never fails the test).
                else -> ConnectionTestResult.Success(fetchStats(api))
            }
        } catch (e: ConnectTimeoutException) {
            noResponseFailure(connection, ConnectionFailureCause.Timeout, e.message)
        } catch (e: HttpRequestTimeoutException) {
            noResponseFailure(connection, ConnectionFailureCause.Timeout, e.message)
        } catch (e: SocketTimeoutException) {
            noResponseFailure(connection, ConnectionFailureCause.Timeout, e.message)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            if (isTlsFailure(e) || isPinRejection(connection.tlsCertSha256, trust?.presentedSha256)) {
                tlsFailure(connection, trust, e.message)
            } else {
                noResponseFailure(connection, transportFailureCause(e), e.message)
            }
        }
    }

    private fun tlsFailure(
        connection: ComfyUIConnection,
        trust: ComfyUIServerTrust.PinnedLeaf?,
        detail: String?,
    ): ConnectionTestResult.Failure {
        val presented = trust?.presentedSha256
        val cause = pinFailureCause(expected = connection.tlsCertSha256, presented = presented)
        val fingerprint = presented.takeIf { cause != ConnectionFailureCause.Tls }
        return failure(connection, cause, detail, presentedSha256 = fingerprint)
    }

    // Reuses this test's client, which already enforces the pin under test, instead of
    // adding a client for that pin to ComfyUIApiProvider's cache.
    private suspend fun fetchStats(api: ComfyUIApi): SystemStats? = fetchSystemStats(api)

    /**
     * A loopback or unspecified host reaches this device rather than the ComfyUI machine (e.g. the
     * `http://0.0.0.0:8188` line ComfyUI logs with `--listen`), so that explains the failure better
     * than the transport error does. Loopback is still a valid target on Desktop, so the host is
     * classified only after a test fails without any HTTP response, never blocked up front.
     */
    private fun noResponseFailure(
        connection: ComfyUIConnection,
        cause: ConnectionFailureCause,
        detail: String?,
    ): ConnectionTestResult.Failure {
        val effectiveCause = if (isLoopbackHost(connection.hostname)) ConnectionFailureCause.LoopbackHost else cause
        return failure(connection, effectiveCause, detail)
    }

    private fun isLoopbackHost(hostname: String): Boolean {
        val host = hostname.trim()
        return host.equals(LOCALHOST, ignoreCase = true) || host == IPV4_UNSPECIFIED || IPV4_LOOPBACK.matches(host)
    }

    private fun failure(
        connection: ComfyUIConnection,
        cause: ConnectionFailureCause,
        detail: String?,
        httpStatus: Int? = null,
        presentedSha256: String? = null,
    ): ConnectionTestResult.Failure {
        Logger.w(TAG, "Test failed for ${connection.baseUrl}: $cause ($detail)")
        return ConnectionTestResult.Failure(cause, httpStatus, presentedSha256)
    }
}

/**
 * True when the pinned client saw a certificate other than the pin and so rejected the handshake.
 * Darwin reports that rejection as `NSURLErrorCancelled` (-999), whose message has nothing for
 * [isTlsFailure] to match; the JVM and Android engines already raise an `SSLException` for it.
 */
internal fun isPinRejection(expected: String?, presented: String?): Boolean =
    presented != null && presented != expected

/**
 * Classifies a TLS failure from a pinned client. [presented] is null when the handshake failed
 * before the certificate was checked, and a presented certificate equal to the pin means the
 * handshake failed for another reason; both stay a plain [ConnectionFailureCause.Tls].
 */
internal fun pinFailureCause(expected: String?, presented: String?): ConnectionFailureCause = when {
    presented == null -> ConnectionFailureCause.Tls
    expected == null -> ConnectionFailureCause.CertificateUnconfirmed
    presented != expected -> ConnectionFailureCause.CertificateChanged
    else -> ConnectionFailureCause.Tls
}
