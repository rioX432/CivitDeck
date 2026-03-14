package com.riox432.civitdeck.data.image

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImageSaver actual constructor() {

    actual suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val picturesDir = File(System.getProperty("user.home"), "Pictures/CivitDeck")
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, "$filename.jpg")
                file.writeBytes(imageBytes)
                true
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                false
            }
        }
}
