package com.riox432.civitdeck.ui.gallery

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object ImageDownloader {

    suspend fun download(context: Context, imageUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bytes = URL(imageUrl).readBytes()
                val fileName = extractFileName(imageUrl)
                saveToGallery(context, fileName, bytes)
            } catch (_: Exception) {
                false
            }
        }

    private fun extractFileName(url: String): String {
        val path = url.substringBefore("?").substringAfterLast("/")
        return if (path.contains(".")) path else "civitdeck_${System.currentTimeMillis()}.jpeg"
    }

    private fun saveToGallery(context: Context, fileName: String, bytes: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, fileName, bytes)
        } else {
            saveToExternalStorage(fileName, bytes)
        }
    }

    private fun saveWithMediaStore(
        context: Context,
        fileName: String,
        bytes: ByteArray,
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, guessMimeType(fileName))
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CivitDeck")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false

        return try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToExternalStorage(fileName: String, bytes: ByteArray): Boolean {
        return try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "CivitDeck",
            )
            dir.mkdirs()
            File(dir, fileName).writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast(".").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }
}
