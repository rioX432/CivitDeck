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
    val ntfyServerUrl: String? = null,
    val ntfyTopic: String? = null,
    /**
     * Lowercase hex SHA-256 of the server leaf certificate (DER) the user confirmed, or null
     * when none is confirmed. Valid only for this hostname, port and [useHttps].
     */
    val tlsCertSha256: String? = null,
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

    /** Whether ntfy push notifications are configured for this connection. */
    val isNtfyConfigured: Boolean get() = !ntfyTopic.isNullOrBlank()

    /** Resolved ntfy server URL, defaulting to the public ntfy.sh instance. */
    val resolvedNtfyServerUrl: String get() =
        ntfyServerUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_NTFY_SERVER_URL

    companion object {
        const val DEFAULT_COMFYUI_PORT = 8188
        const val DEFAULT_NTFY_SERVER_URL = "https://ntfy.sh"
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

    /** Plaintext HTTP on a private network (RFC1918, loopback, Tailscale). */
    LocalInsecure,

    /** Plaintext HTTP on a non-LAN address (internet-facing, risky). */
    RemoteInsecure,
}

data class LoraSelection(
    val name: String,
    val strengthModel: Float = 1.0f,
    val strengthClip: Float = 1.0f,
)

/**
 * Choices the server's split loader nodes (`UNETLoader`, `CLIPLoader`, `VAELoader`) accept.
 * A list is empty when the server lacks that node. [clipTypes] lists the CLIPLoader `type`
 * values, which tell which model families the server can run.
 */
data class DiffusionModelResources(
    val diffusionModels: List<String> = emptyList(),
    val textEncoders: List<String> = emptyList(),
    val vaes: List<String> = emptyList(),
    val clipTypes: List<String> = emptyList(),
)

/** The ComfyUI node that creates the empty latent a diffusion model family samples from. */
enum class DiffusionLatentNode(val classType: String) {
    EMPTY_LATENT_IMAGE("EmptyLatentImage"),
    EMPTY_SD3_LATENT_IMAGE("EmptySD3LatentImage"),
}

/**
 * Files for a model that ships without a bundled text encoder and VAE, loaded by `UNETLoader`,
 * `CLIPLoader` and `VAELoader`. [clipType] is the CLIPLoader `type` of the model's family.
 * [latentNode] and [auraFlowShift] follow the family's template; a null shift adds no
 * `ModelSamplingAuraFlow` node.
 */
data class DiffusionModelSelection(
    val unetName: String,
    val textEncoderName: String,
    val clipType: String,
    val vaeName: String,
    val latentNode: DiffusionLatentNode = DiffusionLatentNode.EMPTY_LATENT_IMAGE,
    val auraFlowShift: Double? = null,
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
    // Inpainting: init image filename (uploaded to ComfyUI input folder)
    val initImageFilename: String? = null,
    // Inpainting: mask image filename (uploaded to ComfyUI input folder)
    val maskImageFilename: String? = null,
    // Inpainting: denoise strength (lower = more of original image preserved)
    val denoiseStrength: Double = DEFAULT_DENOISE,
    // Replaces [checkpoint] with split loaders when non-null; ControlNet and inpainting stay checkpoint-only
    val diffusionModel: DiffusionModelSelection? = null,
) {
    companion object {
        const val DEFAULT_STEPS = 20
        const val DEFAULT_CFG = 7.0
        const val DEFAULT_DIMENSION = 512
        const val DEFAULT_SAMPLER = "euler"
        const val DEFAULT_SCHEDULER = "normal"
        const val DEFAULT_DENOISE = 0.75
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
