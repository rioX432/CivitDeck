package com.riox432.civitdeck.domain.service

/**
 * iOS no-op implementation. iOS cannot maintain a background WebSocket connection
 * reliably, so background monitoring is not supported.
 */
actual class BackgroundMonitorStarter {
    actual fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
        // No-op on iOS
    }

    actual fun stopMonitoring() {
        // No-op on iOS
    }
}
