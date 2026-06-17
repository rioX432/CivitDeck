package com.riox432.civitdeck.data.api

object CivitAiUrls {
    /**
     * Fixed host for the REST API and file downloads.
     *
     * This NEVER changes with the front-door setting: civitai.com and
     * civitai.red share the same account, database, and API. Only web/share
     * links switch between the two front doors (see [modelUrl]).
     */
    const val API_HOST = "https://civitai.com"

    /** Default web/share host (SFW front door). */
    const val DEFAULT_WEB_HOST = "https://civitai.com"

    /**
     * Web/share link to a model's page. The [webHost] reflects the user's
     * front-door choice (civitai.com / civitai.red) and is supplied by the
     * caller from `CivitAiFrontDoor`.
     */
    fun modelUrl(modelId: Long, webHost: String = DEFAULT_WEB_HOST) = "$webHost/models/$modelId"

    /** Download URL — always uses the fixed API host, never a front-door host. */
    fun downloadUrl(versionId: Long) = "$API_HOST/api/download/models/$versionId"
}
