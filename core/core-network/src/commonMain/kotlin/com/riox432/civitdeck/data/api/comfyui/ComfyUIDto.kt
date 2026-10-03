package com.riox432.civitdeck.data.api.comfyui

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Response from POST /prompt
 */
@Serializable
data class PromptResponse(
    @SerialName("prompt_id") val promptId: String,
    @SerialName("number") val number: Int? = null,
)

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

@Serializable
data class HistoryStatus(
    @SerialName("status_str") val statusStr: String? = null,
    val completed: Boolean? = null,
)

/**
 * Response from POST /upload/image
 */
@Serializable
data class UploadImageResponse(
    val name: String,
    val subfolder: String = "",
    val type: String = "input",
)
