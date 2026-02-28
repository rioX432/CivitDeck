package com.riox432.civitdeck.data.image

/**
 * Platform-specific implementation for saving image bytes to the device gallery.
 * Android: uses MediaStore (Pictures/CivitDeck).
 * iOS: uses PHPhotoLibrary.
 */
expect class ImageSaver() {
    /**
     * Saves the given [imageBytes] (JPEG/PNG raw bytes) to the device photo gallery.
     * @param imageBytes raw image data
     * @param filename desired filename hint (without extension)
     * @return true on success
     */
    suspend fun saveToGallery(imageBytes: ByteArray, filename: String): Boolean
}
