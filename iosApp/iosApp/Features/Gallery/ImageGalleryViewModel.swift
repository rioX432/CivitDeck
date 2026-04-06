import Foundation
import Shared

typealias CivitSortOrder = Core_domainSortOrder
typealias CivitImage = Core_domainImage

enum AspectRatioFilter: String, CaseIterable {
    case portrait = "Portrait"
    case landscape = "Landscape"
    case square = "Square"
}

@MainActor
final class ImageGalleryViewModelOwner: ObservableObject {
    let vm: ImageGalleryViewModel
    private let store = ViewModelStore()

    @Published var allImages: [CivitImage] = []
    @Published var selectedSort: CivitSortOrder = .highestRated
    @Published var selectedPeriod: TimePeriod = .allTime
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(
        softIntensity: 75, matureIntensity: 25, explicitIntensity: 0
    )
    @Published var selectedAspectRatio: AspectRatioFilter?
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String?
    @Published var hasMore: Bool = true
    @Published var selectedImageIndex: Int?

    var images: [CivitImage] {
        guard let filter = selectedAspectRatio else { return allImages }
        return allImages.filter { image in
            guard image.width > 0, image.height > 0 else { return true }
            let ratio = CGFloat(image.width) / CGFloat(image.height)
            switch filter {
            case .portrait: return ratio < 0.83
            case .landscape: return ratio > 1.2
            case .square: return ratio >= 0.83 && ratio <= 1.2
            }
        }
    }

    init(modelVersionId: Int64) {
        vm = KoinHelper.shared.createImageGalleryViewModel(modelVersionId: modelVersionId)
        store.put(key: "ImageGalleryViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            allImages = state.allImages as? [CivitImage] ?? []
            selectedSort = state.selectedSort
            selectedPeriod = state.selectedPeriod
            nsfwFilterLevel = state.nsfwFilterLevel
            nsfwBlurSettings = state.nsfwBlurSettings
            isLoading = state.isLoading
            isLoadingMore = state.isLoadingMore
            error = state.error
            hasMore = state.hasMore
            if let idx = state.selectedImageIndex {
                selectedImageIndex = Int(idx.int32Value)
            } else {
                selectedImageIndex = nil
            }
        }
    }

    func onSortSelected(_ sort: CivitSortOrder) {
        selectedSort = sort
        vm.onSortSelected(sort: sort)
    }

    func onPeriodSelected(_ period: TimePeriod) {
        selectedPeriod = period
        vm.onPeriodSelected(period: period)
    }

    func onAspectRatioSelected(_ filter: AspectRatioFilter?) {
        selectedAspectRatio = filter
    }

    func loadMore() {
        vm.loadMore()
    }

    func onImageSelected(_ index: Int) {
        selectedImageIndex = index
        vm.onImageSelected(index: Int32(index))
    }

    func onDismissViewer() {
        vm.onDismissViewer()
        selectedImageIndex = nil
    }

    func retry() {
        vm.retry()
    }

    func savePrompt(meta: ImageGenerationMeta, sourceImageUrl: String) {
        vm.savePrompt(meta: meta, sourceImageUrl: sourceImageUrl)
    }
}
