import Foundation
import Shared

@MainActor
final class DatasetDetailViewModel: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var isSelectionMode = false
    @Published var selectedSource: ImageSource?
    @Published var detailImage: DatasetImage?

    let datasetId: Int64

    private let observeImagesUseCase: ObserveDatasetImagesUseCase
    private let removeImagesUseCase: RemoveImageFromDatasetUseCase
    private let updateTrainableUseCase: UpdateTrainableUseCase
    private let editCaptionUseCase: EditCaptionUseCase
    private var observeTask: Task<Void, Never>?

    init(datasetId: Int64) {
        self.datasetId = datasetId
        self.observeImagesUseCase = KoinHelper.shared.getObserveDatasetImagesUseCase()
        self.removeImagesUseCase = KoinHelper.shared.getRemoveImageFromDatasetUseCase()
        self.updateTrainableUseCase = KoinHelper.shared.getUpdateTrainableUseCase()
        self.editCaptionUseCase = KoinHelper.shared.getEditCaptionUseCase()
        observeTask = Task { await observeImages() }
    }

    deinit {
        observeTask?.cancel()
    }

    var filteredImages: [DatasetImage] {
        guard let source = selectedSource else { return images }
        return images.filter { $0.sourceType == source }
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

    func setSourceFilter(_ source: ImageSource?) { selectedSource = source }

    func showDetail(_ image: DatasetImage) { detailImage = image }

    func dismissDetail() { detailImage = nil }

    func updateTrainable(id: Int64, trainable: Bool) {
        Task {
            try? await updateTrainableUseCase.invoke(
                imageId: KotlinLong(value: id),
                trainable: KotlinBoolean(bool: trainable)
            )
        }
    }

    func saveCaption(imageId: Int64, text: String) {
        Task {
            try? await editCaptionUseCase.invoke(
                datasetImageId: KotlinLong(value: imageId),
                text: text
            )
        }
    }
}
