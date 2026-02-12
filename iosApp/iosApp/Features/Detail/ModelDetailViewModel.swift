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
    }

    func retry() {
        loadModel()
    }

    private func loadModel() {
        Task {
            isLoading = true
            error = nil
            do {
                let result = try await getModelDetailUseCase.invoke(modelId: modelId)
                model = result
                isLoading = false
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
