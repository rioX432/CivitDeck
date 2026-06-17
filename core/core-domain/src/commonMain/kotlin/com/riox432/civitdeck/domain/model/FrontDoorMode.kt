package com.riox432.civitdeck.domain.model

/**
 * Controls which CivitAI front-door site web/share links open.
 *
 * In April 2026 CivitAI split into two front doors: civitai.com (SFW) and
 * civitai.red (full catalog). It is the same account, database, and API — the
 * split is a display/front-door layer only, with no separate API endpoints.
 *
 * This setting therefore affects ONLY web-facing links (model page, share,
 * "open in browser"). The REST API and download URLs always stay on
 * civitai.com regardless of this choice, and this setting is independent from
 * [NsfwFilterLevel].
 */
enum class FrontDoorMode(val webHost: String) {
    /** Open links on civitai.com (SFW front door). Default. */
    Sfw("https://civitai.com"),

    /** Open links on civitai.red (full catalog front door). */
    Full("https://civitai.red"),
}
