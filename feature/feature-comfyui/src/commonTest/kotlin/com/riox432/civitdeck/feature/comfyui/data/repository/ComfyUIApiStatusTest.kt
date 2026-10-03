package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIResponseException
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Lives in feature-comfyui rather than core-network because [ComfyUIApi] logs through `Logger`,
 * which core-network's Android host tests do not mock.
 */
class ComfyUIApiStatusTest {

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) = ComfyUIApi(mockClient(handler), testJson).apply { setBaseUrl("http://h:8188") }

    @Test
    fun getQueue_throws_on_an_unauthorized_json_body_instead_of_returning_an_empty_queue() = runTest {
        // QueueResponse defaults every field, so this proxy body would otherwise decode as an empty queue.
        val api = api {
            respond(ByteReadChannel("""{"detail":"Unauthorized"}"""), HttpStatusCode.Unauthorized, jsonHeaders)
        }

        val e = assertFailsWith<ComfyUIResponseException> { api.getQueue() }

        assertEquals(HttpStatusCode.Unauthorized, e.response.status)
        assertEquals("HTTP 401", e.message)
    }

    @Test
    fun interrupt_and_deleteQueue_throw_on_a_server_error() = runTest {
        val api = api { respondError(HttpStatusCode.InternalServerError) }

        assertFailsWith<ResponseException> { api.interrupt() }
        assertFailsWith<ResponseException> { api.deleteQueue(listOf("p1")) }
    }
}
