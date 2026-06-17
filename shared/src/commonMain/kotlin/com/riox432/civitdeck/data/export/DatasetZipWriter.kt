package com.riox432.civitdeck.data.export

/**
 * Writes ZIP entries to a platform-specific output path. Created via
 * [DatasetZipWriterFactory] so consumers receive their dependencies through
 * Koin constructor injection instead of constructing the platform class directly.
 */
interface DatasetZipWriter {
    fun addEntry(name: String, data: ByteArray)
    fun close()
}

/** Factory for [DatasetZipWriter] instances, bound per platform via Koin. */
interface DatasetZipWriterFactory {
    fun create(outputPath: String): DatasetZipWriter
}

/** Resolves the platform-specific cache directory used for dataset exports. */
interface ExportPathProvider {
    fun getExportCacheDirectory(): String
}
