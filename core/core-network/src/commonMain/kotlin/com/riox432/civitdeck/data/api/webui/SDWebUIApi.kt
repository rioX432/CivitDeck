package com.riox432.civitdeck.data.api.webui

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.concurrent.Volatile

class SDWebUIApi(private val client: HttpClient) {
    @Volatile
    private var baseUrl: String = ""

    fun setBaseUrl(hostname: String, port: Int) {
        baseUrl = "http://$hostname:$port"
    }

    suspend fun getModels(): List<SDWebUIModelInfo> {
        val url = baseUrl
        return client.get("$url/sdapi/v1/sd-models").body()
    }

    suspend fun getSamplers(): List<SDWebUISamplerInfo> {
        val url = baseUrl
        return client.get("$url/sdapi/v1/samplers").body()
    }

    suspend fun getVaes(): List<SDWebUIVaeInfo> {
        val url = baseUrl
        return client.get("$url/sdapi/v1/vae").body()
    }

    suspend fun txt2img(request: SDWebUITxt2ImgRequest): SDWebUIGenerationResponse {
        val url = baseUrl
        return client.post("$url/sdapi/v1/txt2img") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun img2img(request: SDWebUIImg2ImgRequest): SDWebUIGenerationResponse {
        val url = baseUrl
        return client.post("$url/sdapi/v1/img2img") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getProgress(): SDWebUIProgressResponse {
        val url = baseUrl
        return client.get("$url/sdapi/v1/progress?skip_current_image=true").body()
    }

    suspend fun interrupt() {
        val url = baseUrl
        client.post("$url/sdapi/v1/interrupt")
    }
}
