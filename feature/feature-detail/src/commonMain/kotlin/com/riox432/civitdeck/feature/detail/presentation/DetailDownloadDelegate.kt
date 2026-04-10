package com.riox432.civitdeck.feature.detail.presentation

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private const val TAG = "DetailDownloadDelegate"
private const val KB_TO_BYTES = 1024.0

internal class DetailDownloadDelegate(
    private val modelId: Long,
    private val scope: CoroutineScope,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val trackModelViewUseCase: TrackModelViewUseCase,
    private val downloadEnqueuedEvent: MutableSharedFlow<Long>,
) {

    fun downloadFile(file: ModelFile, model: Model?, version: ModelVersion?) {
        model ?: return
        version ?: return
        scope.launch {
            suspendRunCatching {
                val download = buildModelDownload(model, version, file)
                val id = enqueueDownloadUseCase(download)
                downloadEnqueuedEvent.tryEmit(id)
                trackModelViewUseCase.trackInteraction(modelId, InteractionType.DOWNLOAD)
            }.onFailure { e -> Logger.w(TAG, "Download enqueue failed: ${e.message}") }
        }
    }

    fun cancelDownload(downloadId: Long) {
        scope.launch {
            suspendRunCatching { cancelDownloadUseCase(downloadId) }
                .onFailure { e -> Logger.w(TAG, "Cancel download failed: ${e.message}") }
        }
    }

    companion object {
        fun buildModelDownload(
            model: Model,
            version: ModelVersion,
            file: ModelFile,
        ): ModelDownload = ModelDownload(
            modelId = model.id,
            modelName = model.name,
            versionId = version.id,
            versionName = version.name,
            fileId = file.id,
            fileName = file.name,
            fileUrl = file.downloadUrl,
            fileSizeBytes = (file.sizeKB * KB_TO_BYTES).toLong(),
            status = DownloadStatus.Pending,
            modelType = model.type.name,
            expectedSha256 = file.hashes["SHA256"],
        )
    }
}
