package com.riox432.civitdeck.domain.model

/**
 * Determines the security level of a [ComfyUIConnection].
 */
object SecurityLevelHelper {
    private val lanPatterns = listOf(
        Regex("^10\\..*"),
        Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"),
        Regex("^192\\.168\\..*"),
        Regex("^127\\..*"),
        Regex("^localhost$", RegexOption.IGNORE_CASE),
        // Tailscale traffic is WireGuard-encrypted, so plain HTTP to a tailnet peer is not
        // internet-exposed. 100.64.0.0/10 is shared with carrier-grade NAT, but a home ComfyUI is
        // not served from a CGNAT address, so treating the whole range as private is low-risk.
        Regex("^100\\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\\..*"),
        // The leading dot keeps look-alike domains such as evil-ts.net remote.
        Regex("^.+\\.ts\\.net$", RegexOption.IGNORE_CASE),
    )

    fun getSecurityLevel(connection: ComfyUIConnection): ConnectionSecurityLevel {
        if (connection.useHttps) {
            return if (connection.acceptSelfSigned) {
                ConnectionSecurityLevel.SelfSigned
            } else {
                ConnectionSecurityLevel.Secure
            }
        }
        // HTTP — check if it's on a LAN address
        val isLan = lanPatterns.any { it.matches(connection.hostname) }
        return if (isLan) {
            ConnectionSecurityLevel.LocalInsecure
        } else {
            ConnectionSecurityLevel.RemoteInsecure
        }
    }
}
