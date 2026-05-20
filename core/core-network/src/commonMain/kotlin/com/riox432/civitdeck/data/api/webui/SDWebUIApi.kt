package com.riox432.civitdeck.data.api.webui

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException
import kotlin.concurrent.Volatile

class SDWebUIApi(private val client: HttpClient) {
    @Volatile
    private var baseUrl: String = ""

    fun setBaseUrl(hostname: String, port: Int) {
        baseUrl = "http://$hostname:$port"
    }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getModels(): List<SDWebUIModelInfo> =
        logAndRethrow("getModels") { client.get("$baseUrl/sdapi/v1/sd-models").body() }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getSamplers(): List<SDWebUISamplerInfo> =
        logAndRethrow("getSamplers") { client.get("$baseUrl/sdapi/v1/samplers").body() }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getVaes(): List<SDWebUIVaeInfo> =
        logAndRethrow("getVaes") { client.get("$baseUrl/sdapi/v1/vae").body() }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun txt2img(request: SDWebUITxt2ImgRequest): SDWebUIGenerationResponse =
        logAndRethrow("txt2img") {
            client.post("$baseUrl/sdapi/v1/txt2img") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun img2img(request: SDWebUIImg2ImgRequest): SDWebUIGenerationResponse =
        logAndRethrow("img2img") {
            client.post("$baseUrl/sdapi/v1/img2img") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    /**
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getProgress(): SDWebUIProgressResponse =
        logAndRethrow("getProgress") {
            client.get("$baseUrl/sdapi/v1/progress?skip_current_image=true").body()
        }

    /**
     * @throws ResponseException on HTTP error response
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun interrupt(): Unit =
        logAndRethrow("interrupt") { client.post("$baseUrl/sdapi/v1/interrupt") }

    /**
     * Executes [block], catching known Ktor / serialization exceptions,
     * logging them, and rethrowing. Unknown exceptions propagate without logging.
     */
    private suspend inline fun <T> logAndRethrow(operation: String, block: () -> T): T {
        try {
            return block()
        } catch (e: ResponseException) {
            logApiError(operation, e)
        } catch (e: SerializationException) {
            logApiError(operation, e)
        } catch (e: HttpRequestTimeoutException) {
            logApiError(operation, e)
        } catch (e: ConnectTimeoutException) {
            logApiError(operation, e)
        }
    }

    /** Logs an API error and rethrows the exception. */
    private fun logApiError(operation: String, cause: Throwable): Nothing {
        Logger.e(TAG, "$operation failed: ${cause.message}", cause)
        throw cause
    }

    private companion object {
        const val TAG = "SDWebUIApi"
    }
}
