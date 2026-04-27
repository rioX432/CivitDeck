package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException

@Suppress("TooManyFunctions")
class ComfyUIApi(
    private val client: HttpClient,
    private val json: Json,
) {
    @Volatile
    private var baseUrl: String = ""

    /**
     * Sets the API base URL. Accepts a full URL (e.g. "https://myserver:8188")
     * or falls back to constructing one from hostname + port.
     */
    fun setBaseUrl(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /**
     * Legacy overload: constructs http:// URL from hostname and port.
     */
    fun setBaseUrl(hostname: String, port: Int) {
        baseUrl = "http://$hostname:$port"
    }

    /**
     * Health check / queue status: GET /queue
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getQueue(): QueueResponse {
        val url = baseUrl
        return try {
            client.get("$url/queue").body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getQueue failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fetch available checkpoints: GET /object_info/CheckpointLoaderSimple
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getCheckpoints(): List<String> {
        val url = baseUrl
        return try {
            val text = client.get("$url/object_info/CheckpointLoaderSimple").bodyAsText()
            parseCheckpointNames(text)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getCheckpoints failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fetch available LoRA models: GET /object_info/LoraLoader
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getLoras(): List<String> {
        val url = baseUrl
        return try {
            val text = client.get("$url/object_info/LoraLoader").bodyAsText()
            parseNodeInputList(text, "LoraLoader", "lora_name")
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getLoras failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fetch available ControlNet models: GET /object_info/ControlNetLoader
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getControlNets(): List<String> {
        val url = baseUrl
        return try {
            val text = client.get("$url/object_info/ControlNetLoader").bodyAsText()
            parseNodeInputList(text, "ControlNetLoader", "control_net_name")
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getControlNets failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Fetch the full /object_info response containing schemas for all node types.
     * Used for dynamic parameter extraction (dropdown options, min/max ranges, etc.).
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getFullObjectInfo(): String {
        val url = baseUrl
        return try {
            client.get("$url/object_info").bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getFullObjectInfo failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Submit workflow: POST /prompt
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun submitPrompt(workflow: JsonObject): PromptResponse {
        val url = baseUrl
        val body = buildJsonObject { put("prompt", workflow) }
        return try {
            client.post("$url/prompt") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "submitPrompt failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Interrupt the currently running generation: POST /interrupt
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network failure
     */
    suspend fun interrupt() {
        val url = baseUrl
        try {
            client.post("$url/interrupt")
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "interrupt failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Delete (cancel) queued prompts: POST /queue with {"delete": [...promptIds]}
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network failure
     */
    suspend fun deleteQueue(promptIds: List<String>) {
        val url = baseUrl
        val body = buildJsonObject {
            put(
                "delete",
                kotlinx.serialization.json.buildJsonArray {
                    promptIds.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                }
            )
        }
        try {
            client.post("$url/queue") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "deleteQueue failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get all generation history: GET /history
     * Returns a map of prompt_id -> HistoryEntry for all completed prompts.
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getAllHistory(): Map<String, HistoryEntry> {
        val url = baseUrl
        return try {
            val text = client.get("$url/history").bodyAsText()
            json.decodeFromString(text)
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getAllHistory failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get generation history: GET /history/{promptId}
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun getHistory(promptId: String): HistoryEntry? {
        val url = baseUrl
        return try {
            val text = client.get("$url/history/$promptId").bodyAsText()
            val root = json.decodeFromString<Map<String, HistoryEntry>>(text)
            root[promptId]
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "getHistory failed (promptId=$promptId): ${e.message}", e)
            throw e
        }
    }

    /**
     * Upload an image to the ComfyUI server: POST /upload/image (multipart form data).
     * Returns the uploaded filename from the server response.
     * @throws CancellationException if coroutine is cancelled
     * @throws Exception on network or deserialization failure
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        filename: String,
        subfolder: String = "",
        imageType: String = "input",
    ): UploadImageResponse {
        val url = baseUrl
        return try {
            client.submitFormWithBinaryData(
                url = "$url/upload/image",
                formData = formData {
                    append(
                        "image",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"$filename\"",
                            )
                        }
                    )
                    append("subfolder", subfolder)
                    append("type", imageType)
                },
            ).body()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "uploadImage failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Build image URL for viewing: GET /view?filename=...&type=output[&subfolder=...]
     * Omits subfolder when empty to avoid ComfyUI rejecting the request.
     */
    fun getImageUrl(image: ComfyUIOutputImage): String {
        val url = baseUrl
        return URLBuilder(url).apply {
            path("view")
            parameters.append("filename", image.filename)
            parameters.append("type", image.type)
            if (image.subfolder.isNotEmpty()) {
                parameters.append("subfolder", image.subfolder)
            }
        }.buildString()
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
    @Suppress("ReturnCount")
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
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to parse node input list ($nodeType/$fieldName): ${e.message}")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "ComfyUIApi"
    }
}
