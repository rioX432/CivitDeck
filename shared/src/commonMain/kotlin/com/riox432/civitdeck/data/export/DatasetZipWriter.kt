package com.riox432.civitdeck.data.export

/**
 * Writes ZIP entries to a platform-specific output path. Created via
 * [DatasetZipWriterFactory] so consumers receive their dependencies through
 * Koin constructor injection instead of constructing the platform class directly.
 * Each instance has one owner. Concurrent instances use independent staging;
 * the last successful publication to a path wins. Abort never removes published output.
 * Cleanup is best-effort if the filesystem denies deletion; this is not crash recovery.
 */
interface DatasetZipWriter {
    fun addEntry(name: String, data: ByteArray)

    /** Finalizes and atomically publishes the archive. Never call this for failure cleanup. */
    fun close()

    /** Discards unpublished work; a successfully published archive is never removed. */
    fun abort()
}

/** Factory for [DatasetZipWriter] instances, bound per platform via Koin. */
interface DatasetZipWriterFactory {
    fun create(outputPath: String): DatasetZipWriter
}

/** Resolves the platform-specific cache directory used for dataset exports. */
interface ExportPathProvider {
    fun getExportCacheDirectory(): String
}
