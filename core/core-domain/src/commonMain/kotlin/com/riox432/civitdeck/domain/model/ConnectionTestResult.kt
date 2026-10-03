package com.riox432.civitdeck.domain.model

/**
 * Actionable cause for a failed ComfyUI connection test.
 * Used to show the user a specific hint instead of a generic "failed".
 */
enum class ConnectionFailureCause {
    /** Host could not be reached (no route, DNS failure, or any refusal not reported as [Refused]). */
    Unreachable,

    /** The host was reached but nothing accepted the connection on the port (TCP connection refused). */
    Refused,

    /** Connection or request timed out. */
    Timeout,

    /** TLS/SSL handshake failed (e.g. self-signed certificate not trusted). */
    Tls,

    /**
     * Server answered HTTP 401 or 403. ComfyUI itself has no authentication, so a proxy or auth
     * layer in front of it is rejecting the request.
     */
    AuthRequired,

    /** Server answered 2xx, but the response is not ComfyUI's (another service is on this port). */
    NotComfyUI,

    /** Host is a loopback address, which points at this device instead of the ComfyUI machine. */
    LoopbackHost,

    /** The OS denied access to the local network (e.g. iOS Local Network permission). */
    LocalNetworkDenied,

    /** Server responded with an HTTP error status not covered by a more specific cause. */
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

    /**
     * The health check failed; [cause] describes why. [httpStatus] is set for
     * [ConnectionFailureCause.Http] and [ConnectionFailureCause.AuthRequired].
     */
    data class Failure(
        val cause: ConnectionFailureCause,
        val httpStatus: Int? = null,
    ) : ConnectionTestResult
}
