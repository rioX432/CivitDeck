package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * On iOS (Darwin), TLS failures surface as a Ktor-wrapped NSError. Stable error-type
 * introspection via K/N cinterop is brittle, so this falls back to message matching
 * against the NSURLErrorDomain TLS messages (last-resort, per design review).
 *
 * Note: the Darwin client does not bypass self-signed certificates (known limitation),
 * so a self-signed server will reliably surface here as a TLS failure.
 */
private val tlsKeywords = listOf("ssl", "tls", "certificate", "secure connection", "trust")

actual fun isTlsFailure(throwable: Throwable): Boolean {
    var current: Throwable? = throwable
    while (current != null) {
        val message = current.message?.lowercase().orEmpty()
        if (tlsKeywords.any { it in message }) return true
        current = current.cause
    }
    return false
}
