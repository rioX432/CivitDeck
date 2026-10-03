package com.riox432.civitdeck.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Builds an `ACTION_SEND` chooser that attaches a remote image, by downloading it into a
 * FileProvider-backed cache directory.
 */
object ShareImageIntent {

    private const val TAG = "ShareImageIntent"
    private const val SHARED_IMAGES_DIR = "shared_images"
    private const val FALLBACK_FILE_NAME = "shared_image"
    private const val FALLBACK_MIME_TYPE = "image/*"
    private const val MAX_FILE_NAME_LENGTH = 100
    private val UNSAFE_FILE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")
    private val httpClient = OkHttpClient()

    /** Returns the chooser intent, or null when the image could not be downloaded or stored. */
    suspend fun create(context: Context, imageUrl: String, text: String): Intent? =
        withContext(Dispatchers.IO) {
            try {
                val file = downloadToCache(context, imageUrl) ?: return@withContext null
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                buildChooser(uri, mimeTypeOf(file.name), text)
            } catch (e: IOException) {
                Log.w(TAG, "Failed to load image for sharing: $imageUrl", e)
                null
            } catch (e: IllegalArgumentException) {
                // Thrown by OkHttp for a malformed URL and by FileProvider for an unmapped path.
                Log.w(TAG, "Cannot share image: $imageUrl", e)
                null
            }
        }

    private fun CoroutineScope.downloadToCache(context: Context, imageUrl: String): File? {
        val request = Request.Builder().url(imageUrl).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                Log.w(TAG, "Image request returned HTTP ${response.code}: $imageUrl")
                return null
            }
            // execute() blocks through cancellation, so a share abandoned by closing the sheet can
            // finish downloading after a newer share started; it must not clear that share's file.
            ensureActive()
            return writeToCache(context, fileNameFrom(imageUrl), body.byteStream())
        }
    }

    private fun writeToCache(context: Context, fileName: String, input: InputStream): File {
        val dir = File(context.cacheDir, SHARED_IMAGES_DIR)
        // Only the latest shared image is kept, so the directory cannot grow without bound.
        dir.listFiles()?.forEach { it.delete() }
        dir.mkdirs()
        val file = File(dir, fileName)
        try {
            input.use { file.outputStream().use { output -> it.copyTo(output) } }
        } catch (e: IOException) {
            file.delete()
            throw e
        }
        return file
    }

    // ComfyUI `/view` URLs carry the real name in the `filename` query parameter, which can also
    // contain a subfolder or `..`, so only its last segment is kept and then reduced to safe chars.
    private fun fileNameFrom(imageUrl: String): String {
        val uri = Uri.parse(imageUrl)
        val raw = uri.getQueryParameter("filename")?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty()
        val name = raw.substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(UNSAFE_FILE_NAME_CHARS, "_")
            .takeLast(MAX_FILE_NAME_LENGTH)
            .trimStart('.')
        return name.ifBlank { FALLBACK_FILE_NAME }
    }

    private fun mimeTypeOf(fileName: String): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: FALLBACK_MIME_TYPE
    }

    private fun buildChooser(uri: Uri, mimeType: String, text: String): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "Share")
    }
}
