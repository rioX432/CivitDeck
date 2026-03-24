import Foundation
import Shared

@MainActor
final class ModelDetailViewModel: ObservableObject {
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
    private let getModelDetailUseCase: GetModelDetailUseCase
    private let observeIsFavoriteUseCase: ObserveIsFavoriteUseCase
    private let toggleFavoriteUseCase: ToggleFavoriteUseCase
    private let trackModelViewUseCase: TrackModelViewUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let enrichModelImagesUseCase: EnrichModelImagesUseCase
    private let observeCollectionsUseCase: ObserveCollectionsUseCase
    private let observeModelCollectionsUseCase: ObserveModelCollectionsUseCase
    private let addModelToCollectionUseCase: AddModelToCollectionUseCase
    private let removeModelFromCollectionUseCase: RemoveModelFromCollectionUseCase
    private let createCollectionUseCase: CreateCollectionUseCase
    private let observePowerUserModeUseCase: ObservePowerUserModeUseCase
    private let observeModelNoteUseCase: ObserveModelNoteUseCase
    private let saveModelNoteUseCase: SaveModelNoteUseCase
    private let deleteModelNoteUseCase: DeleteModelNoteUseCase
    private let observePersonalTagsUseCase: ObservePersonalTagsUseCase
    private let addPersonalTagUseCase: AddPersonalTagUseCase
    private let removePersonalTagUseCase: RemovePersonalTagUseCase
    private let observeModelDownloadsUseCase: ObserveModelDownloadsUseCase
    private let enqueueDownloadUseCase: EnqueueDownloadUseCase
    private let cancelDownloadUseCase: CancelDownloadUseCase
    private let getModelReviewsUseCase: GetModelReviewsUseCase
    private let getRatingTotalsUseCase: GetRatingTotalsUseCase
    private let submitReviewUseCase: SubmitReviewUseCase
    private var enrichedVersionIds: Set<Int64> = []
    private var viewStartDate: Date?

    var selectedVersion: ModelVersion? {
        guard let model else { return nil }
        let versions = model.modelVersions
        guard selectedVersionIndex < versions.count else { return nil }
        return versions[selectedVersionIndex]
    }

    init(modelId: Int64) {
        self.modelId = modelId
        self.getModelDetailUseCase = KoinHelper.shared.getModelDetailUseCase()
        self.observeIsFavoriteUseCase = KoinHelper.shared.getObserveIsFavoriteUseCase()
        self.toggleFavoriteUseCase = KoinHelper.shared.getToggleFavoriteUseCase()
        self.trackModelViewUseCase = KoinHelper.shared.getTrackModelViewUseCase()
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.enrichModelImagesUseCase = KoinHelper.shared.getEnrichModelImagesUseCase()
        self.observeCollectionsUseCase = KoinHelper.shared.getObserveCollectionsUseCase()
        self.observeModelCollectionsUseCase = KoinHelper.shared.getObserveModelCollectionsUseCase()
        self.addModelToCollectionUseCase = KoinHelper.shared.getAddModelToCollectionUseCase()
        self.removeModelFromCollectionUseCase = KoinHelper.shared.getRemoveModelFromCollectionUseCase()
        self.createCollectionUseCase = KoinHelper.shared.getCreateCollectionUseCase()
        self.observePowerUserModeUseCase = KoinHelper.shared.getObservePowerUserModeUseCase()
        self.observeModelNoteUseCase = KoinHelper.shared.getObserveModelNoteUseCase()
        self.saveModelNoteUseCase = KoinHelper.shared.getSaveModelNoteUseCase()
        self.deleteModelNoteUseCase = KoinHelper.shared.getDeleteModelNoteUseCase()
        self.observePersonalTagsUseCase = KoinHelper.shared.getObservePersonalTagsUseCase()
        self.addPersonalTagUseCase = KoinHelper.shared.getAddPersonalTagUseCase()
        self.removePersonalTagUseCase = KoinHelper.shared.getRemovePersonalTagUseCase()
        self.observeModelDownloadsUseCase = KoinHelper.shared.getObserveModelDownloadsUseCase()
        self.enqueueDownloadUseCase = KoinHelper.shared.getEnqueueDownloadUseCase()
        self.cancelDownloadUseCase = KoinHelper.shared.getCancelDownloadUseCase()
        self.getModelReviewsUseCase = KoinHelper.shared.getModelReviewsUseCase()
        self.getRatingTotalsUseCase = KoinHelper.shared.getRatingTotalsUseCase()
        self.submitReviewUseCase = KoinHelper.shared.getSubmitReviewUseCase()
        self.viewStartDate = nil
        loadModel()
    }

