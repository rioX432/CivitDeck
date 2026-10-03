package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Covers [ComfyUIConnectionTesterImpl]: only a 2xx `/queue` response with ComfyUI's queue shape
 * counts as connected; other servers answering on the port must not. A loopback host that gets no
 * HTTP response is reported as [ConnectionFailureCause.LoopbackHost].
 */
class ComfyUIConnectionTesterImplTest {

    private val connection = ComfyUIConnection(name = "Home", hostname = "192.168.0.10")
    private val loopbackConnection = ComfyUIConnection(name = "Home", hostname = "127.0.0.1")
    private val lanConnection = ComfyUIConnection(name = "Home", hostname = "192.168.1.5")

    private fun tester(status: HttpStatusCode, body: String, contentType: String = "application/json") =
        mockClient { request ->
            if (request.url.encodedPath.endsWith("/queue")) {
                respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, contentType))
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }.let { client -> ComfyUIConnectionTesterImpl(client, client, testJson) }

    private fun throwingTester(error: Throwable) =
        mockClient { throw error }.let { client -> ComfyUIConnectionTesterImpl(client, client, testJson) }

    private fun connectionTo(hostname: String) = ComfyUIConnection(name = "Home", hostname = hostname)

    @Test
    fun test_returns_success_for_comfyui_queue_shape() = runTest {
        val result = tester(HttpStatusCode.OK, """{"queue_running":[],"queue_pending":[]}""").test(connection)

        assertIs<ConnectionTestResult.Success>(result)
    }

    @Test
    fun test_returns_auth_required_for_401_with_json_body() = runTest {
        val result = tester(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.AuthRequired, 401), result)
    }

    @Test
    fun test_returns_auth_required_for_403() = runTest {
        val result = tester(HttpStatusCode.Forbidden, "Forbidden", "text/plain").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.AuthRequired, 403), result)
    }

    @Test
    fun test_returns_not_comfyui_for_2xx_json_without_queue_running() = runTest {
        val result = tester(HttpStatusCode.OK, """{"status":"ok"}""").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.NotComfyUI), result)
    }

    @Test
    fun test_returns_not_comfyui_for_2xx_html_page() = runTest {
        val result = tester(HttpStatusCode.OK, "<html><body>Login</body></html>", "text/html").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.NotComfyUI), result)
    }

    @Test
    fun test_returns_http_failure_for_500() = runTest {
        val result = tester(HttpStatusCode.InternalServerError, """{"error":"boom"}""").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Http, 500), result)
    }

    @Test
    fun test_returns_http_failure_for_404_plain_text() = runTest {
        val result = tester(HttpStatusCode.NotFound, "Not Found", "text/plain").test(connection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Http, 404), result)
    }

    @Test
    fun test_returns_loopback_host_for_unreachable_loopback_hosts() = runTest {
        for (host in listOf("localhost", "LocalHost", "127.0.0.1", "127.1.2.3", "0.0.0.0")) {
            val result = throwingTester(IOException("Connection refused")).test(connectionTo(host))

            assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.LoopbackHost), result, host)
        }
    }

    @Test
    fun test_returns_loopback_host_for_loopback_timeout() = runTest {
        val result = throwingTester(ConnectTimeoutException("timed out")).test(loopbackConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.LoopbackHost), result)
    }

    @Test
    fun test_does_not_return_loopback_host_for_non_loopback_hosts() = runTest {
        for (host in listOf("192.168.1.5", "127.example.com", "10.0.0.127")) {
            val result = throwingTester(IOException("Connection refused")).test(connectionTo(host))

            assertIs<ConnectionTestResult.Failure>(result)
            assertNotEquals(ConnectionFailureCause.LoopbackHost, result.cause, host)
        }
    }

    @Test
    fun test_returns_refused_for_jvm_connection_refused() = runTest {
        val result = throwingTester(IOException("Connection refused")).test(lanConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Refused), result)
    }

    @Test
    fun test_returns_refused_for_okhttp_econnrefused_in_cause() = runTest {
        val cause = IOException(
            "failed to connect to /192.168.1.5 (port 8188) after 10000ms: isConnected failed: " +
                "ECONNREFUSED (Connection refused)",
        )
        val error = IOException("Failed to connect to /192.168.1.5:8188", cause)

        val result = throwingTester(error).test(lanConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Refused), result)
    }

    @Test
    fun test_returns_refused_for_darwin_cannot_connect_to_host() = runTest {
        val error = IOException(
            "Exception in http request: Error Domain=NSURLErrorDomain Code=-1004 \"Could not connect to the server.\"",
        )

        val result = throwingTester(error).test(lanConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Refused), result)
    }

    @Test
    fun test_returns_unreachable_for_dns_failure() = runTest {
        val result = throwingTester(IOException("Unable to resolve host")).test(lanConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.Unreachable), result)
    }

    @Test
    fun test_keeps_auth_required_for_401_from_loopback_host() = runTest {
        val result = tester(HttpStatusCode.Unauthorized, """{"error":"unauthorized"}""").test(loopbackConnection)

        assertEquals(ConnectionTestResult.Failure(ConnectionFailureCause.AuthRequired, 401), result)
    }

    @Test
    fun test_returns_success_for_comfyui_queue_shape_from_loopback_host() = runTest {
        val result = tester(HttpStatusCode.OK, """{"queue_running":[],"queue_pending":[]}""").test(loopbackConnection)

        assertIs<ConnectionTestResult.Success>(result)
    }
}
