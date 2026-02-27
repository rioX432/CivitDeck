package com.riox432.civitdeck.data.api.comfyui

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ComfyUIApi(
    private val client: HttpClient,
    private val json: Json,
) {
    private var baseUrl: String = ""

    fun setBaseUrl(hostname: String, port: Int) {
        baseUrl = "http://$hostname:$port"
    }

    /**
     * Health check / queue status: GET /queue
     */
    suspend fun getQueue(): QueueResponse {
        return client.get("$baseUrl/queue").body()
    }

    /**
     * Fetch available checkpoints: GET /object_info/CheckpointLoaderSimple
     */
    suspend fun getCheckpoints(): List<String> {
        val text = client.get("$baseUrl/object_info/CheckpointLoaderSimple").bodyAsText()
        return parseCheckpointNames(text)
    }

    /**
     * Submit workflow: POST /prompt
     */
    suspend fun submitPrompt(workflow: JsonObject): PromptResponse {
        val body = buildJsonObject { put("prompt", workflow) }
        return client.post("$baseUrl/prompt") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    /**
     * Get generation history: GET /history/{promptId}
     */
    suspend fun getHistory(promptId: String): HistoryEntry? {
        val text = client.get("$baseUrl/history/$promptId").bodyAsText()
        val root = json.decodeFromString<Map<String, HistoryEntry>>(text)
        return root[promptId]
    }

    /**
     * Build image URL for viewing: GET /view?filename=...&subfolder=...&type=output
     */
    fun getImageUrl(image: ComfyUIOutputImage): String {
        return "$baseUrl/view?filename=${image.filename}&subfolder=${image.subfolder}&type=${image.type}"
    }

    private fun parseCheckpointNames(responseText: String): List<String> {
        val root = json.decodeFromString<JsonObject>(responseText)
        val info = root["CheckpointLoaderSimple"]?.let {
            json.decodeFromString<CheckpointLoaderInfo>(it.toString())
        }
        return info?.input?.required?.ckptName?.firstOrNull() ?: emptyList()
    }
}
