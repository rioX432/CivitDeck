package com.riox432.civitdeck.data.export

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

private object ExportDirProvider : KoinComponent {
    val context: Context by inject()
}

actual fun getExportCacheDirectory(): String {
    val dir = File(ExportDirProvider.context.cacheDir, "dataset_exports")
    if (!dir.exists()) dir.mkdirs()
    return dir.absolutePath
}
