package com.riox432.civitdeck.data.image

import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.data.repository.testJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [SaveGeneratedImageUseCase] downloads bytes and forwards them to [ImageSaver],
 * returning the saver's result, and returns false (without throwing) when the download fails.
 */
class SaveGeneratedImageUseCaseTest {

    private class RecordingImageSaver(private val result: Boolean = true) : ImageSaver {
        var lastBytes: ByteArray? = null
        var lastFilename: String? = null
        override suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean {
            lastBytes = imageBytes
            lastFilename = filename
            return result
        }
    }

    private fun provider(
        sharedClient: HttpClient,
        dao: FakeComfyUIConnectionDao = FakeComfyUIConnectionDao(),
        pinnedClient: HttpClient = sharedClient,
    ) = ComfyUIApiProvider(dao, sharedClient, testJson, createPinnedClient = { pinnedClient })

    @Test
    fun downloadsThroughThePinnedClient_whenTheUrlBelongsToAPinnedConnection() = runTest {
        val shared = HttpClient(MockEngine { respond(ByteReadChannel(byteArrayOf(9)), HttpStatusCode.OK) })
        val pinned = HttpClient(MockEngine { respond(ByteReadChannel(byteArrayOf(7)), HttpStatusCode.OK) })
        val dao = FakeComfyUIConnectionDao().apply {
            rows.add(
                ComfyUIConnectionEntity(
                    id = 1, name = "A", hostname = "comfy.local", port = 8443, createdAt = 1,
                    useHttps = true, acceptSelfSigned = true, tlsCertSha256 = "ab".repeat(32),
                ),
            )
        }
        val saver = RecordingImageSaver()
        val useCase = SaveGeneratedImageUseCase(provider(shared, dao, pinned), saver)

        useCase(url = "https://comfy.local:8443/view?filename=a.png&type=output")
        val pinnedBytes = saver.lastBytes
        useCase(url = "https://example.com/img.png")

        assertTrue(byteArrayOf(7).contentEquals(pinnedBytes))
        assertTrue(byteArrayOf(9).contentEquals(saver.lastBytes))
    }

    @Test
    fun downloadsBytesAndForwardsToSaver() = runTest {
        val payload = byteArrayOf(1, 2, 3, 4)
        val client = HttpClient(MockEngine { respond(ByteReadChannel(payload), HttpStatusCode.OK) })
        val saver = RecordingImageSaver(result = true)
        val useCase = SaveGeneratedImageUseCase(provider(client), saver)

        val success = useCase(url = "https://example.com/img.png", filename = "my_image")

        assertTrue(success)
        assertEquals("my_image", saver.lastFilename)
        assertTrue(payload.contentEquals(saver.lastBytes))
    }

    @Test
    fun returnsSaverResult_whenSaverReportsFailure() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel(byteArrayOf(0)), HttpStatusCode.OK) })
        val saver = RecordingImageSaver(result = false)
        val useCase = SaveGeneratedImageUseCase(provider(client), saver)

        val success = useCase(url = "https://example.com/img.png")

        assertFalse(success)
    }

    @Test
    fun returnsFalseWithoutThrowing_whenDownloadFails() = runTest {
        // Engine throws (e.g. network failure) so the use case must catch it.
        val client = HttpClient(MockEngine { throw RuntimeException("network down") })
        val saver = RecordingImageSaver()
        val useCase = SaveGeneratedImageUseCase(provider(client), saver)

        val success = useCase(url = "https://example.com/missing.png")

        // The use case swallows the exception and reports failure.
        assertFalse(success)
        // Saver must not have been invoked since the download threw.
        assertEquals(null, saver.lastBytes)
    }
}
