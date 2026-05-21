package com.riox432.civitdeck.domain.service

/**
 * Platform-specific mechanism to start a background monitor that keeps
 * the WebSocket connection alive when the app is backgrounded.
 *
 * On Android this starts a foreground service with a persistent notification.
 * On iOS and Desktop this is a no-op (iOS cannot maintain background WebSocket
 * reliably, and Desktop apps are always in the foreground).
 */
expect class BackgroundMonitorStarter {
    /**
     * Start monitoring a generation job in the background.
     *
     * @param promptId the ComfyUI prompt ID being monitored
     * @param baseUrl the ComfyUI server base URL (e.g. "http://192.168.1.100:8188")
     * @param wsScheme the WebSocket scheme ("ws" or "wss")
     */
    fun startMonitoring(promptId: String, baseUrl: String, wsScheme: String)

    /** Stop the background monitor. Called when generation completes or is interrupted. */
    fun stopMonitoring()
}
