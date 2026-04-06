import Foundation
import Shared

@MainActor
final class ComfyUIHistoryViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiComfyUIHistoryViewModel
    private let store = ViewModelStore()

    @Published var images: [ComfyUIGeneratedImage] = []
    @Published var isLoading = false
    @Published var error: String?
    @Published var selectedSort: Feature_comfyuiHistorySortOrder = .newest
    @Published var imageSaveSuccess: KotlinBoolean?
    @Published var showDatasetPicker = false
    @Published var pendingImageForDataset: ComfyUIGeneratedImage?
    @Published var addToDatasetSuccess: KotlinBoolean?
    @Published var datasets: [DatasetCollection] = []
    @Published var shareHashtags: [ShareHashtag] = []

    init() {
        vm = KoinHelper.shared.createComfyUIHistoryViewModel()
        store.put(key: "ComfyUIHistoryViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            images = state.images as? [ComfyUIGeneratedImage] ?? []
            isLoading = state.isLoading
            error = state.error
            selectedSort = state.selectedSort
            imageSaveSuccess = state.imageSaveSuccess
            showDatasetPicker = state.showDatasetPicker
            pendingImageForDataset = state.pendingImageForDataset
            addToDatasetSuccess = state.addToDatasetSuccess
        }
    }

    func observeDatasets() async {
        for await list in vm.datasets {
            datasets = list as? [DatasetCollection] ?? []
        }
    }

    func observeShareHashtags() async {
        for await list in vm.shareHashtags {
            shareHashtags = list as? [ShareHashtag] ?? []
        }
    }

    var filteredImages: [ComfyUIGeneratedImage] {
        (vm.filteredImages() as? [ComfyUIGeneratedImage]) ?? []
    }

    func refresh() { vm.refresh() }
    func onSelectSort(_ sort: Feature_comfyuiHistorySortOrder) { vm.onSelectSort(sort: sort) }
    func onSaveImage(url: String, filename: String) { vm.onSaveImage(imageUrl: url, filename: filename) }
    func onDismissSaveResult() { vm.onDismissSaveResult() }
    func onAddToDatasetTap(image: ComfyUIGeneratedImage) { vm.onAddToDatasetTap(image: image) }
    func onDatasetSelected(datasetId: Int64) { vm.onDatasetSelected(datasetId: datasetId) }
    func onCreateDatasetAndSelect(name: String) { vm.onCreateDatasetAndSelect(name: name) }
    func onDismissDatasetPicker() { vm.onDismissDatasetPicker() }
    func onDismissDatasetResult() { vm.onDismissDatasetResult() }
    func onToggleShareHashtag(tag: String, isEnabled: Bool) {
        vm.onToggleShareHashtag(tag: tag, isEnabled: isEnabled)
    }
    func onAddShareHashtag(tag: String) { vm.onAddShareHashtag(tag: tag) }
    func onRemoveShareHashtag(tag: String) { vm.onRemoveShareHashtag(tag: tag) }
}
