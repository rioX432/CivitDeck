package com.riox432.civitdeck.domain.model

/**
 * Actionable cause for a failed ComfyUI connection test.
 * Used to show the user a specific hint instead of a generic "failed".
 */
enum class ConnectionFailureCause {
    /** Host could not be reached (connection refused, no route, DNS failure). */
    Unreachable,

    /** Connection or request timed out. */
    Timeout,

    /** TLS/SSL handshake failed (e.g. self-signed certificate not trusted). */
    Tls,

    /** Server responded with an HTTP error status. */
    Http,

    /** Any other failure that does not match a known cause. */
    Unknown,
}

/**
 * Result of testing a [ComfyUIConnection] against a live server.
 */
sealed interface ConnectionTestResult {
    /**
     * The server responded to the health check.
     * [stats] is present when /system_stats is available, null otherwise.
     */
    data class Success(val stats: SystemStats?) : ConnectionTestResult

    /** The health check failed; [cause] describes why and [httpStatus] is set for [ConnectionFailureCause.Http]. */
    data class Failure(
        val cause: ConnectionFailureCause,
        val httpStatus: Int? = null,
    ) : ConnectionTestResult
}
