package com.riox432.civitdeck.domain.model

/**
 * A server discovered via mDNS/LAN scan.
 */
data class DiscoveredServer(
    val hostname: String,
    val ip: String,
    val port: Int,
    val displayName: String,
)
