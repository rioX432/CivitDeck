package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIServerTrust
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

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
        }.let { client -> ComfyUIConnectionTesterImpl(client, { client }, testJson) }

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
    fun test_uses_a_one_off_pinned_client_with_the_stored_pin_for_self_signed_https() = runTest {
        val trusts = mutableListOf<ComfyUIServerTrust.PinnedLeaf>()
        val pinnedClients = mutableListOf<HttpClient>()
        val tester = ComfyUIConnectionTesterImpl(
            normalClient = mockClient { respondError(HttpStatusCode.InternalServerError) },
            createPinnedClient = { trust ->
                trusts += trust
                mockClient { okJson(QUEUE_BODY) }.also { pinnedClients += it }
            },
            json = testJson,
        )
        val pinned = connection.copy(useHttps = true, acceptSelfSigned = true, tlsCertSha256 = PIN)

        assertIs<ConnectionTestResult.Success>(tester.test(pinned))
        assertIs<ConnectionTestResult.Success>(tester.test(pinned))

        assertEquals(listOf(PIN, PIN), trusts.map { it.expectedSha256 })
        assertNotSame(trusts[0], trusts[1])
        assertTrue(pinnedClients.none { it.isActive })
    }

    @Test
    fun test_uses_the_normal_client_unless_both_https_and_self_signed_are_on() = runTest {
        var pinnedClientsCreated = 0
        val tester = ComfyUIConnectionTesterImpl(
            normalClient = mockClient { okJson(QUEUE_BODY) },
            createPinnedClient = {
                pinnedClientsCreated++
                mockClient { respondError(HttpStatusCode.InternalServerError) }
            },
            json = testJson,
        )

        assertIs<ConnectionTestResult.Success>(tester.test(connection.copy(acceptSelfSigned = true)))
        assertIs<ConnectionTestResult.Success>(tester.test(connection.copy(useHttps = true)))
        assertEquals(0, pinnedClientsCreated)
    }

    @Test
    fun pin_failure_cause_reports_unconfirmed_when_no_pin_is_stored() {
        assertEquals(ConnectionFailureCause.CertificateUnconfirmed, pinFailureCause(expected = null, presented = PIN))
    }

    @Test
    fun pin_failure_cause_reports_changed_when_the_presented_certificate_differs() {
        assertEquals(ConnectionFailureCause.CertificateChanged, pinFailureCause(expected = PIN, presented = OTHER_PIN))
    }

    @Test
    fun pin_failure_cause_stays_tls_without_a_presented_certificate_or_on_a_match() {
        assertEquals(ConnectionFailureCause.Tls, pinFailureCause(expected = null, presented = null))
        assertEquals(ConnectionFailureCause.Tls, pinFailureCause(expected = PIN, presented = PIN))
    }

    private companion object {
        const val QUEUE_BODY = """{"queue_running":[],"queue_pending":[]}"""
        val PIN = "a".repeat(64)
        val OTHER_PIN = "b".repeat(64)
    }
}
