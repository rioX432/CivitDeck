package com.riox432.civitdeck.domain.model

/**
 * Centralized default connection values shared across local-generation server
 * settings screens (ComfyUI / SD WebUI) and the network layer.
 *
 * Keeping these in one place avoids the host/port literals drifting between the
 * Desktop/Android settings UIs and the WebSocket connection code.
 */
object ComfyUiConnectionDefaults {
    /** Default host used when adding a new local server connection. */
    const val DEFAULT_HOST = "127.0.0.1"

    /** Fallback WebSocket (ws://) port when none is present in the base URL. */
    const val DEFAULT_WS_PORT = 80

    /** Fallback secure WebSocket (wss://) port when none is present in the base URL. */
    const val DEFAULT_WSS_PORT = 443
}
