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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ComfyUIApi(
    private val client: HttpClient,
    private val json: Json,
) {
    private val _baseUrl = MutableStateFlow("")

    /**
     * Sets the API base URL. Accepts a full URL (e.g. "https://myserver:8188")
     * or falls back to constructing one from hostname + port.
     */
    fun setBaseUrl(url: String) {
        _baseUrl.value = url.trimEnd('/')
    }

    /**
     * Legacy overload: constructs http:// URL from hostname and port.
     */
    fun setBaseUrl(hostname: String, port: Int) {
        _baseUrl.value = "http://$hostname:$port"
    }

    /**
     * Health check / queue status: GET /queue
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getQueue(): QueueResponse =
        logAndRethrow("getQueue") { client.get("${_baseUrl.value}/queue").body() }

    /**
     * Fetch available checkpoints: GET /object_info/CheckpointLoaderSimple
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getCheckpoints(): List<String> = logAndRethrow("getCheckpoints") {
        val text = client.get("${_baseUrl.value}/object_info/CheckpointLoaderSimple").bodyAsText()
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
        val text = client.get("${_baseUrl.value}/object_info/LoraLoader").bodyAsText()
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
        val text = client.get("${_baseUrl.value}/object_info/ControlNetLoader").bodyAsText()
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
        logAndRethrow("getFullObjectInfo") { client.get("${_baseUrl.value}/object_info").bodyAsText() }

    /**
     * Submit workflow: POST /prompt
     * @param clientId WebSocket client id that ComfyUI routes this job's non-broadcast events
     *   (`execution_success`, `execution_error`) to. Omitted from the body when null.
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun submitPrompt(
        workflow: JsonObject,
        clientId: String? = null,
    ): PromptResponse = logAndRethrow("submitPrompt") {
        val body = buildJsonObject {
            put("prompt", workflow)
            if (clientId != null) put("client_id", clientId)
        }
        client.post("${_baseUrl.value}/prompt") {
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
        logAndRethrow("interrupt") { client.post("${_baseUrl.value}/interrupt") }

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
        client.post("${_baseUrl.value}/queue") {
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
        val text = client.get("${_baseUrl.value}/history").bodyAsText()
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
            val text = client.get("${_baseUrl.value}/history/$promptId").bodyAsText()
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
            url = "${_baseUrl.value}/upload/image",
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
     * Fetch system stats: GET /system_stats
     * Returns hardware info (GPU, VRAM, RAM) and software versions.
     * Available in ComfyUI 0.1.0+.
     * @throws ResponseException on HTTP error response
     * @throws SerializationException on deserialization failure
     * @throws HttpRequestTimeoutException on request timeout
     * @throws ConnectTimeoutException on connection timeout
     */
    suspend fun getSystemStats(): SystemStatsResponse =
        logAndRethrow("getSystemStats") { client.get("${_baseUrl.value}/system_stats").body() }

    /**
     * Build image URL for viewing: GET /view?filename=...&type=output[&subfolder=...]
     * Omits subfolder when empty to avoid ComfyUI rejecting the request.
     */
    fun getImageUrl(image: ComfyUIOutputImage): String {
        val url = _baseUrl.value
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

    /**
     * Unlike [parseNodeInputList], decode errors propagate so callers can report a failed
     * checkpoint fetch instead of showing an empty picker.
     */
    private fun parseCheckpointNames(responseText: String): List<String> =
        requiredInputNames(json.decodeFromString<JsonObject>(responseText), "CheckpointLoaderSimple", "ckpt_name")

    /**
     * Generic parser for /object_info nodes that return a list of filenames
     * under `inputs.required.<fieldName>[0]`.
     */
    private fun parseNodeInputList(responseText: String, nodeType: String, fieldName: String): List<String> {
        return try {
            requiredInputNames(json.decodeFromString<JsonObject>(responseText), nodeType, fieldName)
        } catch (e: SerializationException) {
            Logger.w(TAG, "Failed to parse node input list ($nodeType/$fieldName): ${e.message}")
            emptyList()
        } catch (e: IllegalArgumentException) {
            Logger.w(TAG, "Failed to parse node input list ($nodeType/$fieldName): ${e.message}")
            emptyList()
        }
    }

    /**
     * Reads the names at `<nodeType>.input.required.<fieldName>[0]`. Later elements, such as the
     * `{"tooltip": ...}` options object ComfyUI appends, are ignored. Returns an empty list when
     * any level is absent or has an unexpected shape.
     */
    @Suppress("ReturnCount")
    private fun requiredInputNames(root: JsonObject, nodeType: String, fieldName: String): List<String> {
        val nodeInfo = root[nodeType] as? JsonObject ?: return emptyList()
        val inputObj = nodeInfo["input"] as? JsonObject ?: return emptyList()
        val requiredObj = inputObj["required"] as? JsonObject ?: return emptyList()
        val fieldArray = requiredObj[fieldName] as? JsonArray ?: return emptyList()
        val namesList = fieldArray.firstOrNull() as? JsonArray ?: return emptyList()
        return namesList.mapNotNull { (it as? JsonPrimitive)?.content }
    }

    private companion object {
        const val TAG = "ComfyUIApi"
    }
}