    /// Called from SwiftUI .task modifier — observes favorite state reactively via SKIE Flow.
    func observeFavorite() async {
        for await value in observeIsFavoriteUseCase.invoke(modelId: modelId) {
            isFavorite = value.boolValue
        }
    }

    func observePowerUserMode() async {
        for await value in observePowerUserModeUseCase.invoke() {
            powerUserMode = value.boolValue
        }
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            nsfwFilterLevel = value
        }
    }

    func observeNote() async {
        for await value in observeModelNoteUseCase.invoke(modelId: modelId) {
            note = value
        }
    }

    func observePersonalTags() async {
        for await list in observePersonalTagsUseCase.invoke(modelId: modelId) {
            personalTags = list.compactMap { $0 as? PersonalTag }
        }
    }

    func onFavoriteToggle() {
        guard let model else { return }
        Task {
            try? await toggleFavoriteUseCase.invoke(model: model)
            trackInteraction(.favorite)
        }
    }

    func saveNote(_ text: String) {
        Task {
            if text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                try? await deleteModelNoteUseCase.invoke(modelId: modelId)
            } else {
                try? await saveModelNoteUseCase.invoke(modelId: modelId, noteText: text)
            }
        }
    }

    func addTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else { return }
        Task {
            try? await addPersonalTagUseCase.invoke(modelId: modelId, tag: trimmed)
        }
    }

    func removeTag(_ tag: String) {
        Task {
            try? await removePersonalTagUseCase.invoke(modelId: modelId, tag: tag)
        }
    }

    func observeCollections() async {
        for await list in observeCollectionsUseCase.invoke() {
            collections = list.compactMap { $0 as? ModelCollection }
        }
    }

    func observeModelCollections() async {
        for await list in observeModelCollectionsUseCase.invoke(modelId: modelId) {
            modelCollectionIds = list.compactMap { ($0 as? NSNumber)?.int64Value }
        }
    }

    func toggleCollection(_ collectionId: Int64) {
        guard let model else { return }
        Task {
            if modelCollectionIds.contains(collectionId) {
                try? await removeModelFromCollectionUseCase.invoke(
                    collectionId: collectionId, modelId: model.id
                )
            } else {
                try? await addModelToCollectionUseCase.invoke(
                    collectionId: collectionId, model: model
                )
            }
        }
    }

    func createCollectionAndAdd(name: String) {
        guard let model else { return }
        Task {
            let newId = try await createCollectionUseCase.invoke(name: name)
            try? await addModelToCollectionUseCase.invoke(
                collectionId: newId.int64Value, model: model
            )
        }
    }

    func onVersionSelected(_ index: Int) {
        selectedVersionIndex = index
        enrichCurrentVersion()
    }

    func retry() {
        loadModel()
    }

    private func enrichCurrentVersion() {
        guard let model else { return }
        let versions = model.modelVersions
        guard selectedVersionIndex < versions.count else { return }
        let version = versions[selectedVersionIndex]
        let versionId = version.id
        guard !enrichedVersionIds.contains(versionId) else { return }
        enrichedVersionIds.insert(versionId)
        Task {
            do {
                let enriched = try await enrichModelImagesUseCase.invoke(
                    modelVersionId: versionId,
                    images: version.images
                )
                guard let currentModel = self.model else { return }
                let updatedVersions: [ModelVersion] = currentModel.modelVersions.map { v in
                    guard v.id == versionId else { return v }
                    return ModelVersion(
                        id: v.id, modelId: v.modelId, name: v.name, description: v.description_,
                        createdAt: v.createdAt, baseModel: v.baseModel, trainedWords: v.trainedWords,
                        downloadUrl: v.downloadUrl, files: v.files, images: enriched, stats: v.stats
                    )
                }
                self.model = Model(
                    id: currentModel.id, name: currentModel.name,
                    description: currentModel.description_,
                    type: currentModel.type, nsfw: currentModel.nsfw, tags: currentModel.tags,
                    mode: currentModel.mode, creator: currentModel.creator,
                    stats: currentModel.stats, modelVersions: updatedVersions,
                    source: currentModel.source
                )
            } catch is CancellationError {
                return
            } catch {
                enrichedVersionIds.remove(versionId)
            }
        }
    }

    func observeDownloads() async {
        for await list in observeModelDownloadsUseCase.invoke(modelId: modelId) {
            downloads = list.compactMap { $0 as? ModelDownload }
        }
    }

    func downloadFile(_ file: ModelFile) {
        guard let model else { return }
        guard let version = selectedVersion else { return }
        let apiKey = KoinHelper.shared.getApiKeyProvider().apiKey
        Task {
            let download = ModelDownload(
                id: 0,
                modelId: model.id,
                modelName: model.name,
                versionId: version.id,
                versionName: version.name,
                fileId: file.id,
                fileName: file.name,
                fileUrl: file.downloadUrl,
                fileSizeBytes: Int64(file.sizeKB * 1024),
                downloadedBytes: 0,
                status: .pending,
                modelType: model.type.name,
                destinationPath: nil,
                errorMessage: nil,
                createdAt: 0,
                updatedAt: 0
            )
            let downloadId = try await enqueueDownloadUseCase.invoke(download: download)
            trackInteraction(.download)
            let existingDownload = downloads.first(where: { $0.fileId == file.id })
            // Only start download if status is pending (new or re-enqueued)
            if existingDownload == nil || existingDownload?.status == .pending {
                DownloadService.shared.startDownload(
                    downloadId: downloadId.int64Value,
                    url: file.downloadUrl,
                    apiKey: apiKey
                )
            }
        }
    }

    func cancelDownload(_ downloadId: Int64) {
        DownloadService.shared.cancelDownload(downloadId: downloadId)
        Task {
            try? await cancelDownloadUseCase.invoke(id: downloadId)
        }
    }

    func loadReviews() {
        Task {
            isReviewsLoading = true
            do {
                let totals = try await getRatingTotalsUseCase.invoke(modelId: modelId, modelVersionId: nil)
                let page = try await getModelReviewsUseCase.invoke(
                    modelId: modelId, modelVersionId: nil, limit: 20, cursor: nil
                )
                let sorted = sortReviews(page.items.compactMap { $0 as? ResourceReview })
                ratingTotals = totals
                reviews = sorted
                isReviewsLoading = false
            } catch is CancellationError {
                return
            } catch {
                isReviewsLoading = false
            }
        }
    }

    func onReviewSortChanged(_ order: ReviewSortOrder) {
        reviewSortOrder = order
        reviews = sortReviews(reviews)
    }

    func submitReview(modelVersionId: Int64, rating: Int32, recommended: Bool, details: String?) {
        Task {
            isSubmittingReview = true
            do {
                try await submitReviewUseCase.invoke(
                    modelId: modelId,
                    modelVersionId: modelVersionId,
                    rating: rating,
                    recommended: recommended,
                    details: details
                )
                isSubmittingReview = false
                reviewSubmitSuccess = true
                loadReviews()
            } catch is CancellationError {
                return
            } catch {
                isSubmittingReview = false
            }
        }
    }

    func dismissReviewSuccess() {
        reviewSubmitSuccess = false
    }

    func onDisappear() {
        guard let startDate = viewStartDate else { return }
        let durationMs = Int64(Date().timeIntervalSince(startDate) * 1000)
        viewStartDate = nil
        Task {
            try? await trackModelViewUseCase.endView(modelId: modelId, durationMs: durationMs)
        }
    }

    func trackInteraction(_ type: InteractionType) {
        Task {
            try? await trackModelViewUseCase.trackInteraction(
                modelId: modelId, interactionType: type
            )
        }
    }

    private func sortReviews(_ reviews: [ResourceReview]) -> [ResourceReview] {
        switch reviewSortOrder {
        case .newest:
            return reviews.sorted { $0.createdAt > $1.createdAt }
        case .highestRated:
            return reviews.sorted { $0.rating > $1.rating }
        case .lowestRated:
            return reviews.sorted { $0.rating < $1.rating }
        }
    }

    private func loadModel() {
        Task {
            isLoading = true
            error = nil
            do {
                let result = try await getModelDetailUseCase.invoke(modelId: modelId)
                model = result
                isLoading = false
                enrichCurrentVersion()
                let thumbUrl = result.modelVersions.first?.images.first?.url
                try? await trackModelViewUseCase.invoke(
                    modelId: result.id,
                    modelName: result.name,
                    modelType: result.type.name,
                    creatorName: result.creator?.username,
                    thumbnailUrl: thumbUrl,
                    tags: result.tags
                )
                viewStartDate = Date()
            } catch is CancellationError {
                return
            } catch {
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }
}
