import Foundation
import Shared

@MainActor
final class ModelCompareViewModel: ObservableObject {
    @Published var leftModel: Model?
    @Published var rightModel: Model?
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var leftSelectedVersionIndex: Int = 0
    @Published var rightSelectedVersionIndex: Int = 0
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off

    private let leftModelId: Int64
    private let rightModelId: Int64
    private let getModelDetailUseCase: GetModelDetailUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let enrichModelImagesUseCase: EnrichModelImagesUseCase
    private var enrichedVersionIds: Set<Int64> = []

    var leftSelectedVersion: ModelVersion? {
        guard let model = leftModel else { return nil }
        let versions = model.modelVersions
        guard leftSelectedVersionIndex < versions.count else { return nil }
        return versions[leftSelectedVersionIndex]
    }

    var rightSelectedVersion: ModelVersion? {
        guard let model = rightModel else { return nil }
        let versions = model.modelVersions
        guard rightSelectedVersionIndex < versions.count else { return nil }
        return versions[rightSelectedVersionIndex]
    }

    init(leftModelId: Int64, rightModelId: Int64) {
        self.leftModelId = leftModelId
        self.rightModelId = rightModelId
        self.getModelDetailUseCase = KoinHelper.shared.getModelDetailUseCase()
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.enrichModelImagesUseCase = KoinHelper.shared.getEnrichModelImagesUseCase()
        loadModels()
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            nsfwFilterLevel = value
        }
    }

    func onLeftVersionSelected(_ index: Int) {
        leftSelectedVersionIndex = index
        enrichVersion(model: leftModel, versionIndex: index, side: .left)
    }

    func onRightVersionSelected(_ index: Int) {
        rightSelectedVersionIndex = index
        enrichVersion(model: rightModel, versionIndex: index, side: .right)
    }

    private func loadModels() {
        Task {
            isLoading = true
            error = nil
            do {
                async let left = getModelDetailUseCase.invoke(modelId: leftModelId)
                async let right = getModelDetailUseCase.invoke(modelId: rightModelId)
                leftModel = try await left
                rightModel = try await right
                isLoading = false
                enrichVersion(model: leftModel, versionIndex: 0, side: .left)
                enrichVersion(model: rightModel, versionIndex: 0, side: .right)
            } catch is CancellationError {
                return
            } catch {
                self.error = error.localizedDescription
                isLoading = false
            }
        }
    }

    private enum Side { case left, right }

    private func enrichVersion(model: Model?, versionIndex: Int, side: Side) {
        guard let model else { return }
        let versions = model.modelVersions
        guard versionIndex < versions.count else { return }
        let version = versions[versionIndex]
        let versionId = version.id
        guard !enrichedVersionIds.contains(versionId) else { return }
        enrichedVersionIds.insert(versionId)
        Task {
            do {
                let enriched = try await enrichModelImagesUseCase.invoke(
                    modelVersionId: versionId,
                    images: version.images
                )
                let updatedVersions: [ModelVersion] = model.modelVersions.map { v in
                    guard v.id == versionId else { return v }
                    return ModelVersion(
                        id: v.id, modelId: v.modelId, name: v.name, description: v.description_,
                        createdAt: v.createdAt, baseModel: v.baseModel, trainedWords: v.trainedWords,
                        downloadUrl: v.downloadUrl, files: v.files, images: enriched, stats: v.stats
                    )
                }
                let updatedModel = Model(
                    id: model.id, name: model.name, description: model.description_,
                    type: model.type, nsfw: model.nsfw, tags: model.tags,
                    mode: model.mode, creator: model.creator,
                    stats: model.stats, modelVersions: updatedVersions
                )
                switch side {
                case .left: leftModel = updatedModel
                case .right: rightModel = updatedModel
                }
            } catch is CancellationError {
                return
            } catch {
                enrichedVersionIds.remove(versionId)
            }
        }
    }
}
