package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DtoMapperPaidAccessTest {

    // Mirrors the production Json in NetworkModule: unknown keys such as per-version paidAccess are ignored.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun decode(item: String): ModelResponse = json.decodeFromString(ModelResponse.serializer(), item)

    @Test
    fun paid_access_model_maps_to_true() {
        val item = """
            {
              "id": 958009,
              "name": "Paid model",
              "type": "Checkpoint",
              "nsfw": false,
              "hasActivePaidAccess": true,
              "modelVersions": [
                {
                  "id": 3331574,
                  "name": "v1",
                  "paidAccess": { "permanent": true, "endsAt": null }
                }
              ]
            }
        """.trimIndent()

        assertTrue(decode(item).toDomain().hasActivePaidAccess)
    }

    @Test
    fun item_without_the_key_maps_to_false() {
        val item = """
            {
              "id": 1,
              "name": "Free model",
              "type": "LORA",
              "nsfw": false,
              "modelVersions": []
            }
        """.trimIndent()

        assertFalse(decode(item).toDomain().hasActivePaidAccess)
    }

    @Test
    fun cached_list_json_without_the_key_decodes_to_false() {
        val list = """
            {
              "items": [ { "id": 2, "name": "Cached", "type": "LORA" } ],
              "metadata": {}
            }
        """.trimIndent()

        val models = json.decodeFromString(ModelListResponse.serializer(), list).items.map { it.toDomain() }
        assertEquals(listOf(false), models.map { it.hasActivePaidAccess })
    }
}
