package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.repository.ServerDiscoveryRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "ServerDiscovery"
private const val DEFAULT_PORT = 8188
private const val SCAN_BATCH_SIZE = 20

/**
 * Scans LAN for ComfyUI servers by probing port 8188 on common subnet ranges.
 * Uses the ComfyUI API to verify each candidate by calling GET /queue.
 */
class ServerDiscoveryRepositoryImpl(
    private val api: ComfyUIApi,
    private val localIpProvider: LocalIpProvider,
) : ServerDiscoveryRepository {

    override fun scanForServers(): Flow<List<DiscoveredServer>> = flow {
        val discovered = mutableListOf<DiscoveredServer>()
        emit(discovered.toList()) // Emit empty initially

        val subnet = localIpProvider.getLocalSubnet()
        if (subnet == null) {
            Logger.w(TAG, "Could not determine local subnet")
            return@flow
        }

        Logger.d(TAG, "Scanning subnet: $subnet.0/24 on port $DEFAULT_PORT")
        val candidates = (1..254).map { "$subnet.$it" }

        // Probe in batches to avoid overwhelming the network
        candidates.chunked(SCAN_BATCH_SIZE).forEach { batch ->
            val results = coroutineScope {
                batch.map { ip ->
                    async { probeServer(ip) }
                }.awaitAll()
            }
            results.filterNotNull().forEach { server ->
                discovered.add(server)
                emit(discovered.toList())
            }
        }
    }

    private suspend fun probeServer(ip: String): DiscoveredServer? {
        return try {
            api.setBaseUrl("http://$ip:$DEFAULT_PORT")
            api.getQueue()
            DiscoveredServer(
                hostname = ip,
                ip = ip,
                port = DEFAULT_PORT,
                displayName = "ComfyUI @ $ip",
            )
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
expect class LocalIpProvider() {
    fun getLocalSubnet(): String?
}
