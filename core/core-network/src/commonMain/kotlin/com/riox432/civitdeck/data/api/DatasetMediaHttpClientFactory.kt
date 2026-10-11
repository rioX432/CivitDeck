package com.riox432.civitdeck.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

const val DATASET_MEDIA_CLIENT = "datasetMedia"

/**
 * Fetches arbitrary dataset media without access to the application's CivitAI key.
 * Authenticated external media requires its own explicit source-scoped policy.
 */
fun createDatasetMediaHttpClient(): HttpClient = createHttpClient(ApiKeyProvider())

/** Engine injection shares the same credential-free production configuration. */
fun createDatasetMediaHttpClient(engine: HttpClientEngine): HttpClient =
    createHttpClient(engine, ApiKeyProvider())
