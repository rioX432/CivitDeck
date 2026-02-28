package com.riox432.civitdeck.data.api.civitailink

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CivitaiLinkApi(
    private val client: HttpClient,
    private val json: Json,
) {
    companion object {
        private const val HOST = "civitai.com"
        private const val PATH_PREFIX = "/api/link/ws?key="
    }

    fun observeLink(key: String): Flow<CivitaiLinkIncomingMessage> = callbackFlow {
        client.wss(host = HOST, path = "$PATH_PREFIX$key") {
            outgoing.invokeOnClose { close() }
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    runCatching {
                        val msg = json.decodeFromString<CivitaiLinkIncomingMessage>(frame.readText())
                        trySend(msg)
                    }
                }
            }
        }
        close()
    }

    suspend fun sendCommand(key: String, command: CivitaiLinkOutgoingMessage) {
        client.wss(host = HOST, path = "$PATH_PREFIX$key") {
            val text = json.encodeToString(command)
            send(text)
        }
    }
}
