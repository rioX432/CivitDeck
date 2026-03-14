package com.riox432.civitdeck.data.export

import com.riox432.civitdeck.domain.model.ExportFormat
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import com.riox432.civitdeck.domain.repository.ExportRepository
import com.riox432.civitdeck.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

private const val TAG = "ExportRepositoryImpl"

class ExportRepositoryImpl(
    private val datasetRepo: DatasetCollectionRepository,
    private val httpClient: HttpClient,
) : ExportRepository {

    override fun exportDataset(datasetId: Long, format: ExportFormat): Flow<ExportProgress> = flow {
        emit(ExportProgress.Preparing)

        val collection = datasetRepo.observeCollections().first().firstOrNull { it.id == datasetId }
            ?: run {
                emit(ExportProgress.Failed("Dataset not found"))
                return@flow
            }

        val allImages = datasetRepo.observeImages(datasetId).first()
        val exportable = allImages.filter { it.trainable && !it.excluded }
        val warningCount = allImages.count { !it.trainable || it.excluded }

        if (exportable.isEmpty()) {
            emit(ExportProgress.Failed("No exportable images"))
            return@flow
        }

        val datasetDir = sanitizeName(collection.name)
        val outputDir = getExportCacheDirectory()
        val outputPath = "$outputDir/$datasetDir.zip"

        try {
            val zipWriter = DatasetZipWriter(outputPath)
            exportable.forEachIndexed { index, image ->
                emit(ExportProgress.Downloading(index + 1, exportable.size))
                val imageBytes = httpClient.get(image.imageUrl).bodyAsBytes()
                val ext = extractExtension(image.imageUrl)
                val baseName = buildEntryName(index + 1, image.caption?.text)

                zipWriter.addEntry("$datasetDir/images/$baseName.$ext", imageBytes)
                zipWriter.addEntry(
                    "$datasetDir/images/$baseName.txt",
                    (image.caption?.text ?: "").encodeToByteArray(),
                )
            }

            emit(ExportProgress.WritingManifest)
            val manifest = buildManifest(exportable, datasetDir)
            zipWriter.addEntry("$datasetDir/manifest.jsonl", manifest.encodeToByteArray())
            zipWriter.close()

            emit(ExportProgress.Completed(outputPath, warningCount))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e(TAG, "Export failed for dataset $datasetId: ${e.message}")
            emit(ExportProgress.Failed(e.message ?: "Export failed"))
        }
    }
}

private fun sanitizeName(name: String): String =
    name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(MAX_DIR_NAME_LENGTH)

private fun extractExtension(url: String): String {
    val path = url.substringBefore('?')
    val ext = path.substringAfterLast('.', "").lowercase()
    return if (ext in SUPPORTED_EXTENSIONS) ext else "png"
}

private fun buildEntryName(index: Int, captionText: String?): String {
    val prefix = index.toString().padStart(ENTRY_INDEX_PAD, '0')
    if (captionText.isNullOrBlank()) return "${prefix}_image"
    val slug = captionText.take(CAPTION_SLUG_MAX_LENGTH)
        .replace(Regex("[^a-zA-Z0-9 ]"), "")
        .trim()
        .replace(Regex("\\s+"), "_")
        .lowercase()
    return if (slug.isEmpty()) "${prefix}_image" else "${prefix}_$slug"
}

private fun buildManifest(
    images: List<com.riox432.civitdeck.domain.model.DatasetImage>,
    datasetDir: String,
): String = buildString {
    images.forEachIndexed { index, image ->
        val ext = extractExtension(image.imageUrl)
        val baseName = buildEntryName(index + 1, image.caption?.text)
        val tags = image.tags.joinToString(",") { "\"${escapeJson(it.tag)}\"" }
        appendLine(
            """{"image_path":"$datasetDir/images/$baseName.$ext",""" +
                """"caption":"${escapeJson(image.caption?.text ?: "")}",""" +
                """"tags":[$tags],""" +
                """"source":"${image.sourceType.name}",""" +
                """"trainable":${image.trainable}}""",
        )
    }
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

private val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
private const val MAX_DIR_NAME_LENGTH = 64
private const val ENTRY_INDEX_PAD = 3
private const val CAPTION_SLUG_MAX_LENGTH = 40
