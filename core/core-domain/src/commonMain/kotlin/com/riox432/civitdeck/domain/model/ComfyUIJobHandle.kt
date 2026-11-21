package com.riox432.civitdeck.domain.model

/**
 * The server a ComfyUI job was submitted to, frozen at submission time.
 *
 * Capture it before submitting and reuse it for polling, the progress socket, cancellation,
 * result fetches and background monitoring. Activating another connection never changes it.
 * When the saved connection is deleted, or its endpoint or TLS settings change, operations on
 * this target must fail explicitly instead of falling back to the active connection.
 *
 * Only the fields that decide where requests go and which certificate is trusted are part of
 * the identity; the connection's name, active flag and test results are not.
 */
data class ComfyUIJobTarget(
    val connectionId: Long,
    val hostname: String,
    val port: Int,
    val useHttps: Boolean,
    val acceptSelfSigned: Boolean,
    val tlsCertSha256: String?,
) {
    val baseUrl: String get() {
        val scheme = if (useHttps) "https" else "http"
        return "$scheme://$hostname:$port"
    }

    val wsScheme: String get() = if (useHttps) "wss" else "ws"

    companion object {
        fun from(connection: ComfyUIConnection): ComfyUIJobTarget = ComfyUIJobTarget(
            connectionId = connection.id,
            hostname = connection.hostname,
            port = connection.port,
            useHttps = connection.useHttps,
            acceptSelfSigned = connection.acceptSelfSigned,
            tlsCertSha256 = connection.tlsCertSha256,
        )
    }
}

/** A submitted ComfyUI job. Equal prompt IDs on different targets are different jobs. */
data class ComfyUIJobHandle(
    val target: ComfyUIJobTarget,
    val promptId: String,
)
