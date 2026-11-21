package com.riox432.civitdeck.data.api.dto

import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.SourceProvider
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceCodec
import com.riox432.civitdeck.domain.model.SourceReferenceDecodeResult
import com.riox432.civitdeck.domain.model.SourceResource
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageSourceReferenceTest {

    // Same configuration as NetworkModule's Json, which decodes responses and the response cache.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun page(vararg items: String): ImageListResponse =
        json.decodeFromString(ImageListResponse.serializer(), """{"items":[${items.joinToString(",")}],"metadata":{}}""")

    private fun image(item: String): Image = page(item).items.single().toDomain()

    private fun Image.source(): SourceReference = assertNotNull(sourceReference)

    // The response example from the Civitai developer docs (reference/images.md), plus a comfy
    // workflow, an unknown array and an unknown object.
    private val complete = """
        {
          "id": 9173928,
          "url": "https://image.civitai.com/x/cc242d6c.jpeg",
          "hash": "UA8N5},:Ioni~C#laKxaoznNwvx]XmRkVstR",
          "width": 832, "height": 1216, "type": "image", "nsfw": true, "nsfwLevel": "Soft",
          "createdAt": "2025-04-17T21:28:57.225Z", "postId": 1981754, "username": "Ajuro",
          "baseModel": "SDXL 1.0",
          "modelVersionIds": [9208, 249861, 258687, 332071, 345685],
          "meta": {
            "Size": "832x1216", "seed": 1938345220, "steps": 45, "sampler": "DPM++ 2M",
            "cfgScale": 5, "clipSkip": 2, "prompt": "a castle", "negativePrompt": "blurry",
            "resources": [],
            "civitaiResources": [
              { "type": "checkpoint", "modelVersionId": 345685 },
              { "type": "lora", "weight": 0.65, "modelVersionId": 249861 }
            ],
            "comfy": "{\"nodes\":[]}",
            "hashes": {"model": "abc"}
          }
        }
    """.trimIndent()

    @Test
    fun complete_item_keeps_source_identity_settings_and_explicit_resources() {
        val source = image(complete).source()

        assertEquals(SourceProvider.Civitai, source.provider)
        assertEquals("9173928", source.artworkId)
        assertEquals("https://civitai.com/images/9173928", source.artworkUrl)
        assertEquals("https://image.civitai.com/x/cc242d6c.jpeg", source.mediaUrl)
        assertEquals("Ajuro", source.creator)
        assertEquals("Soft", source.contentRating)
        assertEquals(emptyList(), source.usageLinks)
        assertEquals(
            listOf(
                SourceResource(345685, "checkpoint", null),
                SourceResource(249861, "lora", 0.65),
                SourceResource(9208),
                SourceResource(258687),
                SourceResource(332071),
            ),
            source.resources,
        )
        val generation = assertNotNull(source.generation)
        assertEquals("a castle", generation.prompt)
        assertEquals("blurry", generation.negativePrompt)
        assertEquals("DPM++ 2M", generation.sampler)
        assertEquals(5.0, generation.cfgScale)
        assertEquals(45, generation.steps)
        assertEquals(1938345220L, generation.seed)
        assertEquals("832x1216", generation.size)
        assertNull(generation.model)
        assertEquals(mapOf("clipSkip" to "2"), generation.parameters)
        assertEquals(mapOf("resources" to "[]", "hashes" to """{"model":"abc"}"""), generation.opaqueMetadata)
    }

    @Test
    fun opaque_and_workflow_values_never_reach_the_editable_metadata() {
        val image = image(complete)
        val meta = assertNotNull(image.meta)

        assertEquals(mapOf("clipSkip" to "2"), meta.additionalParams)
        assertTrue(image.source().generation!!.opaqueMetadata.keys.none { it == "comfy" })
    }

    @Test
    fun metadata_free_item_keeps_identity_and_top_level_versions_without_type_or_weight() {
        val source = image("""{"id": 42, "url": "https://image.civitai.com/x/a.png", "modelVersionIds": [7, 8]}""")
            .source()

        assertEquals("42", source.artworkId)
        assertNull(source.generation)
        assertNull(source.creator)
        assertNull(source.contentRating)
        assertEquals(listOf(SourceResource(7), SourceResource(8)), source.resources)
    }

    @Test
    fun absent_settings_stay_null_instead_of_taking_editable_defaults() {
        val generation = assertNotNull(image("""{"id": 5, "meta": {"prompt": "only a prompt"}}""").source().generation)

        assertEquals("only a prompt", generation.prompt)
        assertNull(generation.negativePrompt)
        assertNull(generation.sampler)
        assertNull(generation.cfgScale)
        assertNull(generation.steps)
        assertNull(generation.seed)
        assertNull(generation.size)
        assertEquals(emptyMap(), generation.parameters)
    }

    @Test
    fun malformed_resources_are_skipped_without_discarding_the_page() {
        val malformed = """
            {
              "id": 11,
              "modelVersionIds": [3, "x", null, -4, 0, 2.5, {"id": 9}],
              "meta": {
                "civitaiResources": [
                  "lora", {}, {"modelVersionId": "abc"}, {"modelVersionId": "12"}, {"modelVersionId": -1},
                  {"modelVersionId": 1, "type": 5, "weight": "0.5"},
                  {"modelVersionId": 2, "type": null, "weight": null}
                ]
              }
            }
        """.trimIndent()
        val items = page(malformed, """{"id": 12}""").items.map { it.toDomain() }

        assertEquals(2, items.size)
        assertEquals(
            listOf(SourceResource(1), SourceResource(2), SourceResource(3)),
            items[0].source().resources,
        )
        assertEquals(emptyList(), items[1].source().resources)
    }

    @Test
    fun non_array_resource_fields_yield_no_resources() {
        val source = image("""{"id": 13, "modelVersionIds": 5, "meta": {"civitaiResources": {"modelVersionId": 1}}}""")
            .source()

        assertEquals(emptyList(), source.resources)
        assertEquals(emptyList(), image("""{"id": 15, "modelVersionIds": null}""").source().resources)
    }

    @Test
    fun duplicate_versions_keep_the_first_explicit_entry_and_merge_top_level_ids() {
        val source = image(
            """
            {"id": 14, "modelVersionIds": [20, 10, 30],
             "meta": {"civitaiResources": [
               {"type": "lora", "weight": 0.4, "modelVersionId": 10},
               {"type": "lora", "weight": 0.9, "modelVersionId": 10},
               {"type": "checkpoint", "modelVersionId": 20}
             ]}}
            """.trimIndent(),
        ).source()

        assertEquals(
            listOf(SourceResource(10, "lora", 0.4), SourceResource(20, "checkpoint", null), SourceResource(30)),
            source.resources,
        )
    }

    @Test
    fun missing_or_invalid_artwork_id_builds_no_reference() {
        assertNull(image("""{"url": "https://image.civitai.com/x/a.png", "hash": "LKO2"}""").sourceReference)
        assertNull(image("""{"id": 0, "hash": "LKO2"}""").sourceReference)
        assertNull(image("""{"id": -3}""").sourceReference)
    }

    @Test
    fun artwork_id_is_the_source_id_never_the_url_or_hash() {
        val source = image(complete).source()

        assertNotEquals(source.mediaUrl.hashCode().toString(), source.artworkId)
        assertNotEquals("UA8N5},:Ioni~C#laKxaoznNwvx]XmRkVstR", source.artworkId)
        assertEquals("9173928", source.artworkId)
    }

    @Test
    fun response_cache_round_trip_keeps_the_same_source_reference() {
        val original = page(complete)
        val cached = json.encodeToString(ImageListResponse.serializer(), original)
        val restored = json.decodeFromString(ImageListResponse.serializer(), cached)

        assertEquals(original.items.single().toDomain(), restored.items.single().toDomain())
    }

    @Test
    fun valid_ids_and_weights_survive_a_codec_round_trip() {
        val source = image(complete).source()

        val decoded = assertIs<SourceReferenceDecodeResult.Decoded>(
            SourceReferenceCodec.decode(SourceReferenceCodec.encode(source)),
        )
        assertEquals(source, decoded.reference)
        assertEquals(0.65, decoded.reference.resources.single { it.versionId == 249861L }.weight)
    }

    @Test
    fun codec_reports_malformed_text_instead_of_throwing() {
        val malformed = listOf(
            "not json",
            "[]",
            "{}",
            """{"version": "1", "provider": "civitai", "artworkId": "1", "artworkUrl": "https://civitai.com/images/1"}""",
            """{"version": 1, "provider": "civitai", "artworkUrl": "https://civitai.com/images/1"}""",
            """{"version": 1, "provider": "civitai", "artworkId": " ", "artworkUrl": "https://civitai.com/images/1"}""",
            """{"version": 1, "provider": "civitai", "artworkId": "1", "artworkUrl": "http://civitai.com/images/1"}""",
            """{"version": 1, "provider": "elsewhere", "artworkId": "1", "artworkUrl": "https://x.test/1"}""",
            """{"version": 1, "provider": "civitai", "artworkId": "1", "artworkUrl": "https://civitai.com/images/1",
                "resources": [{"versionId": -2}]}""",
        )
        for (raw in malformed) {
            assertEquals(SourceReferenceDecodeResult.Malformed, SourceReferenceCodec.decode(raw), raw)
        }
    }

    @Test
    fun codec_reports_a_newer_version_and_ignores_unknown_fields_of_the_current_one() {
        assertEquals(
            SourceReferenceDecodeResult.UnsupportedVersion(2),
            SourceReferenceCodec.decode("""{"version": 2, "somethingNew": true}"""),
        )
        val decoded = SourceReferenceCodec.decode(
            """{"version": 1, "provider": "civitai", "artworkId": "1", "artworkUrl": "https://civitai.com/images/1",
                "laterField": {"a": 1}}""",
        )
        assertEquals("1", assertIs<SourceReferenceDecodeResult.Decoded>(decoded).reference.artworkId)
    }

    @Test
    fun encoded_text_always_states_its_version() {
        val encoded = SourceReferenceCodec.encode(
            SourceReference(
                provider = SourceProvider.Civitai,
                artworkId = "1",
                artworkUrl = SourceReference.civitaiArtworkUrl(1),
            ),
        )

        assertTrue(encoded.contains("\"version\":1"), encoded)
    }

    @Test
    fun existing_image_construction_and_model_image_paths_still_work() {
        val legacy = Image(
            id = 1, url = "u", hash = null, width = 1, height = 1, nsfw = false, nsfwLevel = NsfwLevel.None,
            createdAt = "", postId = null, username = null, stats = null, meta = null,
        )
        assertNull(legacy.sourceReference)

        val modelImage = json.decodeFromString(
            ModelImageDto.serializer(),
            """{"url": "https://image.civitai.com/x/m.png", "meta": {"prompt": "p", "clipSkip": 2,
                "civitaiResources": [{"type": "lora", "modelVersionId": 3}]}}""",
        ).toDomain()
        assertEquals("p", modelImage.meta?.prompt)
        assertEquals(mapOf("clipSkip" to "2"), modelImage.meta?.additionalParams)
    }
}
