package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.repository.BaseModelCatalogRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BaseModelCatalogRepositoryImpl : BaseModelCatalogRepository {

    private val bundledCatalog = buildBaseModelCatalog(
        activeValues = BUNDLED_ACTIVE_BASE_MODELS,
        allValues = BUNDLED_ALL_BASE_MODELS,
    )

    override fun observeCatalog(): Flow<BaseModelCatalog> = flowOf(bundledCatalog)
}

private const val TAG = "BaseModelCatalogRepository"

/**
 * Builds a catalog from the `ActiveBaseModel` and `BaseModel` arrays of `/api/v1/enums`, keeping
 * their order. A value containing `,` is dropped because saved filters and the model-search cache
 * key join selected values with `,`, so it could never be stored or looked up unambiguously.
 * An active value missing from [allValues] still counts as active.
 */
internal fun buildBaseModelCatalog(
    activeValues: List<String>,
    allValues: List<String>,
): BaseModelCatalog {
    val active = activeValues.toSelectable()
    val activeSet = active.toSet()
    return BaseModelCatalog(
        active = active,
        retired = allValues.toSelectable().filterNot { it in activeSet },
    )
}

private fun List<String>.toSelectable(): List<BaseModel> = distinct().mapNotNull { value ->
    when {
        value.isBlank() -> null
        ',' in value -> {
            Logger.w(TAG, "Dropping base model containing ',': $value")
            null
        }
        else -> BaseModel(value)
    }
}

// `ActiveBaseModel` and `BaseModel` from GET https://civitai.com/api/v1/enums, response of
// 2026-10-03. Copied verbatim: the API matches `baseModels` case-sensitively.
private val BUNDLED_ACTIVE_BASE_MODELS = listOf(
    "Anima",
    "AuraFlow",
    "Chroma",
    "CogVideoX",
    "Ernie",
    "Flux.1 S",
    "Flux.1 D",
    "Flux.1 Krea",
    "Flux.1 Kontext",
    "Flux.2 D",
    "Flux.2 Klein 9B",
    "Flux.2 Klein 9B-base",
    "Flux.2 Klein 4B",
    "Flux.2 Klein 4B-base",
    "Flux.3",
    "Flux 3 Video",
    "Grok",
    "HappyHorse",
    "HiDream",
    "HiDream-O1",
    "Hunyuan 1",
    "Hunyuan Video",
    "Ideogram 4.0",
    "Boogu",
    "Illustrious",
    "Kolors",
    "Krea 2",
    "LTXV",
    "LTXV2",
    "LTXV 2.3",
    "LTXV 2.5",
    "Lens",
    "Lumina",
    "MageFlow",
    "MAI",
    "Mochi",
    "NoobAI",
    "Upscaler",
    "Other",
    "PixArt a",
    "PixArt E",
    "Pony",
    "Pony V7",
    "Qwen",
    "Qwen 2",
    "Qwen 2.1",
    "Qwen 3",
    "SD 1.4",
    "SD 1.5",
    "SD 1.5 LCM",
    "SD 1.5 Hyper",
    "SD 2.0",
    "SD 2.1",
    "SDXL 1.0",
    "SDXL Lightning",
    "SDXL Hyper",
    "Reve",
    "Muse Image",
    "Wan Video 1.3B t2v",
    "Wan Video 14B t2v",
    "Wan Video 14B i2v 480p",
    "Wan Video 14B i2v 720p",
    "Wan Video 2.2 TI2V-5B",
    "Wan Video 2.2 I2V-A14B",
    "Wan Video 2.2 T2V-A14B",
    "Wan Video 2.5 T2V",
    "Wan Video 2.5 I2V",
    "Wan Image 2.7",
    "Wan Video 2.7",
    "Wan Video 3.0",
    "ZImageTurbo",
    "ZImageBase",
    "Ming Image Design 0.1",
    "Ming Image Design Layer 0.1",
    "MiniMax H3",
    "ACE Audio",
    "MiniMax Music 3",
    "Sonilo",
)

private val BUNDLED_ALL_BASE_MODELS = listOf(
    "Anima",
    "AuraFlow",
    "Chroma",
    "CogVideoX",
    "Ernie",
    "Flux.1 S",
    "Flux.1 D",
    "Flux.1 Krea",
    "Flux.1 Kontext",
    "Flux.2 D",
    "Flux.2 Klein 9B",
    "Flux.2 Klein 9B-base",
    "Flux.2 Klein 4B",
    "Flux.2 Klein 4B-base",
    "Flux.3",
    "Flux 3 Video",
    "Grok",
    "HappyHorse",
    "HiDream",
    "HiDream-O1",
    "Hunyuan 1",
    "Hunyuan Video",
    "Ideogram 4.0",
    "Ideogram 4.5",
    "Boogu",
    "Illustrious",
    "Imagen4",
    "Kolors",
    "Krea 2",
    "LTXV",
    "LTXV2",
    "LTXV 2.3",
    "LTXV 2.5",
    "Lens",
    "Lumina",
    "MageFlow",
    "MAI",
    "Mochi",
    "Nano Banana",
    "NoobAI",
    "ODOR",
    "OpenAI",
    "Upscaler",
    "Other",
    "PixArt a",
    "PixArt E",
    "Playground v2",
    "Pony",
    "Pony V7",
    "Qwen",
    "Qwen 2",
    "Qwen 2.1",
    "Qwen 3",
    "Stable Cascade",
    "SD 1.4",
    "SD 1.5",
    "SD 1.5 LCM",
    "SD 1.5 Hyper",
    "SD 2.0",
    "SD 2.0 768",
    "SD 2.1",
    "SD 2.1 768",
    "SD 2.1 Unclip",
    "SD 3",
    "SD 3.5",
    "SD 3.5 Large",
    "SD 3.5 Large Turbo",
    "SD 3.5 Medium",
    "SDXL 0.9",
    "SDXL 1.0",
    "SDXL 1.0 LCM",
    "SDXL Lightning",
    "SDXL Hyper",
    "SDXL Turbo",
    "SDXL Distilled",
    "Reve",
    "Muse Image",
    "Seedream",
    "SVD",
    "SVD XT",
    "Sora 2",
    "Veo 3",
    "Wan Video",
    "Wan Video 1.3B t2v",
    "Wan Video 14B t2v",
    "Wan Video 14B i2v 480p",
    "Wan Video 14B i2v 720p",
    "Wan Video 2.2 TI2V-5B",
    "Wan Video 2.2 I2V-A14B",
    "Wan Video 2.2 T2V-A14B",
    "Wan Video 2.5 T2V",
    "Wan Video 2.5 I2V",
    "Wan Image 2.7",
    "Wan Video 2.7",
    "Wan Video 3.0",
    "ZImageTurbo",
    "ZImageBase",
    "Ming Image Design 0.1",
    "Ming Image Design Layer 0.1",
    "Vidu Q1",
    "MiniMax H3",
    "Kling",
    "Seedance",
    "ACE Audio",
    "MiniMax Music 3",
    "YuE2",
    "Sonilo",
    "PolyGen",
    "Tripo",
    "Hunyuan3D",
    "Pixal3D",
    "Trellis.2",
)
