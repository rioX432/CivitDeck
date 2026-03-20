import Foundation
import Shared

@MainActor
final class ComfyHubBrowserViewModel: ObservableObject {
    @Published var workflows: [ComfyHubWorkflow] = []
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var query: String = ""
    @Published var selectedCategory: ComfyHubCategory = .all

    private let searchWorkflows = KoinHelper.shared.getSearchComfyHubWorkflowsUseCase()

    init() {
        Task { await search() }
    }

    func onQueryChanged(_ query: String) {
        self.query = query
        Task { await search() }
    }

    func onCategorySelected(_ category: ComfyHubCategory) {
        selectedCategory = category
        Task { await search() }
    }

    func retry() {
        Task { await search() }
    }

    private func search() async {
        isLoading = true
        error = nil
        do {
            let results = try await searchWorkflows.invoke(
                query: query,
                category: selectedCategory,
                sort: .mostDownloaded,
                page: 1
            )
            workflows = results
            isLoading = false
        } catch {
            self.error = error.localizedDescription
            isLoading = false
        }
    }
}
