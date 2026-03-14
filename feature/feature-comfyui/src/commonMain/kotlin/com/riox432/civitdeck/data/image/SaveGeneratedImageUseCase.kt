package com.riox432.civitdeck.data.image

import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

private const val TAG = "SaveGeneratedImageUseCase"

/**
 * Downloads an image from [url] and saves it to the device gallery via [ImageSaver].
 * Returns true on success.
 */
class SaveGeneratedImageUseCase(
    private val httpClient: HttpClient,
    private val imageSaver: ImageSaver,
) {
    suspend operator fun invoke(url: String, filename: String = "civitdeck_gen"): Boolean {
        return try {
            val bytes = httpClient.get(url).readBytes()
            imageSaver.saveToGallery(bytes, filename)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Failed to save generated image: ${e.message}", e)
            false
        }
    }
}
