package com.riox432.civitdeck.domain.repository

/**
 * Downloads raw image bytes from a URL.
 *
 * Extracted as an interface so the domain layer stays free of Ktor / HTTP
 * dependencies. The implementation lives in `:core:core-network`.
 */
fun interface ThumbnailDownloader {
    /** Returns the raw JPEG/PNG bytes for [url], or throws on network failure. */
    suspend fun download(url: String): ByteArray
}
