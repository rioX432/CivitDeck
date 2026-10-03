package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.domain.model.ConnectionFailureCause

private const val CONNECTION_REFUSED = "Connection refused"
private const val NS_URL_ERROR_DOMAIN = "NSURLErrorDomain"

// NSURLErrorCannotConnectToHost, as it appears in Foundation's NSError description.
private const val NS_URL_ERROR_CANNOT_CONNECT_TO_HOST = "Code=-1004"

// Guards against a cyclic cause chain, which Throwable does not prevent beyond a direct self-cause.
private const val MAX_CAUSE_DEPTH = 16

/**
 * Classifies a transport failure that produced no HTTP response. Each engine reports a refused TCP
 * connection (typically ComfyUI started without `--listen`) differently: JVM and OkHttp messages
 * say `Connection refused` (OkHttp nests `ECONNREFUSED (Connection refused)` in a cause), and Ktor
 * Darwin embeds the NSError description. The messages are matched in common code, like the iOS TLS
 * classifier, so the logic stays unit-testable without the Darwin engine. Anything else, including
 * DNS and no-route errors, stays [ConnectionFailureCause.Unreachable].
 */
internal fun transportFailureCause(throwable: Throwable): ConnectionFailureCause =
    if (isConnectionRefused(throwable)) ConnectionFailureCause.Refused else ConnectionFailureCause.Unreachable

private fun isConnectionRefused(throwable: Throwable): Boolean =
    generateSequence(throwable) { it.cause }.take(MAX_CAUSE_DEPTH).any { current ->
        val message = current.message.orEmpty()
        message.contains(CONNECTION_REFUSED, ignoreCase = true) ||
            (NS_URL_ERROR_DOMAIN in message && NS_URL_ERROR_CANNOT_CONNECT_TO_HOST in message)
    }
