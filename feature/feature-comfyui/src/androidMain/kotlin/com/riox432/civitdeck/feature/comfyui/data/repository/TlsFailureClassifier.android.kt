package com.riox432.civitdeck.feature.comfyui.data.repository

import javax.net.ssl.SSLException

/**
 * On Android (JVM), TLS handshake failures surface as [SSLException] (including
 * SSLHandshakeException, SSLPeerUnverifiedException) anywhere in the cause chain.
 */
actual fun isTlsFailure(throwable: Throwable): Boolean {
    var current: Throwable? = throwable
    while (current != null) {
        if (current is SSLException) return true
        current = current.cause
    }
    return false
}
