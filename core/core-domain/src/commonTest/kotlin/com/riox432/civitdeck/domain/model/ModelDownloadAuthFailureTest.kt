package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelDownloadAuthFailureTest {

    private fun download(status: DownloadStatus, errorMessage: String?) = ModelDownload(
        modelId = 1L,
        modelName = "m",
        versionId = 1L,
        versionName = "v1",
        fileId = 1L,
        fileName = "model.safetensors",
        fileUrl = "https://civitai.com/api/download/models/1",
        fileSizeBytes = 1L,
        status = status,
        modelType = "Checkpoint",
        errorMessage = errorMessage,
    )

    @Test
    fun failed_with_http_401_is_auth_required() {
        assertTrue(download(DownloadStatus.Failed, "HTTP 401").isAuthRequiredFailure())
    }

    @Test
    fun failed_with_http_403_is_auth_required() {
        assertTrue(download(DownloadStatus.Failed, "HTTP 403").isAuthRequiredFailure())
    }

    @Test
    fun failed_with_http_404_is_not_auth_required() {
        assertFalse(download(DownloadStatus.Failed, "HTTP 404").isAuthRequiredFailure())
    }

    @Test
    fun failed_without_a_message_is_not_auth_required() {
        assertFalse(download(DownloadStatus.Failed, null).isAuthRequiredFailure())
    }

    @Test
    fun completed_with_http_401_is_not_auth_required() {
        assertFalse(download(DownloadStatus.Completed, "HTTP 401").isAuthRequiredFailure())
    }

    @Test
    fun cancelled_with_http_401_is_not_auth_required() {
        assertFalse(download(DownloadStatus.Cancelled, "HTTP 401").isAuthRequiredFailure())
    }
}
