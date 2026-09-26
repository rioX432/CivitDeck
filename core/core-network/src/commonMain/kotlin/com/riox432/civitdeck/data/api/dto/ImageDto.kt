package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
data class ImageListResponse(
    val items: List<ImageDto>,
    val metadata: PaginationMetadataDto,
)

@Serializable
data class ImageDto(
    val id: Long = 0,
    val url: String = "",
    val hash: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val nsfw: Boolean = false,
    val nsfwLevel: String? = null,
    val createdAt: String? = null,
    val postId: Long? = null,
    val username: String? = null,
    val stats: ImageStatsDto? = null,
    val meta: ImageMetaDto? = null,
    val type: String? = null,
)

@Serializable
data class ImageStatsDto(
    val cryCount: Int = 0,
    val laughCount: Int = 0,
    val likeCount: Int = 0,
    val heartCount: Int = 0,
    val commentCount: Int = 0,
)

@Serializable(with = ImageMetaDtoSerializer::class)
data class ImageMetaDto(
    val prompt: String? = null,
    val negativePrompt: String? = null,
    val sampler: String? = null,
    val cfgScale: Double? = null,
    val steps: Int? = null,
    val seed: Long? = null,
    @SerialName("Model")
    val model: String? = null,
    @SerialName("Size")
    val size: String? = null,
    val additionalParams: Map<String, String> = emptyMap(),
)

private val KNOWN_KEYS = setOf(
    "prompt",
    "negativePrompt",
    "sampler",
    "cfgScale",
    "steps",
    "seed",
    "Model",
    "Size",
)

// `comfy` is the full ComfyUI workflow graph as a JSON string (often tens of KB); rendering it as
// an additional-parameter row would flood the metadata UI and bloat the response cache.
private val EXCLUDED_KEYS = setOf("comfy")

internal object ImageMetaDtoSerializer : KSerializer<ImageMetaDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ImageMetaDto")

    override fun deserialize(decoder: Decoder): ImageMetaDto {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement() as JsonObject
        val additional = mutableMapOf<String, String>()
        for ((key, value) in obj) {
            if (key !in KNOWN_KEYS && key !in EXCLUDED_KEYS && value is JsonPrimitive) {
                value.contentOrNull?.let { additional[key] = it }
            }
        }
        return ImageMetaDto(
            prompt = obj.primitive("prompt")?.contentOrNull,
            negativePrompt = obj.primitive("negativePrompt")?.contentOrNull,
            sampler = obj.primitive("sampler")?.contentOrNull,
            cfgScale = obj.primitive("cfgScale")?.doubleOrNull,
            steps = obj.primitive("steps")?.longOrNull?.toInt(),
            seed = obj.primitive("seed")?.longOrNull,
            model = obj.primitive("Model")?.contentOrNull,
            size = obj.primitive("Size")?.contentOrNull,
            additionalParams = additional,
        )
    }

    // Returns null for object/array values instead of throwing, so one malformed item cannot fail
    // a whole page of images.
    private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    override fun serialize(encoder: Encoder, value: ImageMetaDto) {
        val jsonEncoder = encoder as JsonEncoder
        val map = buildMap {
            value.prompt?.let { put("prompt", JsonPrimitive(it)) }
            value.negativePrompt?.let { put("negativePrompt", JsonPrimitive(it)) }
            value.sampler?.let { put("sampler", JsonPrimitive(it)) }
            value.cfgScale?.let { put("cfgScale", JsonPrimitive(it)) }
            value.steps?.let { put("steps", JsonPrimitive(it)) }
            value.seed?.let { put("seed", JsonPrimitive(it)) }
            value.model?.let { put("Model", JsonPrimitive(it)) }
            value.size?.let { put("Size", JsonPrimitive(it)) }
            for ((k, v) in value.additionalParams) {
                put(k, JsonPrimitive(v))
            }
        }
        jsonEncoder.encodeJsonElement(JsonObject(map))
    }
}
