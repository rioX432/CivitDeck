package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers [ComfyUIConnectionTesterImpl]: only a 2xx `/queue` response with ComfyUI's queue shape
 * counts as connected; other servers answering on the port must not.
 */
class ComfyUIConnectionTesterImplTest {

    private val connection = ComfyUIConnection(name = "Home", hostname = "192.168.0.10")

    private fun tester(status: HttpStatusCode, body: String, contentType: String = "application/json") =
        mockClient { request ->
            if (request.url.encodedPath.endsWith("/queue")) {
                respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, contentType))
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }.let { client -> ComfyUIConnectionTesterImpl(client, client, testJson) }

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
}
