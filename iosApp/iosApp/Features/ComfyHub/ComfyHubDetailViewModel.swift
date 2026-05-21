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
    @Published var hasAppMode: Bool = false
    @Published var isSavingTemplate: Bool = false
    @Published var saveTemplateSuccess: Bool = false
    @Published var saveTemplateError: String?

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
            hasAppMode = state.hasAppMode
            isSavingTemplate = state.isSavingTemplate
            saveTemplateSuccess = state.saveTemplateSuccess
            saveTemplateError = state.saveTemplateError
        }
    }

    func retry() { vm.retry() }
    func onImport() { vm.onImport() }
    func onSaveAsTemplate() { vm.onSaveAsTemplate() }
    func dismissImportResult() { vm.dismissImportResult() }
    func dismissSaveTemplateResult() { vm.dismissSaveTemplateResult() }
}
