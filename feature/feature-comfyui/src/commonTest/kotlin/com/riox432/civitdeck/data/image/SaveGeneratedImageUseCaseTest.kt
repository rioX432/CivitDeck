package com.riox432.civitdeck.data.image

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

    @Test
    fun downloadsBytesAndForwardsToSaver() = runTest {
        val payload = byteArrayOf(1, 2, 3, 4)
        val client = HttpClient(MockEngine { respond(ByteReadChannel(payload), HttpStatusCode.OK) })
        val saver = RecordingImageSaver(result = true)
        val useCase = SaveGeneratedImageUseCase(client, saver)

        val success = useCase(url = "https://example.com/img.png", filename = "my_image")

        assertTrue(success)
        assertEquals("my_image", saver.lastFilename)
        assertTrue(payload.contentEquals(saver.lastBytes))
    }

    @Test
    fun returnsSaverResult_whenSaverReportsFailure() = runTest {
        val client = HttpClient(MockEngine { respond(ByteReadChannel(byteArrayOf(0)), HttpStatusCode.OK) })
        val saver = RecordingImageSaver(result = false)
        val useCase = SaveGeneratedImageUseCase(client, saver)

        val success = useCase(url = "https://example.com/img.png")

        assertFalse(success)
    }

    @Test
    fun returnsFalseWithoutThrowing_whenDownloadFails() = runTest {
        // Engine throws (e.g. network failure) so the use case must catch it.
        val client = HttpClient(MockEngine { throw RuntimeException("network down") })
        val saver = RecordingImageSaver()
        val useCase = SaveGeneratedImageUseCase(client, saver)

        val success = useCase(url = "https://example.com/missing.png")

        // The use case swallows the exception and reports failure.
        assertFalse(success)
        // Saver must not have been invoked since the download threw.
        assertEquals(null, saver.lastBytes)
    }
}
