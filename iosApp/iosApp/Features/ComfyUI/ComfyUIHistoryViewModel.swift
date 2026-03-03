import Foundation
import Shared

enum HistorySortOrder: String, CaseIterable {
    case newest = "Newest"
    case oldest = "Oldest"
}

@MainActor
final class ComfyUIHistoryViewModel: ObservableObject {
    @Published var images: [ComfyUIGeneratedImage] = []
    @Published var isLoading = true
    @Published var errorMessage: String?
    @Published var selectedSort: HistorySortOrder = .newest
    @Published var imageSaveSuccess: Bool?

    // Dataset picker state
    @Published var datasets: [DatasetCollection] = []
    @Published var showDatasetPicker: Bool = false
    @Published var pendingImageForDataset: ComfyUIGeneratedImage?
    @Published var addToDatasetSuccess: Bool?

    var filteredImages: [ComfyUIGeneratedImage] {
        selectedSort == .newest ? images.reversed() : images
    }

    private let fetchHistoryUseCase = KoinHelper.shared.getFetchComfyUIHistoryUseCase()
    private let saveImageUseCase = KoinHelper.shared.getSaveGeneratedImageUseCase()
    private let observeDatasetUseCase = KoinHelper.shared.getObserveDatasetCollectionsUseCase()
    private let addToDatasetUseCase = KoinHelper.shared.getAddImageToDatasetUseCase()
    private let createDatasetUseCase = KoinHelper.shared.getCreateDatasetCollectionUseCase()
    private var observeTask: Task<Void, Never>?
    private var datasetObserveTask: Task<Void, Never>?

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
        datasetObserveTask?.cancel()
        datasetObserveTask = Task {
            for await list in observeDatasetUseCase.invoke() {
                guard !Task.isCancelled else { return }
                datasets = list.compactMap { $0 as? DatasetCollection }
            }
        }
    }

    func stopObserving() {
        observeTask?.cancel()
        observeTask = nil
        datasetObserveTask?.cancel()
        datasetObserveTask = nil
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

    // MARK: - Dataset

    func onAddToDatasetTap(image: ComfyUIGeneratedImage) {
        pendingImageForDataset = image
        showDatasetPicker = true
    }

    func onDatasetSelected(datasetId: Int64) {
        guard let image = pendingImageForDataset else { return }
        showDatasetPicker = false
        let tags = buildTags(from: image)
        Task {
            do {
                _ = try await addToDatasetUseCase.invoke(
                    datasetId: datasetId,
                    imageUrl: image.imageUrl,
                    sourceType: ImageSource.generated,
                    trainable: true,
                    tags: tags
                )
                addToDatasetSuccess = true
            } catch {
                addToDatasetSuccess = false
            }
            pendingImageForDataset = nil
        }
    }

    func onCreateDatasetAndSelect(name: String) {
        guard let image = pendingImageForDataset else { return }
        showDatasetPicker = false
        let tags = buildTags(from: image)
        Task {
            do {
                let datasetId = try await createDatasetUseCase.invoke(name: name, description: "")
                _ = try await addToDatasetUseCase.invoke(
                    datasetId: datasetId.int64Value,
                    imageUrl: image.imageUrl,
                    sourceType: ImageSource.generated,
                    trainable: true,
                    tags: tags
                )
                addToDatasetSuccess = true
            } catch {
                addToDatasetSuccess = false
            }
            pendingImageForDataset = nil
        }
    }

    func onDismissDatasetPicker() {
        showDatasetPicker = false
        pendingImageForDataset = nil
    }

    private func buildTags(from image: ComfyUIGeneratedImage) -> [String] {
        var tags: [String] = []
        if let seed = image.meta.seed {
            tags.append("seed:\(seed.int64Value)")
        }
        if let sampler = image.meta.samplerName, !sampler.isEmpty {
            tags.append("sampler:\(sampler)")
        }
        if !image.meta.positivePrompt.isEmpty {
            tags.append("prompt_hash:\(abs(image.meta.positivePrompt.hashValue))")
        }
        return tags
    }
}
