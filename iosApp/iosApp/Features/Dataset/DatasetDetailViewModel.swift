import Foundation
import Shared

@MainActor
final class DatasetDetailViewModel: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var isSelectionMode = false
    @Published var selectedSource: ImageSource?
    @Published var detailImage: DatasetImage?
    @Published var showDuplicateReview = false
    @Published var showResolutionFilter = false
    @Published var showExportSheet = false
    @Published var exportProgress: ExportProgress?
    @Published var minWidth = 0
    @Published var minHeight = 0
    @Published var duplicateImageCount = 0
    @Published var availableExportFormats: [PluginExportFormat] = []
    @Published var selectedExportFormatId: String?

    let datasetId: Int64

    private let observeImagesUseCase: ObserveDatasetImagesUseCase
    private let removeImagesUseCase: RemoveImageFromDatasetUseCase
    private let updateTrainableUseCase: UpdateTrainableUseCase
    private let editCaptionUseCase: EditCaptionUseCase
    private let detectDuplicatesUseCase: DetectDuplicatesUseCase
    private let filterByResolutionUseCase: FilterByResolutionUseCase
    private let markImageExcludedUseCase: MarkImageExcludedUseCase
    private let exportWithPluginUseCase: ExportWithPluginUseCase
    private let getAvailableExportFormatsUseCase: GetAvailableExportFormatsUseCase
    private var observeTask: Task<Void, Never>?
    private var duplicatesTask: Task<Void, Never>?
    private var exportTask: Task<Void, Never>?
    private var formatsTask: Task<Void, Never>?

    init(datasetId: Int64) {
        self.datasetId = datasetId
        self.observeImagesUseCase = KoinHelper.shared.getObserveDatasetImagesUseCase()
        self.removeImagesUseCase = KoinHelper.shared.getRemoveImageFromDatasetUseCase()
        self.updateTrainableUseCase = KoinHelper.shared.getUpdateTrainableUseCase()
        self.editCaptionUseCase = KoinHelper.shared.getEditCaptionUseCase()
        self.detectDuplicatesUseCase = KoinHelper.shared.getDetectDuplicatesUseCase()
        self.filterByResolutionUseCase = KoinHelper.shared.getFilterByResolutionUseCase()
        self.markImageExcludedUseCase = KoinHelper.shared.getMarkImageExcludedUseCase()
        self.exportWithPluginUseCase = KoinHelper.shared.getExportWithPluginUseCase()
        self.getAvailableExportFormatsUseCase = KoinHelper.shared.getGetAvailableExportFormatsUseCase()
        observeTask = Task { await observeImages() }
        duplicatesTask = Task { await observeDuplicateCount() }
        formatsTask = Task { await observeExportFormats() }
    }

    deinit {
        observeTask?.cancel()
        duplicatesTask?.cancel()
        exportTask?.cancel()
        formatsTask?.cancel()
    }

    var filteredImages: [DatasetImage] {
        guard let source = selectedSource else { return images }
        return images.filter { $0.sourceType == source }
    }

    var lowResImageCount: Int {
        guard minWidth > 0 || minHeight > 0 else { return 0 }
        return images.filter { img in
            guard let width = img.width, let height = img.height else { return false }
            return Int(width.int32Value) < minWidth || Int(height.int32Value) < minHeight
        }.count
    }

    private func observeImages() async {
        for await list in observeImagesUseCase.invoke(datasetId: datasetId) {
            let items = list.compactMap { $0 as? DatasetImage }
            self.images = items
        }
    }

    private func observeDuplicateCount() async {
        for await result in detectDuplicatesUseCase.invoke(
            datasetId: datasetId,
            threshold: 10
        ) {
            let groups = result.compactMap { $0 as? DuplicateGroup }
            duplicateImageCount = groups.reduce(0) { $0 + $1.images.count } - groups.count
        }
    }

    private func observeExportFormats() async {
        for await formats in getAvailableExportFormatsUseCase.invoke() {
            let items = formats.compactMap { $0 as? PluginExportFormat }
            self.availableExportFormats = items
            if selectedExportFormatId == nil, let first = items.first {
                selectedExportFormatId = first.id
            }
        }
    }

    func setResolutionFilter(minWidth: Int, minHeight: Int) {
        self.minWidth = minWidth
        self.minHeight = minHeight
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
                imageId: id,
                trainable: trainable
            )
        }
    }

    func saveCaption(imageId: Int64, text: String) {
        Task {
            try? await editCaptionUseCase.invoke(
                datasetImageId: imageId,
                text: text
            )
        }
    }

    var trainableImageCount: Int {
        images.filter { $0.trainable && !$0.excluded }.count
    }

    var nonTrainableImageCount: Int {
        images.count - trainableImageCount
    }

    func selectExportFormat(_ formatId: String) {
        selectedExportFormatId = formatId
    }

    func startExport() {
        guard let formatId = selectedExportFormatId ?? availableExportFormats.first?.id else { return }
        showExportSheet = false
        exportTask = Task { await runExport(formatId: formatId) }
    }

    func dismissExportResult() {
        exportProgress = nil
    }

    private func runExport(formatId: String) async {
        for await progress in exportWithPluginUseCase.invoke(
            datasetId: datasetId,
            formatId: formatId,
            options: [:]
        ) {
            guard let value = progress as? ExportProgress else { continue }
            self.exportProgress = value
        }
    }
}
