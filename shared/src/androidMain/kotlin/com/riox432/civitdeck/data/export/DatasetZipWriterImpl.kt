package com.riox432.civitdeck.data.export

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DatasetZipWriterImpl(outputPath: String) : DatasetZipWriter {
    private val zipOutputStream = ZipOutputStream(FileOutputStream(outputPath))

    override fun addEntry(name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        zipOutputStream.putNextEntry(entry)
        zipOutputStream.write(data)
        zipOutputStream.closeEntry()
    }

    override fun close() {
        zipOutputStream.close()
    }
}

class DatasetZipWriterFactoryImpl : DatasetZipWriterFactory {
    override fun create(outputPath: String): DatasetZipWriter = DatasetZipWriterImpl(outputPath)
}

class ExportPathProviderImpl(
    private val context: Context,
) : ExportPathProvider {
    override fun getExportCacheDirectory(): String {
        val dir = File(context.cacheDir, "dataset_exports")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
}
