package com.riox432.civitdeck.feature.comfyui.data.repository

/**
 * Detects whether a thrown exception represents a TLS/SSL handshake failure
 * (e.g. an untrusted self-signed certificate). TLS exception types differ per platform,
 * so detection is delegated to platform-specific actuals rather than message matching.
 */
expect fun isTlsFailure(throwable: Throwable): Boolean
