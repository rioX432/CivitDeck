package com.riox432.civitdeck.data.api.webui

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException

class SDWebUIApi(private val client: HttpClient) {
    @Volatile
    private var baseUrl: String = ""

    fun setBaseUrl(hostname: String, port: Int) {
        baseUrl = "http://$hostname:$port"
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getModels(): List<SDWebUIModelInfo> {
        val url = baseUrl
        return try {
            client.get("$url/sdapi/v1/sd-models").body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getModels failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getSamplers(): List<SDWebUISamplerInfo> {
        val url = baseUrl
        return try {
            client.get("$url/sdapi/v1/samplers").body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getSamplers failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getVaes(): List<SDWebUIVaeInfo> {
        val url = baseUrl
        return try {
            client.get("$url/sdapi/v1/vae").body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getVaes failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun txt2img(request: SDWebUITxt2ImgRequest): SDWebUIGenerationResponse {
        val url = baseUrl
        return try {
            client.post("$url/sdapi/v1/txt2img") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "txt2img failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun img2img(request: SDWebUIImg2ImgRequest): SDWebUIGenerationResponse {
        val url = baseUrl
        return try {
            client.post("$url/sdapi/v1/img2img") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "img2img failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getProgress(): SDWebUIProgressResponse {
        val url = baseUrl
        return try {
            client.get("$url/sdapi/v1/progress?skip_current_image=true").body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getProgress failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network failure
     */
    suspend fun interrupt() {
        val url = baseUrl
        try {
            client.post("$url/sdapi/v1/interrupt")
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "interrupt failed: ${e.message}", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "SDWebUIApi"
    }
}
