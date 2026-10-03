package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.api.dto.EnumsResponse
import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import com.riox432.civitdeck.domain.repository.BaseModelCatalogRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Serves CivitAI's base model catalog from a pinned 24 h cache of `/api/v1/enums`, refreshed when
 * it is missing or older than 24 h each time the catalog is observed. While offline it falls back
 * to the last cached copy of any age, then to the bundled snapshot.
 */
class BaseModelCatalogRepositoryImpl(
    private val api: CivitAiApi,
    private val localCache: LocalCacheDataSource,
    private val json: Json,
) : BaseModelCatalogRepository {

    private val bundledCatalog = buildBaseModelCatalog(
        activeValues = BUNDLED_ACTIVE_BASE_MODELS,
        allValues = BUNDLED_ALL_BASE_MODELS,
    )

    private val refreshMutex = Mutex()

    // The fallback is emitted before fetching because the client retries with backoff, so an
    // offline fetch can take many seconds and the picker must not wait for it.
    override fun observeCatalog(): Flow<BaseModelCatalog> = flow {
        val fresh = localCache.getCached(CACHE_KEY, CACHE_TTL_MILLIS)?.decodeCatalogOrNull()
        if (fresh != null) {
            emit(fresh)
            return@flow
        }
        emit(localCache.getCachedIgnoringTtl(CACHE_KEY)?.decodeCatalogOrNull() ?: bundledCatalog)
        refreshIfStale()?.let { emit(it) }
    }

    /** Returns the refreshed catalog, or null when the refresh failed and the current list stays. */
    private suspend fun refreshIfStale(): BaseModelCatalog? = refreshMutex.withLock {
        // Another collector may have refreshed while this one waited for the lock.
        localCache.getCached(CACHE_KEY, CACHE_TTL_MILLIS)?.decodeCatalogOrNull()?.let { return@withLock it }
        try {
            val response = api.getEnums()
            val catalog = response.toCatalog()
            if (catalog.active.isEmpty()) {
                Logger.w(TAG, "Enums response has no usable ActiveBaseModel values; keeping the current list")
                return@withLock null
            }
            localCache.putCache(CACHE_KEY, json.encodeToString(EnumsResponse.serializer(), response))
            localCache.pinForOffline(CACHE_KEY)
            catalog
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(TAG, "Base model catalog refresh failed; keeping the current list: ${e.message}")
            null
        }
    }

    private fun String.decodeCatalogOrNull(): BaseModelCatalog? = try {
        json.decodeFromString(EnumsResponse.serializer(), this).toCatalog()
    } catch (e: SerializationException) {
        Logger.w(TAG, "Ignoring unreadable cached enums: ${e.message}")
        null
    }

    private fun EnumsResponse.toCatalog(): BaseModelCatalog =
        buildBaseModelCatalog(activeValues = activeBaseModels, allValues = baseModels)

    private companion object {
        const val CACHE_KEY = "civitai:enums"
        const val CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L
    }
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
