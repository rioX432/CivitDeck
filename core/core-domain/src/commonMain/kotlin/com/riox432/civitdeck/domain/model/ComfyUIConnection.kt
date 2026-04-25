package com.riox432.civitdeck.domain.model

data class ComfyUIConnection(
    val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = DEFAULT_COMFYUI_PORT,
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
    val useHttps: Boolean = false,
    val acceptSelfSigned: Boolean = false,
) {
    /** HTTP base URL with the correct scheme. */
    val baseUrl: String get() {
        val scheme = if (useHttps) "https" else "http"
        return "$scheme://$hostname:$port"
    }

    /** WebSocket scheme matching the HTTP scheme. */
    val wsScheme: String get() = if (useHttps) "wss" else "ws"

    /** Whether this connection uses a secure transport (HTTPS/WSS). */
    val isSecure: Boolean get() = useHttps

    companion object {
        const val DEFAULT_COMFYUI_PORT = 8188
    }
}

enum class ComfyUIConnectionStatus {
    Connected,
    Disconnected,
    Testing,
    Error,
    NotConfigured,
}

/** Security level indicator for the connection badge. */
enum class ConnectionSecurityLevel {
    /** HTTPS with a trusted certificate. */
    Secure,

    /** HTTPS but accepting self-signed certificates. */
    SelfSigned,

    /** Plaintext HTTP on a LAN address (10.x, 192.168.x, 172.16-31.x, localhost). */
    LocalInsecure,

    /** Plaintext HTTP on a non-LAN address (internet-facing, risky). */
    RemoteInsecure,
}

data class LoraSelection(
    val name: String,
    val strengthModel: Float = 1.0f,
    val strengthClip: Float = 1.0f,
)

data class ComfyUIGenerationParams(
    val checkpoint: String,
    val prompt: String,
    val negativePrompt: String = "",
    val steps: Int = DEFAULT_STEPS,
    val cfgScale: Double = DEFAULT_CFG,
    val seed: Long = -1,
    val width: Int = DEFAULT_DIMENSION,
    val height: Int = DEFAULT_DIMENSION,
    val samplerName: String = DEFAULT_SAMPLER,
    val scheduler: String = DEFAULT_SCHEDULER,
    // LoRA injections
    val loraSelections: List<LoraSelection> = emptyList(),
    // ControlNet
    val controlNetEnabled: Boolean = false,
    val controlNetModel: String = "",
    val controlNetStrength: Float = 1.0f,
    // Custom workflow JSON (bypasses built-in workflow builder when non-null)
    val customWorkflowJson: String? = null,
) {
    companion object {
        const val DEFAULT_STEPS = 20
        const val DEFAULT_CFG = 7.0
        const val DEFAULT_DIMENSION = 512
        const val DEFAULT_SAMPLER = "euler"
        const val DEFAULT_SCHEDULER = "normal"
    }
}

enum class GenerationStatus {
    Idle,
    Submitting,
    Running,
    Completed,
    Error,
}

enum class QueueJobStatus {
    Queued,
    Running,
    Completed,
    Error,
}

data class QueueJob(
    val promptId: String,
    val queueNumber: Int,
    val status: QueueJobStatus,
)

data class GenerationResult(
    val promptId: String,
    val status: GenerationStatus,
    val imageUrls: List<String> = emptyList(),
    val error: String? = null,
)
