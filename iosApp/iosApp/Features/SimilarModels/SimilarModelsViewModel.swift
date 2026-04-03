import Foundation
import Shared

@MainActor
final class SimilarModelsViewModelOwner: ObservableObject {
    @Published var sourceModel: Model?
    @Published var similarModels: [Model] = []
    @Published var isLoading: Bool = true
    @Published var error: String?

    private let vm: SimilarModelsViewModel
    private let store: ViewModelStore

    init(modelId: Int64) {
        store = ViewModelStore()
        vm = KoinHelper.shared.createSimilarModelsViewModel(modelId: modelId)
        store.put(key: "SimilarModelsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            sourceModel = state.sourceModel
            similarModels = state.similarModels as? [Model] ?? []
            isLoading = state.isLoading
            error = state.error
        }
    }

    func retry() { vm.retry() }
}
