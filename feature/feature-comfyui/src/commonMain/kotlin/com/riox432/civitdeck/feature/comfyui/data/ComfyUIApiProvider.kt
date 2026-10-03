package com.riox432.civitdeck.feature.comfyui.data

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIServerTrust
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketApi
import com.riox432.civitdeck.data.api.comfyui.ComfyUIWebSocketMessage
import com.riox432.civitdeck.data.api.comfyui.createComfyUIHttpClient
import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.DomainException
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ComfyUI APIs bound to one server. [httpClient] may accept only that server's pinned
 * certificate and the trust manager does not check the host, so it is only ever pointed at
 * [baseUrl]: the WebSocket is opened through [observeProgress] instead of being exposed.
 */
class ComfyUIEndpoint internal constructor(
    val baseUrl: String,
    internal val httpClient: HttpClient,
    json: Json,
) {
    val api: ComfyUIApi = ComfyUIApi(httpClient, json).apply { setBaseUrl(baseUrl) }

    private val webSocketApi = ComfyUIWebSocketApi(httpClient, json)
    private val wsScheme = if (baseUrl.startsWith("https://")) "wss" else "ws"

    fun observeProgress(clientId: String, promptId: String): Flow<ComfyUIWebSocketMessage> =
        webSocketApi.observeProgress(baseUrl, wsScheme, clientId, promptId)
}

/**
 * Builds a [ComfyUIEndpoint] per call so a request's URL and TLS trust come from the same
 * connection row. A row uses its pinned certificate only when `useHttps && acceptSelfSigned`;
 * every other row uses the shared system-trust [sharedClient].
 */
class ComfyUIApiProvider(
    private val dao: ComfyUIConnectionDao,
    private val sharedClient: HttpClient,
    private val json: Json,
    private val createPinnedClient: (ComfyUIServerTrust.PinnedLeaf) -> HttpClient = {
        createComfyUIHttpClient(it)
    },
) {
    // Keyed by pin. A cached client's PinnedLeaf.presentedSha256 is shared by every request
    // on it, so it must not be read to attribute a certificate to one request. Clients for a
    // pin that is no longer stored are kept, because closing one cancels calls still on it.
    private val pinnedClients = MutableStateFlow<Map<String?, HttpClient>>(emptyMap())
    private val snapshot = MutableStateFlow<List<ComfyUIConnectionEntity>>(emptyList())

    /** @throws DomainException.ConnectionException when no connection is active. */
    suspend fun forActive(): ComfyUIEndpoint {
        val active = dao.getActive()
            ?: throw DomainException.ConnectionException("No active ComfyUI connection")
        return endpoint(active.hostname, active.port, active.useHttps, active.pinnedTrust())
    }

    fun forConnection(connection: ComfyUIConnection): ComfyUIEndpoint {
        val trust = if (connection.useHttps && connection.acceptSelfSigned) {
            PinnedTrust(connection.tlsCertSha256)
        } else {
            null
        }
        return endpoint(connection.hostname, connection.port, connection.useHttps, trust)
    }

    /**
     * Endpoint for the server [url] points at, using the trust of the stored connection on the
     * same host and port (see [trustFor]) and system trust otherwise.
     */
    suspend fun forUrl(url: String): ComfyUIEndpoint {
        val parsed = Url(url)
        val useHttps = parsed.protocol == URLProtocol.HTTPS
        val trust = if (useHttps) trustFor(dao.getAll(), parsed.host, parsed.port) else null
        return endpoint(parsed.host, parsed.port, useHttps, trust)
    }

    /** Collects the stored connections for [pinnedSha256For]. Call once, at app start. */
    fun startSnapshot(scope: CoroutineScope): Job = scope.launch {
        // Caught because the app-lifetime scope has no handler, so a database error would
        // otherwise crash the app; the last snapshot stays in place.
        dao.observeAll()
            .catch { Logger.e(TAG, "Connection snapshot stopped: ${it.message}", it) }
            .collect { snapshot.value = it }
    }

    /**
     * Non-suspending pin lookup for callers that only have a URL, such as image loaders.
     * Returns null until [startSnapshot] has delivered the stored connections.
     */
    fun pinnedSha256For(host: String, port: Int): String? = trustFor(snapshot.value, host, port)?.sha256

    private fun endpoint(host: String, port: Int, useHttps: Boolean, trust: PinnedTrust?): ComfyUIEndpoint {
        val scheme = if (useHttps) "https" else "http"
        val client = if (trust != null) pinnedClient(trust.sha256) else sharedClient
        return ComfyUIEndpoint("$scheme://$host:$port", client, json)
    }

    private fun pinnedClient(pin: String?): HttpClient {
        pinnedClients.value[pin]?.let { return it }
        val created = createPinnedClient(ComfyUIServerTrust.PinnedLeaf(pin))
        val winner = pinnedClients.updateAndGet { if (pin in it) it else it + (pin to created) }.getValue(pin)
        if (winner !== created) created.close()
        return winner
    }

    /** Wraps a pin that may be null: a self-signed row whose certificate is not confirmed yet. */
    private class PinnedTrust(val sha256: String?)

    private companion object {
        const val TAG = "ComfyUIApiProvider"

        fun ComfyUIConnectionEntity.pinnedTrust(): PinnedTrust? =
            if (useHttps && acceptSelfSigned) PinnedTrust(tlsCertSha256) else null

        /**
         * Two rows can share a host and port. The active row decides when it matches, even
         * with an unconfirmed pin; otherwise a pin applies only when every matching row that
         * has one agrees, so conflicting pins fall back to system trust instead of guessing.
         * Null means system trust.
         */
        fun trustFor(rows: List<ComfyUIConnectionEntity>, host: String, port: Int): PinnedTrust? {
            val matching = rows.filter { it.hostname.equals(host, ignoreCase = true) && it.port == port }
            matching.firstOrNull { it.isActive }?.let { return it.pinnedTrust() }
            val pin = matching.mapNotNull { it.pinnedTrust()?.sha256 }.distinct().singleOrNull()
            return pin?.let { PinnedTrust(it) }
        }
    }
}
