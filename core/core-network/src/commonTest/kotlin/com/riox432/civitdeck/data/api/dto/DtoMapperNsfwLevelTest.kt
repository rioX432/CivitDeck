package com.riox432.civitdeck.data.api.dto

import com.riox432.civitdeck.domain.model.NsfwLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the numeric browsing-level bitmask mapping used by the `/models`
 * endpoint (PG=1, PG13=2, R=4, X=8, XXX=16, Blocked=32). The previous mapping
 * only matched exact single bits 1/2/4/8, silently classifying XXX (16),
 * Blocked (32), and any combined mask as [NsfwLevel.None] — explicit content
 * was treated as safe, bypassing blur and the Off filter.
 */
class DtoMapperNsfwLevelTest {

    private fun levelOf(raw: Int?, nsfw: Boolean = false): NsfwLevel =
        ModelImageDto(url = "u", nsfw = nsfw, nsfwLevel = raw).toDomain().nsfwLevel

    @Test
    fun single_bits_map_to_their_level() {
        assertEquals(NsfwLevel.None, levelOf(1))
        assertEquals(NsfwLevel.Soft, levelOf(2))
        assertEquals(NsfwLevel.Mature, levelOf(4))
        assertEquals(NsfwLevel.X, levelOf(8))
    }

    @Test
    fun xxx_and_blocked_map_to_x_not_none() {
        assertEquals(NsfwLevel.X, levelOf(16))
        assertEquals(NsfwLevel.X, levelOf(32))
    }

    @Test
    fun combined_masks_classify_by_strictest_bit() {
        assertEquals(NsfwLevel.Soft, levelOf(3)) // PG|PG13
        assertEquals(NsfwLevel.Mature, levelOf(7)) // PG|PG13|R
        assertEquals(NsfwLevel.X, levelOf(31)) // up to XXX
    }

    @Test
    fun unrated_falls_back_to_the_legacy_nsfw_boolean() {
        assertEquals(NsfwLevel.None, levelOf(null, nsfw = false))
        assertEquals(NsfwLevel.None, levelOf(0, nsfw = false))
        assertEquals(NsfwLevel.Mature, levelOf(null, nsfw = true))
        assertEquals(NsfwLevel.Mature, levelOf(0, nsfw = true))
    }

    @Test
    fun string_levels_from_the_images_endpoint_map_directly() {
        fun levelOfString(raw: String?, nsfw: Boolean = false): NsfwLevel =
            ImageDto(id = 1, url = "u", nsfw = nsfw, nsfwLevel = raw).toDomain().nsfwLevel

        assertEquals(NsfwLevel.None, levelOfString("None"))
        assertEquals(NsfwLevel.Soft, levelOfString("Soft"))
        assertEquals(NsfwLevel.Mature, levelOfString("Mature"))
        assertEquals(NsfwLevel.X, levelOfString("X"))
        // Unknown ratings must not be treated as safe when the image is flagged nsfw.
        assertEquals(NsfwLevel.Mature, levelOfString("Blocked", nsfw = true))
        assertEquals(NsfwLevel.None, levelOfString(null, nsfw = false))
    }
}
