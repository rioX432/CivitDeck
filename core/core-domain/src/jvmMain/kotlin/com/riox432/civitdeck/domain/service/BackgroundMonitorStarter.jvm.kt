package com.riox432.civitdeck.domain.service

/**
 * JVM/Desktop no-op implementation. Desktop apps are always in the foreground,
 * so background monitoring is not needed.
 */
actual class BackgroundMonitorStarter {
    actual fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String) {
        // No-op on Desktop
    }

    actual fun stopMonitoring() {
        // No-op on Desktop
    }
}
