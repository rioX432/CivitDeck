package com.riox432.civitdeck.data.api.comfyui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Raw envelope received over the ComfyUI WebSocket.
 * Shape: {"type": "...", "data": {...}}
 */
@Serializable
data class ComfyUIWsEnvelope(
    val type: String,
    val data: JsonObject = JsonObject(emptyMap()),
)

/**
 * Typed WebSocket messages after parsing the envelope.
 */
sealed class ComfyUIWebSocketMessage {
    data class Status(val queueRemaining: Int) : ComfyUIWebSocketMessage()
    data class ExecutionStart(val promptId: String) : ComfyUIWebSocketMessage()
    data class Executing(val promptId: String, val node: String?) : ComfyUIWebSocketMessage()
    data class Progress(
        val promptId: String,
        val value: Int,
        val max: Int,
        val node: String,
    ) : ComfyUIWebSocketMessage()
    data class Executed(val promptId: String, val node: String) : ComfyUIWebSocketMessage()
    data class ExecutionSuccess(val promptId: String) : ComfyUIWebSocketMessage()
    data class ExecutionError(val promptId: String, val exceptionMessage: String) : ComfyUIWebSocketMessage()

    /** Binary preview image data sent during sampling (e.g. from SaveImageWebsocket node). */
    data class PreviewImage(val imageBytes: ByteArray) : ComfyUIWebSocketMessage() {
        override fun equals(other: Any?): Boolean =
            other is PreviewImage && imageBytes.contentEquals(other.imageBytes)
        override fun hashCode(): Int = imageBytes.contentHashCode()
    }
    data class Unknown(val type: String) : ComfyUIWebSocketMessage()
}

/**
 * Intermediate DTOs used for deserialization of the `data` field.
 */
@Serializable
internal data class WsStatusData(
    val status: WsStatusInner? = null,
)

@Serializable
internal data class WsStatusInner(
    @SerialName("exec_info") val execInfo: WsExecInfo? = null,
)

@Serializable
internal data class WsExecInfo(
    @SerialName("queue_remaining") val queueRemaining: Int = 0,
)

@Serializable
internal data class WsExecutionStartData(
    @SerialName("prompt_id") val promptId: String = "",
)

@Serializable
internal data class WsExecutingData(
    @SerialName("prompt_id") val promptId: String = "",
    val node: String? = null,
)

@Serializable
internal data class WsProgressData(
    val value: Int = 0,
    val max: Int = 0,
    @SerialName("prompt_id") val promptId: String = "",
    val node: String = "",
)

@Serializable
internal data class WsExecutedData(
    @SerialName("prompt_id") val promptId: String = "",
    val node: String = "",
)

@Serializable
internal data class WsExecutionSuccessData(
    @SerialName("prompt_id") val promptId: String = "",
)

@Serializable
internal data class WsExecutionErrorData(
    @SerialName("prompt_id") val promptId: String = "",
    @SerialName("exception_message") val exceptionMessage: String = "",
)
