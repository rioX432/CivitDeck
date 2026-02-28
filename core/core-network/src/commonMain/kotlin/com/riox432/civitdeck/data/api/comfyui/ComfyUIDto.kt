package com.riox432.civitdeck.data.api.comfyui

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
 * Response from GET /queue
 */
@Serializable
data class QueueResponse(
    @SerialName("queue_running") val running: List<JsonObject> = emptyList(),
    @SerialName("queue_pending") val pending: List<JsonObject> = emptyList(),
)

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
 */
@Serializable
data class HistoryEntry(
    val status: HistoryStatus? = null,
    val outputs: Map<String, HistoryNodeOutput> = emptyMap(),
)

@Serializable
data class HistoryStatus(
    @SerialName("status_str") val statusStr: String? = null,
    val completed: Boolean? = null,
)

/**
 * Checkpoint info from GET /object_info/CheckpointLoaderSimple
 */
@Serializable
data class CheckpointLoaderInfo(
    val input: CheckpointInput? = null,
)

@Serializable
data class CheckpointInput(
    val required: CheckpointRequired? = null,
)

@Serializable
data class CheckpointRequired(
    @SerialName("ckpt_name") val ckptName: List<List<String>>? = null,
)
