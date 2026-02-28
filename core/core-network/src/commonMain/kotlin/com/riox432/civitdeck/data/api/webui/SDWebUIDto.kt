package com.riox432.civitdeck.data.api.webui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SDWebUIModelInfo(
    val title: String,
    @SerialName("model_name") val modelName: String,
)

@Serializable
data class SDWebUISamplerInfo(val name: String)

@Serializable
data class SDWebUIVaeInfo(@SerialName("model_name") val modelName: String)

@Serializable
data class SDWebUITxt2ImgRequest(
    val prompt: String,
    @SerialName("negative_prompt") val negativePrompt: String = "",
    val steps: Int = 20,
    @SerialName("cfg_scale") val cfgScale: Double = 7.0,
    val width: Int = 512,
    val height: Int = 512,
    @SerialName("sampler_name") val samplerName: String = "Euler",
    val seed: Long = -1,
)

@Serializable
data class SDWebUIImg2ImgRequest(
    val prompt: String,
    @SerialName("negative_prompt") val negativePrompt: String = "",
    val steps: Int = 20,
    @SerialName("cfg_scale") val cfgScale: Double = 7.0,
    val width: Int = 512,
    val height: Int = 512,
    @SerialName("sampler_name") val samplerName: String = "Euler",
    val seed: Long = -1,
    @SerialName("init_images") val initImages: List<String> = emptyList(),
    @SerialName("denoising_strength") val denoisingStrength: Double = 0.75,
)

@Serializable
data class SDWebUIGenerationResponse(
    val images: List<String> = emptyList(),
)

@Serializable
data class SDWebUIProgressState(
    @SerialName("sampling_step") val samplingStep: Int = 0,
    @SerialName("sampling_steps") val samplingSteps: Int = 0,
)

@Serializable
data class SDWebUIProgressResponse(
    val progress: Double = 0.0,
    val state: SDWebUIProgressState = SDWebUIProgressState(),
)
