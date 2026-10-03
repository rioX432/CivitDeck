package com.riox432.civitdeck.data.api.comfyui

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Response from POST /prompt
 */
@Serializable
data class PromptResponse(
    @SerialName("prompt_id") val promptId: String,
    @SerialName("number") val number: Int? = null,
)

/**
 * Error body of a rejected ComfyUI request. [error] stays a [JsonElement] because current servers
 * send an object (`type`, `message`, `details`, `extra_info`) and older ones a plain string.
 */
@Serializable
data class ComfyUIErrorResponse(
    val error: JsonElement? = null,
) {
    /**
     * `"<message>: <details>"`, or whichever of the two is not blank; the string itself for the
     * string form; null when the body carries no usable reason.
     */
    val reason: String?
        get() = when (val e = error) {
            is JsonObject -> listOfNotNull(e.nonBlankString("message"), e.nonBlankString("details"))
                .joinToString(": ")
                .ifEmpty { null }
            is JsonPrimitive -> e.contentOrNull?.takeIf { it.isNotBlank() }
            else -> null
        }

    private fun JsonObject.nonBlankString(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}

/**
 * A non-2xx ComfyUI response whose [message] is the server's own reason (or `HTTP <status>`
 * when the body has none), so callers that show `e.message` display it as-is.
 */
class ComfyUIResponseException(
    response: HttpResponse,
    cachedResponseText: String,
    override val message: String,
) : ResponseException(response, cachedResponseText)

/**
 * Response from GET /queue.
 * Each entry is an array: [queue_number, prompt_id, prompt, extra_data, outputs_to_execute].
 * Using JsonElement to handle both running (array of arrays) and pending (array of arrays).
 */
@Serializable
data class QueueResponse(
    @SerialName("queue_running") val running: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    @SerialName("queue_pending") val pending: List<kotlinx.serialization.json.JsonElement> = emptyList(),
)

/**
 * Outcome of probing GET /queue without deserializing into [QueueResponse], whose all-default
 * fields would accept any JSON object. [hasQueueRunning] is true only when the body is a JSON
 * object containing `queue_running`, which real ComfyUI always returns.
 */
data class QueueProbe(
    val status: Int,
    val hasQueueRunning: Boolean,
) {
    val isSuccessStatus: Boolean get() = HttpStatusCode.fromValue(status).isSuccess()
    val isComfyUIQueue: Boolean get() = isSuccessStatus && hasQueueRunning
}

/**
 * A single output image reference from ComfyUI history.
 */
@Serializable
data class ComfyUIOutputImage(
    val filename: String,
    val subfolder: String = "",
    val type: String = "output",
)

/**
 * Node output within history entry.
 */
@Serializable
data class HistoryNodeOutput(
    val images: List<ComfyUIOutputImage>? = null,
)

/**
 * A single history entry for a prompt.
 * The [prompt] field is an array: [index, prompt_id, {nodeId: {class_type, inputs}}, ...].
 * Index 2 contains the node graph used during generation.
 */
@Serializable
data class HistoryEntry(
    val status: HistoryStatus? = null,
    val outputs: Map<String, HistoryNodeOutput> = emptyMap(),
    @SerialName("prompt") val prompt: kotlinx.serialization.json.JsonArray? = null,
) {
    /**
     * Extracts the node graph (index 2 of the prompt array) as a map of node_id -> node object.
     * Returns null if the prompt array is malformed or absent.
     */
    val promptNodes: JsonObject?
        get() = prompt?.getOrNull(2) as? JsonObject
}

/**
 * [messages] is ComfyUI's list of `[event, data]` pairs. It stays a [JsonElement] because each
 * pair mixes a string and an object, and an unexpected shape must not fail the whole entry.
 */
@Serializable
data class HistoryStatus(
    @SerialName("status_str") val statusStr: String? = null,
    val completed: Boolean? = null,
    val messages: JsonElement? = null,
) {
    /**
     * `exception_message` of the last `execution_error` message, or null when there is none
     * (an interrupted job records `execution_interrupted` instead) or it is blank.
     */
    val executionErrorMessage: String?
        get() = (messages as? JsonArray)
            ?.mapNotNull { it as? JsonArray }
            ?.lastOrNull { it.eventName() == EVENT_EXECUTION_ERROR }
            ?.let { it.getOrNull(1) as? JsonObject }
            ?.let { it["exception_message"] as? JsonPrimitive }
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }

    private fun JsonArray.eventName(): String? = (getOrNull(0) as? JsonPrimitive)?.contentOrNull

    private companion object {
        const val EVENT_EXECUTION_ERROR = "execution_error"
    }
}

/**
 * Response from POST /upload/image
 */
@Serializable
data class UploadImageResponse(
    val name: String,
    val subfolder: String = "",
    val type: String = "input",
)
