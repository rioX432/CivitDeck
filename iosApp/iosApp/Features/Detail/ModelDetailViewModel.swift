import Foundation
import Shared

@MainActor
final class ModelDetailViewModelOwner: ObservableObject {
    let vm: SharedModelDetailViewModel
    private let store = ViewModelStore()

    @Published var model: Model?
    @Published var isFavorite: Bool = false
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var selectedVersionIndex: Int = 0
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var collections: [ModelCollection] = []
    @Published var modelCollectionIds: [Int64] = []
    @Published var powerUserMode: Bool = false
    @Published var note: ModelNote?
    @Published var personalTags: [PersonalTag] = []
    @Published var downloads: [ModelDownload] = []
    @Published var reviews: [ResourceReview] = []
    @Published var ratingTotals: RatingTotals?
    @Published var reviewSortOrder: ReviewSortOrder = .newest
    @Published var isReviewsLoading: Bool = false
    @Published var isSubmittingReview: Bool = false
    @Published var reviewSubmitSuccess: Bool = false

    let modelId: Int64

    var selectedVersion: ModelVersion? {
        guard let model else { return nil }
        let versions = model.modelVersions
        guard selectedVersionIndex < versions.count else { return nil }
        return versions[selectedVersionIndex]
    }

    init(modelId: Int64) {
        self.modelId = modelId
        vm = KoinHelper.shared.createModelDetailViewModel(modelId: modelId)
        store.put(key: "ModelDetailViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    // MARK: - State Observation

    func observeUiState() async {
        for await state in vm.uiState {
            model = state.model
            isFavorite = state.isFavorite
            isLoading = state.isLoading
            error = state.error
            selectedVersionIndex = Int(state.selectedVersionIndex)
            nsfwFilterLevel = state.nsfwFilterLevel
            powerUserMode = state.powerUserMode
            note = state.note
            personalTags = state.personalTags as? [PersonalTag] ?? []
            downloads = state.downloads as? [ModelDownload] ?? []
            reviews = state.reviews as? [ResourceReview] ?? []
            ratingTotals = state.ratingTotals
            reviewSortOrder = state.reviewSortOrder
            isReviewsLoading = state.isReviewsLoading
            isSubmittingReview = state.isSubmittingReview
            reviewSubmitSuccess = state.reviewSubmitSuccess
        }
    }

    func observeCollections() async {
        for await list in vm.collections {
            collections = list as? [ModelCollection] ?? []
        }
    }

    func observeModelCollections() async {
        for await list in vm.modelCollectionIds {
            modelCollectionIds = list.compactMap { ($0 as? NSNumber)?.int64Value }
        }
    }

    // MARK: - Actions

    func onFavoriteToggle() { vm.onFavoriteToggle() }

    func onVersionSelected(_ index: Int) { vm.onVersionSelected(index: Int32(index)) }

    func retry() { vm.retry() }

    func saveNote(_ text: String) { vm.saveNote(text: text) }

    func addTag(_ tag: String) { vm.addTag(tag: tag) }

    func removeTag(_ tag: String) { vm.removeTag(tag: tag) }

    func toggleCollection(_ collectionId: Int64) { vm.toggleCollection(collectionId: collectionId) }

    func createCollectionAndAdd(name: String) { vm.createCollectionAndAdd(name: name) }

    func downloadFile(_ file: ModelFile) {
        vm.downloadFile(file: file)
    }

    func cancelDownload(_ downloadId: Int64) { vm.cancelDownload(downloadId: downloadId) }

    func onReviewSortChanged(_ order: ReviewSortOrder) { vm.onReviewSortChanged(order: order) }

    func submitReview(modelVersionId: Int64, rating: Int32, recommended: Bool, details: String?) {
        vm.submitReview(
            modelVersionId: modelVersionId,
            rating: rating,
            recommended: recommended,
            details: details
        )
    }

    func dismissReviewSuccess() { vm.dismissReviewSuccess() }

    func trackInteraction(_ type: InteractionType) {
        // Interaction tracking is handled internally by shared VM
    }

    func onDisappear() {
        // View cleanup is handled by shared VM's onCleared via ViewModelStore
    }
}
