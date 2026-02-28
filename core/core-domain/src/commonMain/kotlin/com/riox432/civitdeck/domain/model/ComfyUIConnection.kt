package com.riox432.civitdeck.domain.model

data class ComfyUIConnection(
    val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = DEFAULT_COMFYUI_PORT,
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
) {
    val baseUrl: String get() = "http://$hostname:$port"

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
