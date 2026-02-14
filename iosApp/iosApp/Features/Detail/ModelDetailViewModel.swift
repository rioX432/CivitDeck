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
    private let getImagesUseCase: GetImagesUseCase
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
        self.getImagesUseCase = KoinHelper.shared.getImagesUseCase()
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
                let result = try await getImagesUseCase.invoke(
                    modelId: nil,
                    modelVersionId: KotlinLong(value: versionId),
                    username: nil,
                    sort: nil,
                    period: nil,
                    nsfwLevel: nil,
                    limit: KotlinInt(value: 20),
                    cursor: nil
                )
                let metaByImageId = Dictionary(
                    uniqueKeysWithValues: result.items
                        .compactMap { img -> (String, ImageGenerationMeta)? in
                            guard let meta = img.meta,
                                  let imageId = Self.extractImageId(img.url)
                            else { return nil }
                            return (imageId, meta)
                        }
                )
                guard !metaByImageId.isEmpty else { return }
                let enrichedImages: [ModelImage] = version.images.map { image in
                    if image.meta == nil,
                       let imageId = Self.extractImageId(image.url),
                       let meta = metaByImageId[imageId] {
                        return ModelImage(
                            url: image.url,
                            nsfw: image.nsfw,
                            nsfwLevel: image.nsfwLevel,
                            width: image.width,
                            height: image.height,
                            hash: image.hash,
                            meta: meta
                        )
                    }
                    return image
                }
                let updatedVersions: [ModelVersion] = model.modelVersions.map { v in
                    if v.id == versionId {
                        return ModelVersion(
                            id: v.id,
                            modelId: v.modelId,
                            name: v.name,
                            description: v.description_,
                            createdAt: v.createdAt,
                            baseModel: v.baseModel,
                            trainedWords: v.trainedWords,
                            downloadUrl: v.downloadUrl,
                            files: v.files,
                            images: enrichedImages,
                            stats: v.stats
                        )
                    }
                    return v
                }
                self.model = Model(
                    id: model.id,
                    name: model.name,
                    description: model.description_,
                    type: model.type,
                    nsfw: model.nsfw,
                    tags: model.tags,
                    mode: model.mode,
                    creator: model.creator,
                    stats: model.stats,
                    modelVersions: updatedVersions
                )
            } catch is CancellationError {
                return
            } catch {
                enrichedVersionIds.remove(versionId)
            }
        }
    }

    // URL format: https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/{uuid}/...
    private static func extractImageId(_ url: String) -> String? {
        let components = url.components(separatedBy: "/")
        guard components.count > 4 else { return nil }
        return components[4]
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
