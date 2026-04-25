package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.DiscoveredServer
import kotlinx.coroutines.flow.Flow

/**
 * Scans the LAN for ComfyUI servers via mDNS or TCP probe.
 */
interface ServerDiscoveryRepository {
    /**
     * Starts a scan and emits discovered servers. The flow completes when the scan finishes.
     */
    fun scanForServers(): Flow<List<DiscoveredServer>>
}
