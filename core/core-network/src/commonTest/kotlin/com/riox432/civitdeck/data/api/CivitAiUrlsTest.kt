package com.riox432.civitdeck.data.api

import kotlin.test.Test
import kotlin.test.assertEquals

class CivitAiUrlsTest {

    @Test
    fun modelUrlUsesDefaultWebHostWhenNoneProvided() {
        assertEquals("https://civitai.com/models/42", CivitAiUrls.modelUrl(42))
    }

    @Test
    fun modelUrlUsesProvidedFrontDoorWebHost() {
        assertEquals(
            "https://civitai.red/models/42",
            CivitAiUrls.modelUrl(42, webHost = "https://civitai.red"),
        )
    }

    @Test
    fun downloadUrlAlwaysUsesFixedApiHostRegardlessOfFrontDoor() {
        // Download must never switch to civitai.red — the file host is fixed.
        assertEquals(
            "https://civitai.com/api/download/models/7",
            CivitAiUrls.downloadUrl(7),
        )
        assertEquals("https://civitai.com", CivitAiUrls.API_HOST)
    }
}
