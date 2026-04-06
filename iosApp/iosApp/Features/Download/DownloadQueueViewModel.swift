import Foundation
import Shared

@MainActor
final class DownloadQueueViewModelOwner: ObservableObject {
    @Published var activeDownloads: [ModelDownload] = []
    @Published var completedDownloads: [ModelDownload] = []
    @Published var failedDownloads: [ModelDownload] = []
    @Published var isLoading = true
    @Published var totalStorageBytes: Int64 = 0

    private let vm: DownloadQueueViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createDownloadQueueViewModel()
        store.put(key: "DownloadQueueViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            guard let uiState = state as? DownloadQueueUiState else { continue }
            activeDownloads = uiState.activeDownloads as? [ModelDownload] ?? []
            completedDownloads = uiState.completedDownloads as? [ModelDownload] ?? []
            failedDownloads = uiState.failedDownloads as? [ModelDownload] ?? []
            isLoading = uiState.isLoading
            totalStorageBytes = uiState.totalStorageBytes
        }
    }

    func pauseDownload(_ downloadId: Int64) {
        DownloadService.shared.cancelDownload(downloadId: downloadId)
        vm.pauseDownload(downloadId: downloadId)
    }

    func resumeDownload(_ downloadId: Int64) {
        vm.resumeDownload(downloadId: downloadId)
        guard let download = activeDownloads.first(where: { $0.id == downloadId }) else { return }
        let apiKey = KoinHelper.shared.getApiKeyProvider().apiKey
        DownloadService.shared.resumeDownload(
            downloadId: downloadId,
            url: download.fileUrl,
            apiKey: apiKey
        )
    }

    func cancelDownload(_ downloadId: Int64) {
        DownloadService.shared.cancelDownload(downloadId: downloadId)
        vm.cancelDownload(downloadId: downloadId)
    }

    func retryDownload(_ downloadId: Int64) {
        resumeDownload(downloadId)
    }

    func deleteDownload(_ downloadId: Int64) {
        vm.deleteDownload(downloadId: downloadId)
    }

    func clearCompleted() {
        vm.clearCompleted()
    }
}
