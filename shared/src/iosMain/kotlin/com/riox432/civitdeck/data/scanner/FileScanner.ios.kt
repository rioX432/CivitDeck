package com.riox432.civitdeck.data.scanner

import com.riox432.civitdeck.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalForeignApi::class)
actual class FileScanner actual constructor() {

    actual suspend fun scanDirectory(
        path: String,
        onProgress: (current: Int, total: Int) -> Unit,
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val fm = NSFileManager.defaultManager
        val subpaths = fm.subpathsOfDirectoryAtPath(path, null) ?: return@withContext emptyList()

        val modelFiles = subpaths.filterIsInstance<String>()
            .filter { name ->
                val ext = name.substringAfterLast('.', "").lowercase()
                ext in MODEL_FILE_EXTENSIONS
            }
            .map { "$path/$it" }

        val total = modelFiles.size
        modelFiles.mapIndexedNotNull { index, filePath ->
            coroutineContext.ensureActive()
            onProgress(index + 1, total)
            try {
                val attrs = fm.attributesOfItemAtPath(filePath, null) ?: return@mapIndexedNotNull null
                val size = (attrs[NSFileSize] as? Number)?.toLong() ?: 0L
                val fileName = filePath.substringAfterLast('/')
                ScannedFile(
                    filePath = filePath,
                    fileName = fileName,
                    sha256Hash = computeSha256(filePath),
                    sizeBytes = size,
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("FileScanner", "Failed to scan file $filePath, skipping: ${e.message}")
                null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun computeSha256(filePath: String): String {
    val digest = Sha256Digest()
    val inputStream = NSInputStream(uRL = NSURL.fileURLWithPath(filePath))
    inputStream.open()
    val bufferSize = 8192
    val buffer = ByteArray(bufferSize)
    try {
        while (inputStream.hasBytesAvailable) {
            val bytesRead = buffer.usePinned { pinned ->
                inputStream.read(pinned.addressOf(0).reinterpret(), bufferSize.toULong())
            }
            if (bytesRead <= 0) break
            digest.update(buffer, 0, bytesRead.toInt())
        }
    } finally {
        inputStream.close()
    }
    return Sha256Digest.digestToHex(digest.digest())
}
