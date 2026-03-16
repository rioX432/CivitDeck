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
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
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
        val mimeType = guessMimeType(fileName)
        val isVideo = mimeType.startsWith("video/")
        val contentUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val relativePath = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/CivitDeck"
        } else {
            "${Environment.DIRECTORY_PICTURES}/CivitDeck"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(contentUri, values) ?: return false

        return try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }

    // TODO: Remove this fallback when minSdk is raised to 29 (Q).
    //  MediaStore with RELATIVE_PATH/IS_PENDING requires API 29+,
    //  so pre-Q devices (API 24-28) still need the legacy file API.
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
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            false
        }
    }

    private fun guessMimeType(fileName: String): String {
        return when (fileName.substringAfterLast(".").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            else -> "image/jpeg"
        }
    }
}
