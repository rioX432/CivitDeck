import Foundation
import Shared

@MainActor
final class DatasetDetailViewModelOwner: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var isSelectionMode = false
    @Published var selectedSource: ImageSource?
    @Published var detailImage: DatasetImage?
    @Published var showDuplicateReview = false
    @Published var showResolutionFilter = false
    @Published var showExportSheet = false
    @Published var exportProgress: ExportProgress?
    @Published var minWidth: Int = 0
    @Published var minHeight: Int = 0
    @Published var duplicateImageCount: Int = 0
    @Published var availableExportFormats: [PluginExportFormat] = []
    @Published var selectedExportFormatId: String?

    let datasetId: Int64

    private let vm: DatasetDetailViewModel
    private let store: ViewModelStore

    init(datasetId: Int64) {
        self.datasetId = datasetId
        store = ViewModelStore()
        vm = KoinHelper.shared.createDatasetDetailViewModel(datasetId: datasetId)
        store.put(key: "DatasetDetailViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeImages() async {
        for await list in vm.images {
            images = list as? [DatasetImage] ?? []
        }
    }

    func observeSelectionState() async {
        for await ids in vm.selectedImageIds {
            let longIds = ids as? Set<KotlinLong> ?? []
            selectedImageIds = Set(longIds.map { $0.int64Value })
        }
    }

    func observeSelectionMode() async {
        for await mode in vm.isSelectionMode {
            guard let boolValue = mode as? Bool else { continue }
            isSelectionMode = boolValue
        }
    }

    func observeDuplicateCount() async {
        for await count in vm.duplicateCount {
            guard let intValue = count as? Int32 else { continue }
            duplicateImageCount = Int(intValue)
        }
    }

    func observeExportFormats() async {
        for await formats in vm.availableExportFormats {
            let items = formats as? [PluginExportFormat] ?? []
            availableExportFormats = items
            if selectedExportFormatId == nil, let first = items.first {
                selectedExportFormatId = first.id
            }
        }
    }

    func observeExportProgress() async {
        for await progress in vm.exportProgress {
            exportProgress = progress as? ExportProgress
        }
    }

    var filteredImages: [DatasetImage] {
        guard let source = selectedSource else { return images }
        return images.filter { $0.sourceType == source }
    }

    var lowResImageCount: Int {
        guard minWidth > 0 || minHeight > 0 else { return 0 }
        return images.filter { img in
            guard let width = img.width, let height = img.height else { return false }
            return Int32(truncating: width) < minWidth || Int32(truncating: height) < minHeight
        }.count
    }

    func setResolutionFilter(minWidth: Int, minHeight: Int) {
        self.minWidth = minWidth
        self.minHeight = minHeight
        vm.setResolutionFilter(w: Int32(minWidth), h: Int32(minHeight))
    }

    func toggleSelection(id: Int64) { vm.toggleSelection(imageId: id) }
    func enterSelectionMode(id: Int64) { vm.enterSelectionMode(imageId: id) }
    func clearSelection() { vm.clearSelection() }
    func removeSelected() { vm.removeSelected() }
    func setSourceFilter(_ source: ImageSource?) { vm.setSourceFilter(source: source) }
    func showDetail(_ image: DatasetImage) { vm.showDetail(image: image) }
    func dismissDetail() { vm.dismissDetail() }

    func updateTrainable(id: Int64, trainable: Bool) {
        vm.updateTrainable(imageId: id, trainable: trainable)
    }

    func saveCaption(imageId: Int64, text: String) {
        vm.editCaption(imageId: imageId, text: text)
    }

    var trainableImageCount: Int {
        images.filter { $0.trainable && !$0.excluded }.count
    }

    var nonTrainableImageCount: Int {
        images.count - trainableImageCount
    }

    func selectExportFormat(_ formatId: String) {
        selectedExportFormatId = formatId
        vm.selectExportFormat(formatId: formatId)
    }

    func startExport() {
        guard let formatId = selectedExportFormatId ?? availableExportFormats.first?.id else { return }
        showExportSheet = false
        vm.startExport(formatId: formatId)
    }

    func dismissExportResult() {
        vm.dismissExportResult()
    }
}
