package com.riox432.civitdeck.data.scanner

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.CommonCrypto.CC_SHA256_CTX
import platform.CommonCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CommonCrypto.CC_SHA256_Final
import platform.CommonCrypto.CC_SHA256_Init
import platform.CommonCrypto.CC_SHA256_Update
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSInputStream
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalForeignApi::class)
actual class FileScanner actual constructor() {

    actual suspend fun scanDirectory(
        path: String,
        onProgress: (current: Int, total: Int) -> Unit,
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val fm = NSFileManager.defaultManager
        val contents = fm.contentsOfDirectoryAtPath(path, null) ?: return@withContext emptyList()

        val modelFiles = contents.filterIsInstance<String>()
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
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun computeSha256(filePath: String): String {
    val ctx = nativeHeap.alloc<CC_SHA256_CTX>()
    CC_SHA256_Init(ctx.ptr)

    val inputStream = NSInputStream(fileAtPath = filePath)
    inputStream.open()

    val bufferSize = 8192
    val buffer = ByteArray(bufferSize)
    try {
        while (inputStream.hasBytesAvailable) {
            val bytesRead = buffer.usePinned { pinned ->
                inputStream.read(pinned.addressOf(0).reinterpret(), bufferSize.toULong())
            }
            if (bytesRead <= 0) break
            buffer.usePinned { pinned ->
                CC_SHA256_Update(ctx.ptr, pinned.addressOf(0), bytesRead.toUInt())
            }
        }
    } finally {
        inputStream.close()
    }

    val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
    hash.usePinned { pinned ->
        CC_SHA256_Final(pinned.addressOf(0).reinterpret(), ctx.ptr)
    }
    nativeHeap.free(ctx)

    return hash.joinToString("") { byte ->
        val unsigned = byte.toInt() and 0xFF
        unsigned.toString(16).padStart(2, '0')
    }
}
