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
     * Fetch available LoRA models: GET /object_info/LoraLoader
     */
    suspend fun getLoras(): List<String> {
        val text = client.get("$baseUrl/object_info/LoraLoader").bodyAsText()
        return parseNodeInputList(text, "LoraLoader", "lora_name")
    }

    /**
     * Fetch available ControlNet models: GET /object_info/ControlNetLoader
     */
    suspend fun getControlNets(): List<String> {
        val text = client.get("$baseUrl/object_info/ControlNetLoader").bodyAsText()
        return parseNodeInputList(text, "ControlNetLoader", "control_net_name")
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
     * Delete (cancel) queued prompts: POST /queue with {"delete": [...promptIds]}
     */
    suspend fun deleteQueue(promptIds: List<String>) {
        val body = buildJsonObject {
            put(
                "delete",
                kotlinx.serialization.json.buildJsonArray {
                    promptIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
            )
        }
        client.post("$baseUrl/queue") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
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

    /**
     * Generic parser for /object_info nodes that return a list of filenames
     * under `inputs.required.<fieldName>[0]`.
     */
    private fun parseNodeInputList(responseText: String, nodeType: String, fieldName: String): List<String> {
        return try {
            val root = json.decodeFromString<JsonObject>(responseText)
            val nodeInfo = root[nodeType] as? JsonObject ?: return emptyList()
            val inputObj = nodeInfo["input"] as? JsonObject ?: return emptyList()
            val requiredObj = inputObj["required"] as? JsonObject ?: return emptyList()
            val fieldArray = requiredObj[fieldName] as? kotlinx.serialization.json.JsonArray
                ?: return emptyList()
            val namesList = fieldArray.firstOrNull() as? kotlinx.serialization.json.JsonArray
                ?: return emptyList()
            namesList.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            emptyList()
        }
    }
}
