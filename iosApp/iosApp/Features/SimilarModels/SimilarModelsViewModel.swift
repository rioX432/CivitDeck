import Foundation
import Shared

@MainActor
final class SimilarModelsViewModel: ObservableObject {
    @Published var sourceModel: Model?
    @Published var similarModels: [Model] = []
    @Published var isLoading: Bool = true
    @Published var error: String?

    private let modelId: Int64
    private let getModelDetailUseCase: GetModelDetailUseCase
    private let getSimilarModelsUseCase: GetSimilarModelsUseCase

    init(modelId: Int64) {
        self.modelId = modelId
        self.getModelDetailUseCase = KoinHelper.shared.getModelDetailUseCase()
        self.getSimilarModelsUseCase = KoinHelper.shared.getSimilarModelsUseCase()
        Task { await loadSimilarModels() }
    }

    func retry() {
        Task { await loadSimilarModels() }
    }

    private func loadSimilarModels() async {
        isLoading = true
        error = nil
        do {
            let source = try await getModelDetailUseCase.invoke(modelId: modelId)
            sourceModel = source
            let similar = try await getSimilarModelsUseCase.invoke(
                sourceModel: source,
                limit: Int32(20)
            )
            similarModels = similar
            isLoading = false
        } catch {
            self.error = error.localizedDescription
            isLoading = false
        }
    }
}
