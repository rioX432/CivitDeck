package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.concurrent.Volatile

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
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getQueue(): QueueResponse =
        logAndRethrow("getQueue") { client.get("$baseUrl/queue").body() }

    /**
     * Fetch available checkpoints: GET /object_info/CheckpointLoaderSimple
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getCheckpoints(): List<String> = logAndRethrow("getCheckpoints") {
        val text = client.get("$baseUrl/object_info/CheckpointLoaderSimple").bodyAsText()
        parseCheckpointNames(text)
    }

    /**
     * Fetch available LoRA models: GET /object_info/LoraLoader
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getLoras(): List<String> = logAndRethrow("getLoras") {
        val text = client.get("$baseUrl/object_info/LoraLoader").bodyAsText()
        parseNodeInputList(text, "LoraLoader", "lora_name")
    }

    /**
     * Fetch available ControlNet models: GET /object_info/ControlNetLoader
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getControlNets(): List<String> = logAndRethrow("getControlNets") {
        val text = client.get("$baseUrl/object_info/ControlNetLoader").bodyAsText()
        parseNodeInputList(text, "ControlNetLoader", "control_net_name")
    }

    /**
     * Fetch the full /object_info response containing schemas for all node types.
     * Used for dynamic parameter extraction (dropdown options, min/max ranges, etc.).
     * @throws ResponseException on HTTP error response
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getFullObjectInfo(): String =
        logAndRethrow("getFullObjectInfo") { client.get("$baseUrl/object_info").bodyAsText() }

    /**
     * Submit workflow: POST /prompt
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun submitPrompt(workflow: JsonObject): PromptResponse = logAndRethrow("submitPrompt") {
        val body = buildJsonObject { put("prompt", workflow) }
        client.post("$baseUrl/prompt") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    /**
     * Interrupt the currently running generation: POST /interrupt
     * @throws ResponseException on HTTP error response
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun interrupt(): Unit =
        logAndRethrow("interrupt") { client.post("$baseUrl/interrupt") }

    /**
     * Delete (cancel) queued prompts: POST /queue with {"delete": [...promptIds]}
     * @throws ResponseException on HTTP error response
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun deleteQueue(promptIds: List<String>): Unit = logAndRethrow("deleteQueue") {
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
     * Get all generation history: GET /history
     * Returns a map of prompt_id -> HistoryEntry for all completed prompts.
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getAllHistory(): Map<String, HistoryEntry> = logAndRethrow("getAllHistory") {
        val text = client.get("$baseUrl/history").bodyAsText()
        json.decodeFromString(text)
    }

    /**
     * Get generation history: GET /history/{promptId}
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getHistory(promptId: String): HistoryEntry? =
        logAndRethrow("getHistory (promptId=$promptId)") {
            val text = client.get("$baseUrl/history/$promptId").bodyAsText()
            val root = json.decodeFromString<Map<String, HistoryEntry>>(text)
            root[promptId]
        }

    /**
     * Upload an image to the ComfyUI server: POST /upload/image (multipart form data).
     * Returns the uploaded filename from the server response.
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        filename: String,
        subfolder: String = "",
        imageType: String = "input",
    ): UploadImageResponse = logAndRethrow("uploadImage") {
        client.submitFormWithBinaryData(
            url = "$baseUrl/upload/image",
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
        } catch (e: SerializationException) {
            Logger.w(TAG, "Failed to parse node input list ($nodeType/$fieldName): ${e.message}")
            emptyList()
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to parse node input list ($nodeType/$fieldName): ${e.message}")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "ComfyUIApi"
    }
}
