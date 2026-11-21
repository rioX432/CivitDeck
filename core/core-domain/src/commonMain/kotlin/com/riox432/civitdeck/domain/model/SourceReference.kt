package com.riox432.civitdeck.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Immutable record of where an artwork came from and what the source said about it, taken when
 * the artwork was decoded. It is a snapshot of the source, never of the user's edits or the
 * app's defaults: every value the source did not supply stays null or empty.
 *
 * @property artworkId the source's own artwork identifier. Never derived from a URL or hash.
 * @property artworkUrl the source's canonical page for the artwork.
 * @property mediaUrl the media file the source served, if any.
 * @property contentRating the source's own rating string, unmapped.
 * @property generation the original generation settings, or null when the source supplied none.
 * @property resources resource versions the source named explicitly, deduplicated by version ID.
 * @property usageLinks usage or license links the source supplied. Empty means unknown, never
 *   permission.
 */
@Serializable
data class SourceReference(
    val version: Int = SourceReferenceCodec.CURRENT_VERSION,
    val provider: SourceProvider,
    val artworkId: String,
    val artworkUrl: String,
    val mediaUrl: String? = null,
    val creator: String? = null,
    val contentRating: String? = null,
    val generation: SourceGenerationSnapshot? = null,
    val resources: List<SourceResource> = emptyList(),
    val usageLinks: List<String> = emptyList(),
) {
    init {
        require(artworkId.isNotBlank()) { "artworkId must not be blank" }
        require(artworkUrl.startsWith(HTTPS_PREFIX)) { "artworkUrl must use HTTPS" }
    }

    companion object {
        private const val HTTPS_PREFIX = "https://"
        private const val CIVITAI_IMAGE_URL = "https://civitai.com/images/"

        fun civitaiArtworkUrl(imageId: Long): String = "$CIVITAI_IMAGE_URL$imageId"
    }
}

@Serializable
enum class SourceProvider {
    @SerialName("civitai")
    Civitai,
}

/**
 * Generation settings exactly as the source recorded them. A null field means the source did
 * not supply it; consumers must not substitute an editable default for it.
 *
 * @property parameters other primitive values the source supplied, as text.
 * @property opaqueMetadata unrecognised object or array values, kept as raw JSON text. They are
 *   for display and preservation only and must never be executed or used to prefill settings.
 */
@Serializable
data class SourceGenerationSnapshot(
    val prompt: String? = null,
    val negativePrompt: String? = null,
    val sampler: String? = null,
    val cfgScale: Double? = null,
    val steps: Int? = null,
    val seed: Long? = null,
    val model: String? = null,
    val size: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val opaqueMetadata: Map<String, String> = emptyMap(),
)

/**
 * A resource version the source named. [type] and [weight] are null when the source did not
 * state them, for example for a version listed only by ID.
 */
@Serializable
data class SourceResource(
    val versionId: Long,
    val type: String? = null,
    val weight: Double? = null,
) {
    init {
        require(versionId > 0) { "versionId must be positive" }
    }
}

sealed interface SourceReferenceDecodeResult {
    data class Decoded(val reference: SourceReference) : SourceReferenceDecodeResult

    /** Written by a newer schema this app cannot read; keep the raw text rather than drop it. */
    data class UnsupportedVersion(val version: Int) : SourceReferenceDecodeResult

    data object Malformed : SourceReferenceDecodeResult
}

/**
 * String form of [SourceReference] for storage and backups. Callers outside core-domain use
 * only this codec, so they need no serialization dependency of their own.
 */
object SourceReferenceCodec {
    const val CURRENT_VERSION = 1
    private const val VERSION_KEY = "version"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(reference: SourceReference): String =
        json.encodeToString(SourceReference.serializer(), reference)

    fun decode(raw: String): SourceReferenceDecodeResult {
        val obj = try {
            json.parseToJsonElement(raw) as? JsonObject
        } catch (_: SerializationException) {
            null
        } ?: return SourceReferenceDecodeResult.Malformed
        val version = (obj[VERSION_KEY] as? JsonPrimitive)
            ?.takeUnless { it.isString }
            ?.intOrNull
            ?: return SourceReferenceDecodeResult.Malformed
        if (version != CURRENT_VERSION) return SourceReferenceDecodeResult.UnsupportedVersion(version)
        // SerializationException extends IllegalArgumentException, which the init checks throw.
        return try {
            SourceReferenceDecodeResult.Decoded(json.decodeFromJsonElement(SourceReference.serializer(), obj))
        } catch (_: IllegalArgumentException) {
            SourceReferenceDecodeResult.Malformed
        }
    }
}
