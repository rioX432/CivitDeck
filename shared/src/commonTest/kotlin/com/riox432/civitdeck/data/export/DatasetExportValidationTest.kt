package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.data.api.ApiKeyProvider
import com.riox432.civitdeck.data.api.DATASET_MEDIA_CLIENT
import com.riox432.civitdeck.data.api.RetryConfig
import com.riox432.civitdeck.data.api.createDatasetMediaHttpClient
import com.riox432.civitdeck.data.api.createHttpClient
import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatasetExportValidationTest : DatasetExportTestFixture() {
    @Test
    fun regression_nonCivitaiInitialOriginHasNoAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        client(
            ApiKeyProvider().apply { apiKey = sentinel },
            seen
        ).use { it.get("https://media.example.test/sample.png") }
        assertEquals(1, seen.size)
        assertNull(seen.single().authorization)
    }

    @Test
    fun regression_exportGeneratedImageHasNoCivitaiAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        val writer = Writer()
        client(ApiKeyProvider().apply { apiKey = sentinel }, seen).use {
            val events = repo(
                it,
                writer,
                listOf(image(), image(2, trainable = false), image(3, excluded = true))
            ).exportDataset(1, ExportFormat.ZIP).toList()
            assertEquals(2, (events.last() as ExportProgress.Completed).warningCount)
            assertEquals(3, writer.entries.size)
            assertTrue(writer.closed)
        }
        assertEquals(1, seen.size)
        assertEquals("https://comfy.example.test/view?filename=sample.png", seen.single().url)
        assertNull(seen.single().authorization)
    }

    @Test
    fun regression_lookalikeHostHasNoAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        client(
            ApiKeyProvider().apply {
                apiKey = sentinel
            },
            seen
        ).use { it.get("https://civitai.com.example.test/api/v1/models") }
        assertNull(seen.single().authorization)
    }

    @Test
    fun regression_plainHttpHasNoAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        client(ApiKeyProvider().apply { apiKey = sentinel }, seen).use { it.get("http://civitai.com/api/v1/models") }
        assertNull(seen.single().authorization)
    }

    @Test
    fun regression_unexpectedPortHasNoAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        client(
            ApiKeyProvider().apply { apiKey = sentinel },
            seen
        ).use { it.get("https://civitai.com:8443/api/v1/models") }
        assertNull(seen.single().authorization)
    }

    @Test
    fun control_intendedApiReceivesSentinelAndClearedKeyIsNotStale() = runTest {
        val seen = mutableListOf<Seen>()
        val provider = ApiKeyProvider().apply { apiKey = sentinel }
        client(provider, seen).use {
            it.get("https://civitai.com/api/v1/models")
            provider.apiKey = null
            it.get("https://civitai.com/api/v1/models")
            it.get("https://comfy.example.test/view")
        }
        assertEquals(listOf("Bearer $sentinel", null, null), seen.map { it.authorization })
    }

    @Test
    fun control_nullKeyExportSucceedsWithoutAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        val writer = Writer()
        client(ApiKeyProvider(), seen).use {
            val events = repo(it, writer).exportDataset(1, ExportFormat.ZIP).toList()
            assertTrue(events.last() is ExportProgress.Completed)
            assertTrue(writer.closed)
            assertEquals(3, writer.entries.size)
        }
        assertNull(seen.single().authorization)
    }

    @Test
    fun control_crossOriginRedirectStripsAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        val engine = MockEngine { request ->
            check(request.url.host in setOf("civitai.com", "media.example.test"))
            seen += Seen(request.url.toString(), request.headers[HttpHeaders.Authorization])
            if (request.url.host == "civitai.com") {
                respond(
                    "",
                    HttpStatusCode.Found,
                    headersOf(HttpHeaders.Location, "https://media.example.test/sample.png")
                )
            } else {
                respond(byteArrayOf(1), HttpStatusCode.OK)
            }
        }
        createHttpClient(
            engine,
            ApiKeyProvider().apply { apiKey = sentinel },
            retryConfig = RetryConfig(maxRetries = 0)
        ).use {
            it.get("https://civitai.com/api/v1/models")
        }
        assertEquals(2, seen.size)
        assertEquals("Bearer $sentinel", seen[0].authorization)
        assertNull(seen[1].authorization)
    }

    @Test
    fun regression_completedIsOnlyObservedAfterClose() = runTest {
        val writer = Writer()
        var closedAtCompleted: Boolean? = null
        client(ApiKeyProvider(), mutableListOf()).use {
            repo(it, writer).exportDataset(1, ExportFormat.ZIP).collect { event ->
                if (event is ExportProgress.Completed) closedAtCompleted = writer.closed
            }
        }
        assertTrue(writer.closed)
        assertEquals(true, closedAtCompleted)
    }

    @Test
    fun regression_closeFailureNeverEmitsCompleted() = runTest {
        val events = mutableListOf<ExportProgress>()
        val writer = Writer(failClose = true)
        client(ApiKeyProvider(), mutableListOf()).use {
            repo(it, writer).exportDataset(1, ExportFormat.ZIP).toList(events)
            assertEquals("synthetic-close-failure", (events.last() as ExportProgress.Failed).message)
        }
        assertTrue(events.none { it is ExportProgress.Completed })
    }

    @Test
    fun regression_manifestEscapesTabAndCarriageReturn() = runTest {
        val writer = Writer()
        client(ApiKeyProvider(), mutableListOf()).use {
            repo(
                it,
                writer,
                listOf(image(caption = "日本語 😀\tcaption\r\nquote \" slash \\"))
            ).exportDataset(1, ExportFormat.ZIP).toList()
        }
        val manifest = writer.entries.entries.single { it.key.endsWith("manifest.jsonl") }.value.decodeToString()
        assertFalse(manifest.contains('\t'), "A literal tab is invalid inside a JSON string")
        assertFalse(manifest.contains('\r'), "A literal CR is invalid inside a JSON string")
    }

    @Test
    fun control_sameOriginRedirectPreservesIntendedApiAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        val engine = MockEngine { request ->
            check(request.url.host == "civitai.com")
            seen += Seen(request.url.toString(), request.headers[HttpHeaders.Authorization])
            if (request.url.encodedPath.endsWith("/models")) {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://civitai.com/api/v1/images"))
            } else {
                respond(byteArrayOf(1), HttpStatusCode.OK)
            }
        }
        createHttpClient(
            engine,
            ApiKeyProvider().apply { apiKey = sentinel },
            retryConfig = RetryConfig(maxRetries = 0)
        ).use {
            it.get("https://civitai.com/api/v1/models")
        }
        assertEquals(listOf("Bearer $sentinel", "Bearer $sentinel"), seen.map { it.authorization })
    }

    @Test
    fun regression_actualDataModuleUsesCredentialFreeMediaForPluginExport() = runTest {
        for (url in listOf(
            "https://comfy.example.test/view?filename=sample.png",
            "https://civitai.com/api/v1/images",
            "https://civitai.com.example.test/sample.png"
        )) {
            val mediaSeen = mutableListOf<Seen>()
            val globalSeen = mutableListOf<Seen>()
            val writer = Writer()
            val global = client(ApiKeyProvider().apply { apiKey = sentinel }, globalSeen)
            val media = createDatasetMediaHttpClient(
                MockEngine { request ->
                    check(request.url.toString() == url)
                    mediaSeen += Seen(request.url.toString(), request.headers[HttpHeaders.Authorization])
                    respond(byteArrayOf(1, 2, 3, 4), HttpStatusCode.OK)
                }
            )
            val app = org.koin.dsl.koinApplication {
                allowOverride(true)
                modules(
                    com.riox432.civitdeck.di.dataModule,
                    org.koin.dsl.module {
                        single<HttpClient> { global }
                        single<HttpClient>(org.koin.core.qualifier.named(DATASET_MEDIA_CLIENT)) { media }
                        single<DatasetCollectionRepository> {
                            FakeDatasetRepo(
                                listOf(DatasetCollection(1, "clean_room", createdAt = 0, updatedAt = 0)),
                                listOf(image().copy(imageUrl = url))
                            )
                        }
                        single<DatasetZipWriterFactory> {
                            object : DatasetZipWriterFactory { override fun create(outputPath: String) = writer }
                        }
                        single<ExportPathProvider> {
                            object : ExportPathProvider {
                                override fun getExportCacheDirectory() = "/synthetic/no-file-written"
                            }
                        }
                    }
                )
            }
            try {
                val plugin = app.koin.get<KohyaZipExportPlugin>()
                val events = plugin.export(1, KohyaZipExportPlugin.FORMAT_ID, emptyMap()).toList()
                assertTrue(events.last() is com.riox432.civitdeck.plugin.PluginExportProgress.Completed)
                assertTrue(globalSeen.isEmpty(), "Export must never use the global authenticated client")
                assertEquals(1, mediaSeen.size)
                assertNull(mediaSeen.single().authorization)
            } finally {
                app.close()
                global.close()
                media.close()
            }
        }
    }

    @Test
    fun regression_mediaRedirectNeverAcquiresGlobalAuthorization() = runTest {
        val seen = mutableListOf<Seen>()
        val engine = MockEngine { request ->
            check(request.url.host in setOf("media.example.test", "civitai.com"))
            seen += Seen(request.url.toString(), request.headers[HttpHeaders.Authorization])
            if (request.url.host == "media.example.test") {
                respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://civitai.com/api/v1/images"))
            } else {
                respond(byteArrayOf(1), HttpStatusCode.OK)
            }
        }
        createDatasetMediaHttpClient(engine).use { it.get("https://media.example.test/sample.png") }
        assertEquals(2, seen.size)
        assertTrue(seen.all { it.authorization == null })
    }

    @Test
    fun regression_manifestControlCharactersRoundTrip() = runTest {
        val caption = "日本語 😀\tcaption\r\nquote \" slash \\"
        val writer = Writer()
        client(ApiKeyProvider(), mutableListOf()).use {
            repo(it, writer, listOf(image(caption = caption))).exportDataset(1, ExportFormat.ZIP).toList()
        }
        val manifest = writer.entries.entries.single { it.key.endsWith("manifest.jsonl") }.value.decodeToString()
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(
            manifest.trim()
        ) as kotlinx.serialization.json.JsonObject
        assertEquals(caption, (obj["caption"] as kotlinx.serialization.json.JsonPrimitive).content)
    }

    @Test
    fun regression_cancellationPropagatesAndAbortsWriter() = runTest {
        val writer = Writer()
        val httpClient = createDatasetMediaHttpClient(
            MockEngine {
                throw kotlinx.coroutines.CancellationException("synthetic-cancel")
            }
        )
        val events = mutableListOf<ExportProgress>()
        httpClient.use {
            assertFailsWith<kotlinx.coroutines.CancellationException> {
                repo(it, writer).exportDataset(1, ExportFormat.ZIP).toList(events)
            }
        }
        assertTrue(writer.aborted)
        assertFalse(writer.closed)
        assertTrue(events.none { it is ExportProgress.Completed || it is ExportProgress.Failed })
    }

    @Test
    fun regression_equivalentCanonicalOriginStillAuthenticates() = runTest {
        val seen = mutableListOf<String?>()
        val engine = MockEngine { request ->
            check(request.url.host.equals("civitai.com", ignoreCase = true))
            seen += request.headers[HttpHeaders.Authorization]
            respond(byteArrayOf(1), HttpStatusCode.OK)
        }
        createHttpClient(engine, ApiKeyProvider().apply { apiKey = sentinel }).use {
            it.get("https://CIVITAI.COM:443/api/v1/models")
        }
        assertEquals(listOf<String?>("Bearer $sentinel"), seen)
    }

    private suspend fun assertRejectedMedia(status: HttpStatusCode, body: String, contentType: String) {
        val writer = Writer()
        val httpClient = createDatasetMediaHttpClient(
            MockEngine { request ->
                check(request.url.host == "comfy.example.test")
                respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
            }
        )
        httpClient.use {
            val events = repo(it, writer).exportDataset(1, ExportFormat.ZIP).toList()
            assertTrue(
                events.none { event -> event is ExportProgress.Completed },
                "HTTP $status / $contentType / ${body.length} bytes must not complete"
            )
            assertTrue(events.last() is ExportProgress.Failed)
        }
    }

    @Test
    fun regression_unauthorizedMediaFailsWithoutCompleted() = runTest {
        assertRejectedMedia(HttpStatusCode.Unauthorized, "synthetic access denied", "text/html")
    }

    @Test
    fun regression_notFoundMediaFailsWithoutCompleted() = runTest {
        assertRejectedMedia(HttpStatusCode.NotFound, "synthetic missing image", "text/html")
    }

    @Test
    fun regression_explicitHtmlIsNotExportedAsAnImage() = runTest {
        assertRejectedMedia(HttpStatusCode.OK, "<html>synthetic login</html>", "text/html; charset=utf-8")
    }

    @Test
    fun regression_zeroByteImageFailsWithoutCompleted() = runTest {
        assertRejectedMedia(HttpStatusCode.OK, "", "image/png")
    }
}
