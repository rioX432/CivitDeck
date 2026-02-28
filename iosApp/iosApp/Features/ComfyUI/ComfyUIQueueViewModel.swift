import Foundation
import Shared

@MainActor
class ComfyUIQueueViewModel: ObservableObject {
    @Published var jobs: [QueueJob] = []
    @Published var isLoading = true
    @Published var error: String?
    @Published var cancellingIds: Set<String> = []

    private let observeQueue = KoinHelper.shared.getObserveComfyUIQueueUseCase()
    private let cancelJob = KoinHelper.shared.getCancelComfyUIJobUseCase()
    private var observeTask: Task<Void, Never>?

    func startObserving() {
        observeTask?.cancel()
        observeTask = Task {
            do {
                for try await jobList in observeQueue.invoke() {
                    guard !Task.isCancelled else { return }
                    jobs = jobList as? [QueueJob] ?? []
                    isLoading = false
                }
            } catch {
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }

    func stopObserving() {
        observeTask?.cancel()
        observeTask = nil
    }

    func onCancelJob(promptId: String) {
        guard !cancellingIds.contains(promptId) else { return }
        cancellingIds.insert(promptId)
        Task {
            defer { cancellingIds.remove(promptId) }
            do {
                try await cancelJob.invoke(promptId: promptId)
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func dismissError() {
        error = nil
    }
}
