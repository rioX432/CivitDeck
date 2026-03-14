package com.riox432.civitdeck.data.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual class DatasetZipWriter actual constructor(outputPath: String) {
    private val zipOutputStream = ZipOutputStream(FileOutputStream(outputPath))

    actual fun addEntry(name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(data)
        zipOutputStream.closeEntry()
    }

    actual fun close() {
        zipOutputStream.close()
    }
}

actual fun getExportCacheDirectory(): String {
    val dir = File(System.getProperty("java.io.tmpdir"), "civitdeck_dataset_exports")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}
