import Foundation
import Shared

@MainActor
final class ComfyHubBrowserViewModelOwner: ObservableObject {
    let vm: ComfyHubBrowserViewModel
    private let store = ViewModelStore()

    @Published var workflows: [ComfyHubWorkflow] = []
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var query: String = ""
    @Published var selectedCategory: ComfyHubCategory = .all

    init() {
        vm = KoinHelper.shared.createComfyHubBrowserViewModel()
        store.put(key: "ComfyHubBrowserViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            workflows = state.workflows as? [ComfyHubWorkflow] ?? []
            isLoading = state.isLoading
            error = state.error
            query = state.query
            selectedCategory = state.selectedCategory
        }
    }

    func onQueryChanged(_ query: String) {
        self.query = query
        vm.onQueryChange(query: query)
    }
    func onCategorySelected(_ category: ComfyHubCategory) {
        selectedCategory = category
        vm.onCategorySelected(category: category)
    }
    func retry() { vm.retry() }
}
