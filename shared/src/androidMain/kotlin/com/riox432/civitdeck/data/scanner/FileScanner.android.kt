package com.riox432.civitdeck.data.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import com.riox432.civitdeck.util.Logger
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

actual class FileScanner actual constructor() {

    actual suspend fun scanDirectory(
        path: String,
        onProgress: (current: Int, total: Int) -> Unit,
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val modelFiles = dir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in MODEL_FILE_EXTENSIONS }
            .toList()

        val total = modelFiles.size
        modelFiles.mapIndexedNotNull { index, file ->
            coroutineContext.ensureActive()
            onProgress(index + 1, total)
            try {
                ScannedFile(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    sha256Hash = computeSha256(file),
                    sizeBytes = file.length(),
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("FileScanner", "Failed to scan file ${file.absolutePath}, skipping: ${e.message}")
                null
            }
        }
    }
}

private fun computeSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_SIZE)
    file.inputStream().use { input ->
        var bytesRead = input.read(buffer)
        while (bytesRead != -1) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = input.read(buffer)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private const val BUFFER_SIZE = 8192
