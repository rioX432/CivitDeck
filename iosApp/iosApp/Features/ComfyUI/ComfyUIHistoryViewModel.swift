import Foundation
import Shared

@MainActor
final class ComfyUIHistoryViewModel: ObservableObject {
    @Published var images: [ComfyUIGeneratedImage] = []
    @Published var isLoading = true
    @Published var errorMessage: String?
    @Published var selectedWorkflow: String?
    @Published var imageSaveSuccess: Bool?

    var workflows: [String] {
        let ids = images.map { $0.promptId }
        var seen = Set<String>()
        return ids.filter { seen.insert($0).inserted }
    }

    var filteredImages: [ComfyUIGeneratedImage] {
        guard let workflow = selectedWorkflow else { return images }
        return images.filter { $0.promptId == workflow }
    }

    private let fetchHistoryUseCase = KoinHelper.shared.getFetchComfyUIHistoryUseCase()
    private let saveImageUseCase = KoinHelper.shared.getSaveGeneratedImageUseCase()
    private var observeTask: Task<Void, Never>?

    func startObserving() {
        observeTask?.cancel()
        observeTask = Task {
            do {
                for try await list in fetchHistoryUseCase.invoke() {
                    guard !Task.isCancelled else { return }
                    images = list as? [ComfyUIGeneratedImage] ?? []
                    isLoading = false
                }
            } catch {
                guard !Task.isCancelled else { return }
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func stopObserving() {
        observeTask?.cancel()
        observeTask = nil
    }

    func retry() {
        isLoading = true
        errorMessage = nil
        startObserving()
    }

    func dismissError() {
        errorMessage = nil
    }

    func onSaveImage(url: String) {
        Task {
            do {
                let success = try await saveImageUseCase.invoke(url: url, filename: "civitdeck_history")
                imageSaveSuccess = success.boolValue
            } catch {
                imageSaveSuccess = false
            }
        }
    }
}
