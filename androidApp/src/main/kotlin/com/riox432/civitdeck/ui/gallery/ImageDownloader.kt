package com.riox432.civitdeck.ui.gallery

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ImageDownloader {

    private const val TAG = "ImageDownloader"
    private val httpClient = OkHttpClient()

    suspend fun download(context: Context, imageUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val bytes = downloadBytes(imageUrl) ?: return@withContext false
                val fileName = extractFileName(imageUrl)
                saveToGallery(context, fileName, bytes)
            } catch (e: IOException) {
                Log.w(TAG, "Failed to download image: $imageUrl", e)
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission denied during image download: $imageUrl", e)
                false
            }
        }

    private fun downloadBytes(url: String): ByteArray? {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        return response.body?.bytes()
    }

    private fun extractFileName(url: String): String {
        val path = url.substringBefore("?").substringAfterLast("/")
        return if (path.contains(".")) path else "civitdeck_${System.currentTimeMillis()}.jpeg"
    }

    private fun saveToGallery(context: Context, fileName: String, bytes: ByteArray): Boolean {
        return saveWithMediaStore(context, fileName, bytes)
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
        } catch (e: IOException) {
            Log.w(TAG, "Failed to write image via MediaStore: $fileName", e)
            resolver.delete(uri, null, null)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied writing to MediaStore: $fileName", e)
            resolver.delete(uri, null, null)
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
