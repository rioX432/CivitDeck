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

    private let modelId: Int64
    private let getModelDetailUseCase: GetModelDetailUseCase
    private let observeIsFavoriteUseCase: ObserveIsFavoriteUseCase
    private let toggleFavoriteUseCase: ToggleFavoriteUseCase
    private let trackModelViewUseCase: TrackModelViewUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let enrichModelImagesUseCase: EnrichModelImagesUseCase
    private var enrichedVersionIds: Set<Int64> = []

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
        loadModel()
    }

    /// Called from SwiftUI .task modifier — observes favorite state reactively via SKIE Flow.
    func observeFavorite() async {
        for await value in observeIsFavoriteUseCase.invoke(modelId: modelId) {
            isFavorite = value.boolValue
        }
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            nsfwFilterLevel = value
        }
    }

    func onFavoriteToggle() {
        guard let model else { return }
        Task {
            try? await toggleFavoriteUseCase.invoke(model: model)
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
                    stats: currentModel.stats, modelVersions: updatedVersions
                )
            } catch is CancellationError {
                return
            } catch {
                enrichedVersionIds.remove(versionId)
            }
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
                try? await trackModelViewUseCase.invoke(
                    modelId: result.id,
                    modelType: result.type.name,
                    creatorName: result.creator?.username,
                    tags: result.tags
                )
            } catch is CancellationError {
                return
            } catch {
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }
}
