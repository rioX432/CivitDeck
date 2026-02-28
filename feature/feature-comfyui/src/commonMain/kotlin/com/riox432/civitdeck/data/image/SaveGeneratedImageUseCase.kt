package com.riox432.civitdeck.data.image

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

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
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            false
        }
    }
}
