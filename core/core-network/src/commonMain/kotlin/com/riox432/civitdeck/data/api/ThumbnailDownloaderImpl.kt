package com.riox432.civitdeck.data.api

import com.riox432.civitdeck.domain.repository.ThumbnailDownloader
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes

/**
 * Ktor-backed implementation of [ThumbnailDownloader].
 *
 * Uses the existing CivitAI [HttpClient] for connection pooling and retry policy.
 * The response is read as raw bytes without JSON deserialization, so the
 * `ContentNegotiation` plugin is not invoked.
 */
class ThumbnailDownloaderImpl(
    private val client: HttpClient,
) : ThumbnailDownloader {

    override suspend fun download(url: String): ByteArray =
        client.get(url).readRawBytes()
}
