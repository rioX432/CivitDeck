package com.riox432.civitdeck.data.export

import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DatasetZipWriterImpl internal constructor(
    outputPath: String,
    openStream: (Path) -> OutputStream,
    publish: (Path, Path) -> Unit,
) : DatasetZipWriter {
    constructor(outputPath: String) : this(
        outputPath,
        { Files.newOutputStream(it) },
        { staged, target -> Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE) },
    )

    private val target = File(outputPath).toPath().toAbsolutePath()
    private val staged = Files.createTempFile(target.parent, ".dataset-export-", ".tmp")
    private val outputStream = try {
        openStream(staged)
    } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
        try {
            Files.deleteIfExists(staged)
        } catch (@Suppress("TooGenericExceptionCaught") cleanup: Exception) {
            failure.addSuppressed(cleanup)
        }
        throw failure
    }
    private val zipOutputStream = ZipOutputStream(outputStream)
    private val transaction = DatasetZipTransaction(
        publish = {
            zipOutputStream.close()
            // No non-atomic fallback: unsupported replacement must leave the previous ZIP intact.
            publish(staged, target)
        },
        discard = { discardStaging() },
    )

    override fun addEntry(name: String, data: ByteArray) = transaction.write {
        zipOutputStream.putNextEntry(ZipEntry(name))
        zipOutputStream.write(data)
        zipOutputStream.closeEntry()
    }

    override fun close() = transaction.close()

    override fun abort() = transaction.abort()

    private fun discardStaging() {
        var failure: Exception? = null
        // ZIP finalization may throw before closing its underlying stream.
        val cleanup = listOf<() -> Unit>(
            { zipOutputStream.close() },
            { outputStream.close() },
            { Files.deleteIfExists(staged) },
        )
        for (action in cleanup) {
            try {
                action()
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                val previous = failure
                if (previous == null) failure = error else previous.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}

class DatasetZipWriterFactoryImpl : DatasetZipWriterFactory {
    override fun create(outputPath: String): DatasetZipWriter = DatasetZipWriterImpl(outputPath)
}

class ExportPathProviderImpl : ExportPathProvider {
    override fun getExportCacheDirectory(): String {
        val dir = File(System.getProperty("java.io.tmpdir"), "civitdeck_dataset_exports")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
}
