package com.riox432.civitdeck.domain.model

/**
 * A model family that ComfyUI loads with `UNETLoader`, `CLIPLoader` and `VAELoader`. Sampler
 * values follow the family's official ComfyUI template.
 *
 * [baseModel] is CivitAI's `baseModel` for the family and doubles as its display name. The
 * text encoder and VAE hints are case-insensitive prefixes of a file's base name; they only
 * preselect a file and never decide the family, which is not guessable from file names.
 * Deliberately separate from [BaseModel], the search-filter value.
 */
@Suppress("LongParameterList") // Each entry is one table row; grouping columns would scatter it.
enum class DiffusionModelFamily(
    val baseModel: String,
    val clipType: String,
    val textEncoderHint: String,
    val vaeHint: String,
    val steps: Int,
    val cfgScale: Double,
    val samplerName: String,
    val scheduler: String,
    val width: Int,
    val height: Int,
    val latentNode: DiffusionLatentNode = DiffusionLatentNode.EMPTY_LATENT_IMAGE,
    val auraFlowShift: Double? = null,
) {
    KREA_2(
        baseModel = "Krea 2",
        clipType = "krea2",
        textEncoderHint = "qwen3vl_4b",
        vaeHint = "qwen_image_vae",
        steps = 8,
        cfgScale = 1.0,
        samplerName = "euler",
        scheduler = "simple",
        width = 1024,
        height = 1024,
    ),
    ANIMA(
        baseModel = "Anima",
        clipType = "stable_diffusion",
        textEncoderHint = "qwen_3_06b",
        vaeHint = "qwen_image_vae",
        steps = 30,
        cfgScale = 4.0,
        samplerName = "euler",
        scheduler = "simple",
        width = 1024,
        height = 1024,
    ),
    Z_IMAGE_TURBO(
        baseModel = "ZImageTurbo",
        clipType = "lumina2",
        textEncoderHint = "qwen_3_4b",
        // The bare name, because a prefix of "ae" alone would match unrelated VAE files.
        vaeHint = "ae.safetensors",
        steps = 8,
        cfgScale = 1.0,
        samplerName = "res_multistep",
        scheduler = "simple",
        width = 1024,
        height = 1024,
        latentNode = DiffusionLatentNode.EMPTY_SD3_LATENT_IMAGE,
        auraFlowShift = 3.0,
    ),
    ;

    /** The server's `CLIPLoader` must list [clipType]; older ComfyUI versions lack newer types. */
    fun isSupportedBy(serverClipTypes: List<String>): Boolean = clipType in serverClipTypes

    companion object {
        fun forBaseModel(baseModel: String?): DiffusionModelFamily? {
            val name = baseModel?.trim() ?: return null
            return entries.firstOrNull { it.baseModel.equals(name, ignoreCase = true) }
        }
    }
}
