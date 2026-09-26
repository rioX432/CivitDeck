package com.riox432.civitdeck.domain.model

enum class DownloadStatus { Pending, Downloading, Paused, Completed, Failed, Cancelled }

data class ModelDownload(
    val id: Long = 0,
    val modelId: Long,
    val modelName: String,
    val versionId: Long,
    val versionName: String,
    val fileId: Long,
    val fileName: String,
    val fileUrl: String,
    val fileSizeBytes: Long,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.Pending,
    val modelType: String,
    val destinationPath: String? = null,
    val errorMessage: String? = null,
    val expectedSha256: String? = null,
    val hashVerified: Boolean? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

private val AUTH_REQUIRED_HTTP_FAILURES = setOf("HTTP 401", "HTTP 403")

/**
 * Whether CivitAI rejected this download for missing or insufficient credentials.
 *
 * Writers must record an HTTP failure's [ModelDownload.errorMessage] as `HTTP <status code>`
 * (for example `HTTP 401`), with nothing before or after it. Only a [DownloadStatus.Failed]
 * download whose message is exactly `HTTP 401` or `HTTP 403` is classified; any other text,
 * including `null`, is not. 403 is included because a valid key without access to a paid or
 * early-access file may be rejected with it.
 */
fun ModelDownload.isAuthRequiredFailure(): Boolean =
    status == DownloadStatus.Failed && errorMessage in AUTH_REQUIRED_HTTP_FAILURES
