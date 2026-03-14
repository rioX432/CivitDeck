package com.riox432.civitdeck.data.api.comfyui

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

private const val MAX_RETRIES = 5
private const val BASE_DELAY_MS = 1_000L
private const val MAX_DELAY_MS = 16_000L

/**
 * Manages a ComfyUI WebSocket connection and emits typed [ComfyUIWebSocketMessage] events.
 *
 * The returned Flow reconnects automatically on failure using exponential backoff (max 5 retries).
 * Messages are filtered to the given [promptId] so callers only receive events for their job.
 */
private const val TAG = "ComfyUIWebSocketApi"

class ComfyUIWebSocketApi(
    private val client: HttpClient,
    private val json: Json,
) {
    /**
     * Opens `ws://{host}:{port}/ws?clientId={clientId}` and emits messages for [promptId].
     * Completes when [ComfyUIWebSocketMessage.ExecutionSuccess] or [ComfyUIWebSocketMessage.ExecutionError]
     * is received for [promptId].
     */
    fun observeProgress(
        host: String,
        port: Int,
        clientId: String,
        promptId: String,
    ): Flow<ComfyUIWebSocketMessage> = flow {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt <= MAX_RETRIES) {
            lastError = null
            try {
                runWebSocketSession(host, port, clientId, promptId)
                // Session ended normally — return without retrying
                attempt = MAX_RETRIES + 1
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "WebSocket connection failed (attempt $attempt): ${e.message}")
                lastError = e
                attempt++
                if (attempt <= MAX_RETRIES) {
                    val backoff = minOf(BASE_DELAY_MS * (1L shl (attempt - 1)), MAX_DELAY_MS)
                    delay(backoff)
                }
            }
        }
        lastError?.let { throw it }
    }

    private suspend fun FlowCollector<ComfyUIWebSocketMessage>.runWebSocketSession(
        host: String,
        port: Int,
        clientId: String,
        promptId: String,
    ) {
        client.webSocket(host = host, port = port, path = "/ws?clientId=$clientId") {
            var done = false
            while (!done) {
                val msg = incoming.receive().toRelevantMessage(promptId)
                if (msg != null) {
                    emit(msg)
                    done = isTerminal(msg)
                }
            }
        }
    }

    private fun Frame.toRelevantMessage(promptId: String): ComfyUIWebSocketMessage? {
        if (this !is Frame.Text) return null
        val msg = parseMessage(readText()) ?: return null
        return if (isRelevant(msg, promptId)) msg else null
    }

    private fun isTerminal(msg: ComfyUIWebSocketMessage): Boolean =
        msg is ComfyUIWebSocketMessage.ExecutionSuccess ||
            msg is ComfyUIWebSocketMessage.ExecutionError

    @Suppress("CyclomaticComplexMethod")
    private fun parseMessage(text: String): ComfyUIWebSocketMessage? {
        return try {
            val envelope = json.decodeFromString<ComfyUIWsEnvelope>(text)
            when (envelope.type) {
                "status" -> {
                    val data = json.decodeFromJsonElement<WsStatusData>(envelope.data)
                    val remaining = data.status?.execInfo?.queueRemaining ?: 0
                    ComfyUIWebSocketMessage.Status(remaining)
                }
                "execution_start" -> {
                    val data = json.decodeFromJsonElement<WsExecutionStartData>(envelope.data)
                    ComfyUIWebSocketMessage.ExecutionStart(data.promptId)
                }
                "executing" -> {
                    val data = json.decodeFromJsonElement<WsExecutingData>(envelope.data)
                    ComfyUIWebSocketMessage.Executing(data.promptId, data.node)
                }
                "progress" -> {
                    val data = json.decodeFromJsonElement<WsProgressData>(envelope.data)
                    ComfyUIWebSocketMessage.Progress(data.promptId, data.value, data.max, data.node)
                }
                "executed" -> {
                    val data = json.decodeFromJsonElement<WsExecutedData>(envelope.data)
                    ComfyUIWebSocketMessage.Executed(data.promptId, data.node)
                }
                "execution_success" -> {
                    val data = json.decodeFromJsonElement<WsExecutionSuccessData>(envelope.data)
                    ComfyUIWebSocketMessage.ExecutionSuccess(data.promptId)
                }
                "execution_error" -> {
                    val data = json.decodeFromJsonElement<WsExecutionErrorData>(envelope.data)
                    ComfyUIWebSocketMessage.ExecutionError(data.promptId, data.exceptionMessage)
                }
                else -> ComfyUIWebSocketMessage.Unknown(envelope.type)
            }
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            Logger.w(TAG, "Failed to parse WebSocket message: ${e.message}")
            null
        }
    }

    /** Returns true for messages that carry a promptId matching the active job, or status messages. */
    private fun isRelevant(msg: ComfyUIWebSocketMessage, promptId: String): Boolean = when (msg) {
        is ComfyUIWebSocketMessage.Status -> true
        is ComfyUIWebSocketMessage.ExecutionStart -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.Executing -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.Progress -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.Executed -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.ExecutionSuccess -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.ExecutionError -> msg.promptId == promptId
        is ComfyUIWebSocketMessage.Unknown -> false
    }
}
