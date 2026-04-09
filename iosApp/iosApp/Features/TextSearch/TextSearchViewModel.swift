import Foundation
import Shared

@MainActor
final class TextSearchViewModelOwner: ObservableObject {
    @Published var query: String = ""
    @Published var results: [Model] = []
    @Published var isLoading: Bool = false
    @Published var isModelAvailable: Bool = false
    @Published var hasSearched: Bool = false
    @Published var error: String?

    private let vm: TextSearchViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createTextSearchViewModel()
        store.put(key: "TextSearchViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            query = state.query
            results = state.results as? [Model] ?? []
            isLoading = state.isLoading
            isModelAvailable = state.isModelAvailable
            hasSearched = state.hasSearched
            error = state.error
        }
    }

    func onQueryChanged(_ newQuery: String) {
        vm.onQueryChanged(query: newQuery)
    }

    func search() {
        vm.search()
    }

    func retry() {
        vm.retry()
    }
}
