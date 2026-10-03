package com.riox432.civitdeck.domain.model

/**
 * A model family that ComfyUI loads with `UNETLoader`, `CLIPLoader` and `VAELoader`. Sampler
 * values follow the family's official ComfyUI template.
 *
 * [baseModel] is CivitAI's `baseModel` for the family and doubles as its display name. The
 * text encoder and VAE hints are case-insensitive prefixes of a file's base name; they only
 * preselect a file and never decide the family, which is not guessable from file names.
 * Deliberately separate from [BaseModel], the search-filter enum.
 */
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
