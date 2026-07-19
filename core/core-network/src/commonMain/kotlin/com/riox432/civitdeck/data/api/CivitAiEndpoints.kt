package com.riox432.civitdeck.data.api

/**
 * Base URLs for the CivitAI backend, injected into [CivitAiApi].
 *
 * Production points at civitai.com. The E2E/QA path replaces these with a local fixture
 * server so the discovery flow can be exercised deterministically (issue #990). The value
 * object is the single auditable seam for that swap — release builds always resolve
 * [Production] and never read any runtime override.
 */
data class CivitAiEndpoints(
    val apiBaseUrl: String,
    val trpcBaseUrl: String,
) {
    companion object {
        val Production = CivitAiEndpoints(
            apiBaseUrl = "https://civitai.com/api/v1",
            trpcBaseUrl = "https://civitai.com/api/trpc",
        )
    }
}
