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
