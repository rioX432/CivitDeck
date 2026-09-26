package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.repository.ModelDownloadRepository

/**
 * Records the result of a finished download: [DownloadStatus.Failed] when the file's SHA-256
 * differs from the hash CivitAI published, [DownloadStatus.Completed] otherwise.
 *
 * A download without a published hash (older CivitAI files have none), or one whose local hash
 * could not be computed ([actualSha256] is null), is not treated as a mismatch and leaves
 * `hashVerified` unset. A mismatch stores no error message; the UI derives its text from
 * `hashVerified == false`.
 *
 * Returns true when the download was marked Completed, false when it was marked Failed or does
 * not exist.
 */
class VerifyDownloadHashUseCase(private val repository: ModelDownloadRepository) {
    suspend operator fun invoke(downloadId: Long, actualSha256: String?): Boolean {
        val download = repository.getDownloadById(downloadId) ?: return false
        val expectedSha256 = download.expectedSha256
        if (expectedSha256.isNullOrBlank() || actualSha256 == null) {
            repository.updateStatus(downloadId, DownloadStatus.Completed)
            return true
        }
        val matched = actualSha256.equals(expectedSha256, ignoreCase = true)
        // Written before the status so an observer never sees the final status without it.
        repository.updateHashVerified(downloadId, matched)
        repository.updateStatus(
            downloadId,
            if (matched) DownloadStatus.Completed else DownloadStatus.Failed,
            errorMessage = null,
        )
        return matched
    }
}
