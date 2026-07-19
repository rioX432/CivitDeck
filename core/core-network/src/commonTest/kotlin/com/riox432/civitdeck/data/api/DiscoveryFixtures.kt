package com.riox432.civitdeck.data.api

/**
 * Recorded CivitAI responses used to drive the discovery flow deterministically (issue #990).
 *
 * These are trimmed captures of the real `/api/v1/models` shape. Embedding them as string
 * constants (rather than classpath resources) keeps the fixtures portable across every KMP
 * test target, including Kotlin/Native where classpath resource loading is not available.
 * The same JSON payloads back the Android/iOS E2E fixture server.
 */
internal object DiscoveryFixtures {

    /** Two-item models page. IDs and order are fixed so ranking assertions are stable. */
    val modelsPage = """
        {
          "items": [
            {
              "id": 101,
              "name": "Anime Diffusion XL",
              "type": "Checkpoint",
              "nsfw": false,
              "tags": ["anime", "style"],
              "creator": { "username": "fixture_creator" },
              "stats": { "downloadCount": 12000, "thumbsUpCount": 800, "rating": 4.8 },
              "modelVersions": []
            },
            {
              "id": 102,
              "name": "Realistic Vision",
              "type": "Checkpoint",
              "nsfw": false,
              "tags": ["photorealistic", "base model"],
              "creator": { "username": "fixture_creator" },
              "stats": { "downloadCount": 9000, "thumbsUpCount": 600, "rating": 4.6 },
              "modelVersions": []
            }
          ],
          "metadata": { "totalItems": 2, "currentPage": 1, "pageSize": 20, "nextPage": null }
        }
    """.trimIndent()
}
