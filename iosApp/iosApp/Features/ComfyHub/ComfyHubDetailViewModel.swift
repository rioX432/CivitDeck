import Foundation
import Shared

@MainActor
final class ComfyHubDetailViewModelOwner: ObservableObject {
    let vm: ComfyHubDetailViewModel
    private let store = ViewModelStore()

    @Published var workflow: ComfyHubWorkflow?
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var isImporting: Bool = false
    @Published var importSuccess: Bool = false
    @Published var importError: String?
    @Published var nodeNames: [String] = []

    init(workflowId: String) {
        vm = KoinHelper.shared.createComfyHubDetailViewModel(workflowId: workflowId)
        store.put(key: "ComfyHubDetailViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            workflow = state.workflow
            isLoading = state.isLoading
            error = state.error
            isImporting = state.isImporting
            importSuccess = state.importSuccess
            importError = state.importError
            nodeNames = state.nodeNames as? [String] ?? []
        }
    }

    func retry() { vm.retry() }
    func onImport() { vm.onImport() }
    func dismissImportResult() { vm.dismissImportResult() }
}
