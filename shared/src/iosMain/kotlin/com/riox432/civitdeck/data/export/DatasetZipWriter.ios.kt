@file:OptIn(ExperimentalForeignApi::class)

package com.riox432.civitdeck.data.export

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile

actual class DatasetZipWriter actual constructor(outputPath: String) {
    private val chunks = mutableListOf<ByteArray>()
    private val entries = mutableListOf<ZipEntryRecord>()
    private val filePath = outputPath
    private var offset: Long = 0

    actual fun addEntry(name: String, data: ByteArray) {
        val nameBytes = name.encodeToByteArray()
        val crc = Crc32.compute(data)

        entries.add(
            ZipEntryRecord(
                name = name,
                crc32 = crc,
                compressedSize = data.size,
                uncompressedSize = data.size,
                localHeaderOffset = offset,
            ),
        )

        appendLocalFileHeader(nameBytes, data.size, crc)
        appendBytes(data)
    }

    actual fun close() {
        val centralDirOffset = offset
        for (entry in entries) {
            appendCentralDirectoryEntry(entry)
        }
        val centralDirSize = offset - centralDirOffset
        appendEndOfCentralDirectory(entries.size, centralDirSize, centralDirOffset)

        val totalSize = chunks.sumOf { it.size }
        val result = ByteArray(totalSize)
        var pos = 0
        for (chunk in chunks) {
            chunk.copyInto(result, pos)
            pos += chunk.size
        }
        result.usePinned { pinned ->
            val nsData = NSData.create(bytes = pinned.addressOf(0), length = result.size.toULong())
            nsData.writeToFile(filePath, atomically = true)
        }
        chunks.clear()
    }

    private fun appendLocalFileHeader(nameBytes: ByteArray, size: Int, crc: Int) {
        appendInt32(LOCAL_FILE_HEADER_SIG)
        appendInt16(VERSION_NEEDED)
        appendInt16(0) // flags
        appendInt16(0) // compression = STORE
        appendInt16(0) // mod time
        appendInt16(0) // mod date
        appendInt32(crc)
        appendInt32(size) // compressed size
        appendInt32(size) // uncompressed size
        appendInt16(nameBytes.size)
        appendInt16(0) // extra field length
        appendBytes(nameBytes)
    }

    private fun appendCentralDirectoryEntry(entry: ZipEntryRecord) {
        val nameBytes = entry.name.encodeToByteArray()
        appendInt32(CENTRAL_DIR_SIG)
        appendInt16(VERSION_MADE_BY)
        appendInt16(VERSION_NEEDED)
        appendInt16(0) // flags
        appendInt16(0) // compression = STORE
        appendInt16(0) // mod time
        appendInt16(0) // mod date
        appendInt32(entry.crc32)
        appendInt32(entry.compressedSize)
        appendInt32(entry.uncompressedSize)
        appendInt16(nameBytes.size)
        appendInt16(0) // extra field length
        appendInt16(0) // comment length
        appendInt16(0) // disk start
        appendInt16(0) // internal attrs
        appendInt32(0) // external attrs
        appendInt32(entry.localHeaderOffset.toInt())
        appendBytes(nameBytes)
    }

    private fun appendEndOfCentralDirectory(entryCount: Int, centralDirSize: Long, centralDirOffset: Long) {
        appendInt32(END_OF_CENTRAL_DIR_SIG)
        appendInt16(0) // disk number
        appendInt16(0) // central dir start disk
        appendInt16(entryCount)
        appendInt16(entryCount)
        appendInt32(centralDirSize.toInt())
        appendInt32(centralDirOffset.toInt())
        appendInt16(0) // comment length
    }

    private fun appendInt16(value: Int) {
        appendBytes(
            byteArrayOf(
                (value and BYTE_MASK).toByte(),
                (value shr BYTE_BITS and BYTE_MASK).toByte(),
            ),
        )
    }

    private fun appendInt32(value: Int) {
        appendBytes(
            byteArrayOf(
                (value and BYTE_MASK).toByte(),
                (value shr BYTE_BITS and BYTE_MASK).toByte(),
                (value shr SHIFT_16 and BYTE_MASK).toByte(),
                (value shr SHIFT_24 and BYTE_MASK).toByte(),
            ),
        )
    }

    private fun appendBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        chunks.add(bytes)
        offset += bytes.size
    }
}

private data class ZipEntryRecord(
    val name: String,
    val crc32: Int,
    val compressedSize: Int,
    val uncompressedSize: Int,
    val localHeaderOffset: Long,
)

private const val LOCAL_FILE_HEADER_SIG = 0x04034b50.toInt()
private const val CENTRAL_DIR_SIG = 0x02014b50.toInt()
private const val END_OF_CENTRAL_DIR_SIG = 0x06054b50.toInt()
private const val VERSION_NEEDED = 20
private const val VERSION_MADE_BY = 20
private const val BYTE_MASK = 0xFF
private const val BYTE_BITS = 8
private const val SHIFT_16 = 16
private const val SHIFT_24 = 24

private object Crc32 {
    private val table = IntArray(CRC_TABLE_SIZE).also { tbl ->
        for (n in 0 until CRC_TABLE_SIZE) {
            var c = n
            repeat(BYTE_BITS) {
                c = if (c and 1 != 0) CRC_POLYNOMIAL xor (c ushr 1) else c ushr 1
            }
            tbl[n] = c
        }
    }

    fun compute(data: ByteArray): Int {
        var crc = 0.inv()
        for (byte in data) {
            crc = table[(crc xor byte.toInt()) and BYTE_MASK] xor (crc ushr BYTE_BITS)
        }
        return crc.inv()
    }
}

private const val CRC_TABLE_SIZE = 256
private const val CRC_POLYNOMIAL = 0xEDB88320.toInt()

actual fun getExportCacheDirectory(): String {
    val tmp = NSTemporaryDirectory()
    val dir = "${tmp}dataset_exports"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return dir
}
