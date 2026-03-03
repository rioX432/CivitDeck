import Foundation
import Shared

@MainActor
final class DatasetDetailViewModel: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var isSelectionMode = false

    let datasetId: Int64

    private let observeImagesUseCase: ObserveDatasetImagesUseCase
    private let removeImagesUseCase: RemoveImageFromDatasetUseCase
    private var observeTask: Task<Void, Never>?

    init(datasetId: Int64) {
        self.datasetId = datasetId
        self.observeImagesUseCase = KoinHelper.shared.getObserveDatasetImagesUseCase()
        self.removeImagesUseCase = KoinHelper.shared.getRemoveImageFromDatasetUseCase()
        observeTask = Task { await observeImages() }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observeImages() async {
        for await list in observeImagesUseCase.invoke(datasetId: datasetId) {
            let items = list.compactMap { $0 as? DatasetImage }
            self.images = items
        }
    }

    func toggleSelection(id: Int64) {
        if selectedImageIds.contains(id) {
            selectedImageIds.remove(id)
        } else {
            selectedImageIds.insert(id)
        }
        if selectedImageIds.isEmpty { isSelectionMode = false }
    }

    func enterSelectionMode(id: Int64) {
        isSelectionMode = true
        selectedImageIds = [id]
    }

    func clearSelection() {
        selectedImageIds.removeAll()
        isSelectionMode = false
    }

    func removeSelected() {
        let ids = Array(selectedImageIds).map { KotlinLong(value: $0) }
        guard !ids.isEmpty else { return }
        Task {
            try? await removeImagesUseCase.invoke(imageIds: ids)
            clearSelection()
        }
    }
}
