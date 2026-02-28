package com.riox432.civitdeck.domain.model

data class SDWebUIConnection(
    val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = DEFAULT_SDWEBUI_PORT,
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
) {
    val baseUrl: String get() = "http://$hostname:$port"

    companion object {
        const val DEFAULT_SDWEBUI_PORT = 7860
    }
}

enum class SDWebUIConnectionStatus {
    Connected,
    Disconnected,
    Testing,
    Error,
    NotConfigured,
}

data class SDWebUIGenerationParams(
    val prompt: String,
    val negativePrompt: String = "",
    val steps: Int = DEFAULT_STEPS,
    val cfgScale: Double = DEFAULT_CFG,
    val width: Int = DEFAULT_DIMENSION,
    val height: Int = DEFAULT_DIMENSION,
    val samplerName: String = DEFAULT_SAMPLER,
    val seed: Long = -1,
    val initImageBase64: String? = null,
    val denoisingStrength: Double = DEFAULT_DENOISING,
) {
    val isImg2Img: Boolean get() = initImageBase64 != null

    companion object {
        const val DEFAULT_STEPS = 20
        const val DEFAULT_CFG = 7.0
        const val DEFAULT_DIMENSION = 512
        const val DEFAULT_SAMPLER = "Euler"
        const val DEFAULT_DENOISING = 0.75
    }
}

sealed class SDWebUIGenerationProgress {
    data class Generating(val step: Int, val totalSteps: Int, val fraction: Double) :
        SDWebUIGenerationProgress()
    data class Completed(val base64Images: List<String>) : SDWebUIGenerationProgress()
    data class Error(val message: String) : SDWebUIGenerationProgress()
}
