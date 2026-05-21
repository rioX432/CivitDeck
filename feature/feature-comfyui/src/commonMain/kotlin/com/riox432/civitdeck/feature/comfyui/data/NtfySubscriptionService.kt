package com.riox432.civitdeck.feature.comfyui.data

import com.riox432.civitdeck.domain.service.GenerationNotificationService
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Subscribes to an ntfy.sh topic via the JSON streaming endpoint and fires
 * local notifications when generation-complete messages arrive.
 *
 * ntfy.sh JSON stream format (one JSON object per line):
 * ```
 * {"id":"abc","time":1635528757,"event":"open","topic":"mytopic"}
 * {"id":"def","time":1635528741,"event":"message","topic":"mytopic","message":"Generation complete"}
 * {"id":"ghi","time":1635528787,"event":"keepalive","topic":"mytopic"}
 * ```
 *
 * @see <a href="https://docs.ntfy.sh/subscribe/api/">ntfy subscribe API</a>
 */
class NtfySubscriptionService(
    private val httpClient: HttpClient,
    private val notificationService: GenerationNotificationService,
    private val scope: CoroutineScope,
) {
    private var subscriptionJob: Job? = null
    private var currentTopic: String? = null
    private var currentServerUrl: String? = null

    /** Whether the service is currently subscribed to a topic. */
    val isSubscribed: Boolean get() = subscriptionJob?.isActive == true

    /**
     * Start subscribing to the given ntfy topic. Cancels any existing subscription first.
     * Reconnects automatically with exponential backoff on connection failures.
     */
    fun subscribe(serverUrl: String, topic: String) {
        if (currentTopic == topic && currentServerUrl == serverUrl && isSubscribed) {
            Logger.d(TAG, "Already subscribed to $topic on $serverUrl")
            return
        }
        unsubscribe()
        currentTopic = topic
        currentServerUrl = serverUrl
        subscriptionJob = scope.launch {
            subscribeWithReconnect(serverUrl, topic)
        }
        Logger.d(TAG, "Started subscription to $serverUrl/$topic")
    }

    /** Stop the current subscription. */
    fun unsubscribe() {
        subscriptionJob?.cancel()
        subscriptionJob = null
        currentTopic = null
        currentServerUrl = null
        Logger.d(TAG, "Unsubscribed from ntfy")
    }

    /**
     * Send a test message to the configured ntfy topic to verify the setup.
     * Returns true if the publish request succeeded.
     */
    suspend fun sendTestNotification(serverUrl: String, topic: String): Boolean {
        return try {
            val url = "${serverUrl.trimEnd('/')}/$topic"
            val response = httpClient.post(url) {
                contentType(ContentType.Text.Plain)
                setBody("CivitDeck test notification")
            }
            response.status.value in SUCCESS_STATUS_RANGE
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Test notification failed: ${e.message}")
            false
        }
    }

    private suspend fun subscribeWithReconnect(serverUrl: String, topic: String) {
        var backoffMs = INITIAL_BACKOFF_MS
        while (scope.isActive) {
            try {
                connectAndListen(serverUrl, topic)
                // If connectAndListen returns normally, the stream ended — reconnect
                backoffMs = INITIAL_BACKOFF_MS
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w(TAG, "Connection error, retrying in ${backoffMs}ms: ${e.message}")
            }
            delay(backoffMs)
            backoffMs = (backoffMs * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    private suspend fun connectAndListen(serverUrl: String, topic: String) {
        val url = "${serverUrl.trimEnd('/')}/$topic/json"
        Logger.d(TAG, "Connecting to $url")
        httpClient.prepareGet(url).execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.isBlank()) continue
                processLine(line)
            }
        }
    }

    private fun processLine(line: String) {
        try {
            val message = json.decodeFromString<NtfyMessage>(line)
            when (message.event) {
                EVENT_MESSAGE -> handleMessage(message)
                EVENT_OPEN -> Logger.d(TAG, "Stream opened for topic: ${message.topic}")
                EVENT_KEEPALIVE -> { /* ignore keepalive */ }
                else -> Logger.d(TAG, "Unknown event: ${message.event}")
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to parse ntfy message: ${e.message}")
        }
    }

    private fun handleMessage(message: NtfyMessage) {
        val text = message.message ?: return
        Logger.d(TAG, "Received ntfy message: $text")
        // Fire a local notification with the message content
        notificationService.notifyGenerationComplete(
            promptId = message.id,
            imageCount = 0,
            elapsedMs = 0L,
        )
    }

    companion object {
        private const val TAG = "NtfySubscription"
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 60_000L
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val EVENT_MESSAGE = "message"
        private const val EVENT_OPEN = "open"
        private const val EVENT_KEEPALIVE = "keepalive"
        private val SUCCESS_STATUS_RANGE = 200..299
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

/**
 * Represents a message from the ntfy.sh JSON stream.
 *
 * @see <a href="https://docs.ntfy.sh/subscribe/api/">ntfy subscribe API</a>
 */
@Serializable
private data class NtfyMessage(
    val id: String = "",
    val time: Long = 0L,
    val event: String = "",
    val topic: String = "",
    val message: String? = null,
    val title: String? = null,
    val priority: Int? = null,
    val tags: List<String>? = null,
)
