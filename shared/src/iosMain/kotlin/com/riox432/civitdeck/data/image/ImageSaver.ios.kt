package com.riox432.civitdeck.data.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual class ImageSaver actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val nsData = imageBytes.toNSData()
                val image = UIImage(data = nsData) ?: return@withContext false
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
                true
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                false
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
