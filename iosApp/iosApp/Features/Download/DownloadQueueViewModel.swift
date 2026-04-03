import Foundation
import Shared

@MainActor
final class DownloadQueueViewModel: ObservableObject {
    @Published var activeDownloads: [ModelDownload] = []
    @Published var completedDownloads: [ModelDownload] = []
    @Published var failedDownloads: [ModelDownload] = []
    @Published var isLoading = true
    @Published var totalStorageBytes: Int64 = 0

    private let observeDownloadsUseCase: ObserveDownloadsUseCase
    private let pauseDownloadUseCase: PauseDownloadUseCase
    private let resumeDownloadUseCase: ResumeDownloadUseCase
    private let cancelDownloadUseCase: CancelDownloadUseCase
    private let deleteDownloadUseCase: DeleteDownloadUseCase
    private let clearCompletedDownloadsUseCase: ClearCompletedDownloadsUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.observeDownloadsUseCase = KoinHelper.shared.getObserveDownloadsUseCase()
        self.pauseDownloadUseCase = KoinHelper.shared.getPauseDownloadUseCase()
        self.resumeDownloadUseCase = KoinHelper.shared.getResumeDownloadUseCase()
        self.cancelDownloadUseCase = KoinHelper.shared.getCancelDownloadUseCase()
        self.deleteDownloadUseCase = KoinHelper.shared.getDeleteDownloadUseCase()
        self.clearCompletedDownloadsUseCase = KoinHelper.shared.getClearCompletedDownloadsUseCase()
        observeDownloads()
    }

    private func observeDownloads() {
        observeTask = Task {
            for await downloads in observeDownloadsUseCase.invoke() {
                guard !Task.isCancelled else { return }
                guard let list = downloads as? [ModelDownload] else { continue }

                let active = list.filter { download in
                    let status = download.status
                    return status == .pending || status == .downloading || status == .paused
                }
                let completed = list.filter { $0.status == .completed }
                let failed = list.filter { $0.status == .failed || $0.status == .cancelled }
                let totalBytes = completed.reduce(Int64(0)) { $0 + $1.fileSizeBytes }

                self.activeDownloads = active
                self.completedDownloads = completed
                self.failedDownloads = failed
                self.totalStorageBytes = totalBytes
                self.isLoading = false
            }
        }
    }

    func pauseDownload(_ downloadId: Int64) {
        DownloadService.shared.cancelDownload(downloadId: downloadId)
        Task { try? await pauseDownloadUseCase.invoke(id: downloadId) }
    }

    func resumeDownload(_ downloadId: Int64) {
        Task {
            try? await resumeDownloadUseCase.invoke(id: downloadId)
            guard let download = activeDownloads.first(where: { $0.id == downloadId }) else { return }
            let apiKey = KoinHelper.shared.getApiKeyProvider().apiKey
            DownloadService.shared.resumeDownload(
                downloadId: downloadId,
                url: download.fileUrl,
                apiKey: apiKey
            )
        }
    }

    func cancelDownload(_ downloadId: Int64) {
        DownloadService.shared.cancelDownload(downloadId: downloadId)
        Task { try? await cancelDownloadUseCase.invoke(id: downloadId) }
    }

    func retryDownload(_ downloadId: Int64) {
        resumeDownload(downloadId)
    }

    func deleteDownload(_ downloadId: Int64) {
        Task { try? await deleteDownloadUseCase.invoke(id: downloadId) }
    }

    func clearCompleted() {
        Task { try? await clearCompletedDownloadsUseCase.invoke() }
    }
}
