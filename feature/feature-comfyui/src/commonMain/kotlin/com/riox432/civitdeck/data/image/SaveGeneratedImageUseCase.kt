package com.riox432.civitdeck.data.image

import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.util.Logger
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess

private const val TAG = "SaveGeneratedImageUseCase"

/**
 * Downloads an image from [url] and saves it to the device gallery via [ImageSaver].
 * The download uses the TLS trust of the stored ComfyUI connection that serves [url].
 * Returns true on success, and false without saving when the response status is not 2xx.
 */
class SaveGeneratedImageUseCase(
    private val apiProvider: ComfyUIApiProvider,
    private val imageSaver: ImageSaver,
) {
    suspend operator fun invoke(url: String, filename: String = "civitdeck_gen"): Boolean {
        return try {
            val response = apiProvider.forUrl(url).httpClient.get(url)
            // The ComfyUI client does not set expectSuccess, so an error body would be saved as the image.
            if (response.status.isSuccess()) {
                imageSaver.saveToGallery(response.readBytes(), filename)
            } else {
                Logger.e(TAG, "Failed to save generated image: HTTP ${response.status.value}")
                false
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Failed to save generated image: ${e.message}", e)
            false
        }
    }
}
