package com.riox432.civitdeck.data.export

expect class DatasetZipWriter(outputPath: String) {
    fun addEntry(name: String, data: ByteArray)
    fun close()
}

expect fun getExportCacheDirectory(): String
