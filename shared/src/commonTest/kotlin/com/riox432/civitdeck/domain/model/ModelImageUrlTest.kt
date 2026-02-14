package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelImageUrlTest {

    private fun image(url: String) = ModelImage(
        url = url,
        nsfw = false,
        nsfwLevel = NsfwLevel.None,
        width = 512,
        height = 512,
        hash = null,
        meta = null,
    )

    // -- thumbnailUrl --

    @Test
    fun thumbnailUrl_inserts_width_for_civitai_cdn() {
        val img = image("https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/original=true/photo.jpeg")
        val result = img.thumbnailUrl(450)
        assertEquals(
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/width=450/original=true/photo.jpeg",
            result,
        )
    }

    @Test
    fun thumbnailUrl_replaces_existing_width() {
        val img = image("https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/width=200/photo.jpeg")
        val result = img.thumbnailUrl(450)
        assertEquals(
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/width=450/photo.jpeg",
            result,
        )
    }

    @Test
    fun thumbnailUrl_returns_unchanged_for_non_civitai_url() {
        val img = image("https://example.com/image.png")
        assertEquals("https://example.com/image.png", img.thumbnailUrl())
    }

    // -- stripCdnWidth --

    @Test
    fun stripCdnWidth_removes_width_segment() {
        val url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/width=450/photo.jpeg"
        assertEquals(
            "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/photo.jpeg",
            url.stripCdnWidth(),
        )
    }

    @Test
    fun stripCdnWidth_noop_when_no_width() {
        val url = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/original=true/photo.jpeg"
        assertEquals(url, url.stripCdnWidth())
    }

    @Test
    fun stripCdnWidth_noop_for_non_civitai_url() {
        val url = "https://example.com/width=450/image.png"
        assertEquals(url, url.stripCdnWidth())
    }

    // -- Roundtrip: thumbnailUrl -> stripCdnWidth should recover comparable URL --

    @Test
    fun thumbnailUrl_then_stripCdnWidth_matches_original_stripped() {
        val rawUrl = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/abc-123/original=true/photo.jpeg"
        val img = image(rawUrl)
        val thumbnailUrl = img.thumbnailUrl(450)

        assertEquals(rawUrl.stripCdnWidth(), thumbnailUrl.stripCdnWidth())
    }
}
