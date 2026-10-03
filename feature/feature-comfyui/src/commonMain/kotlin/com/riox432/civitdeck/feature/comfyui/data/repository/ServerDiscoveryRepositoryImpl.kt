package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.repository.ServerDiscoveryRepository
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

private const val TAG = "ServerDiscovery"
private const val DEFAULT_PORT = 8188
private const val FIRST_HOST = 1
private const val LAST_HOST = 254

/**
 * Upper bound for one probe, connect included. The shared ComfyUI client's own timeouts do not
 * bound a silent host on iOS: Ktor's Darwin engine ignores the connect timeout and maps the
 * socket timeout (120 s) to `NSURLRequest.timeoutInterval`. A LAN ComfyUI answers `/queue` well
 * within this.
 */
internal const val PROBE_TIMEOUT_MS = 1_500L

/**
 * Probes in flight at once. Kept below OkHttp's default `Dispatcher.maxRequests` (64): a probe
 * queued inside OkHttp would spend its timeout before connecting. The shared client is not
 * isolated, so many concurrent calls elsewhere on it can still queue a probe.
 */
internal const val PROBE_CONCURRENCY = 48

/**
 * Scans LAN for ComfyUI servers by probing port 8188 on common subnet ranges.
 * Each probe uses a transient [ComfyUIApi] (wrapping the shared client) so concurrent
 * probes do not race on a shared mutable base URL.
 */
class ServerDiscoveryRepositoryImpl(
    private val client: HttpClient,
    private val json: Json,
    private val localIpProvider: LocalIpProvider,
) : ServerDiscoveryRepository {

    override fun scanForServers(): Flow<List<DiscoveredServer>> = channelFlow {
        send(emptyList())

        val subnet = localIpProvider.getLocalSubnet()
        if (subnet == null) {
            Logger.w(TAG, "Could not determine local subnet")
            return@channelFlow
        }

        Logger.d(TAG, "Scanning subnet: $subnet.0/24 on port $DEFAULT_PORT")
        val permits = Semaphore(PROBE_CONCURRENCY)
        val lock = Mutex()
        val discovered = mutableListOf<DiscoveredServer>()
        // channelFlow completes only after every launched probe has finished.
        for (host in FIRST_HOST..LAST_HOST) {
            launch {
                val server = permits.withPermit { probeServer("$subnet.$host") } ?: return@launch
                // Sending under the lock keeps each emission a superset of the previous one.
                lock.withLock {
                    discovered += server
                    send(discovered.toList())
                }
            }
        }
    }

    private suspend fun probeServer(ip: String): DiscoveredServer? = withTimeoutOrNull(PROBE_TIMEOUT_MS) {
        try {
            val api = ComfyUIApi(client, json)
            api.setBaseUrl("http://$ip:$DEFAULT_PORT")
            api.getQueue()
            DiscoveredServer(
                hostname = ip,
                ip = ip,
                port = DEFAULT_PORT,
                displayName = "ComfyUI @ $ip",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            // Expected: most IPs won't have ComfyUI running
            null
        }
    }
}

/**
 * Platform-specific provider for the local device IP subnet.
 * Returns the first 3 octets (e.g. "192.168.1") or null.
 */
interface LocalIpProvider {
    fun getLocalSubnet(): String?
}
