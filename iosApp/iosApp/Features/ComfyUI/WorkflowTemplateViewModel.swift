import Foundation
import Shared

@MainActor
final class WorkflowTemplateViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiWorkflowTemplateViewModel
    private let store = ViewModelStore()

    @Published var templates: [WorkflowTemplate] = []
    @Published var filteredTemplates: [WorkflowTemplate] = []
    @Published var isLoading = true
    @Published var error: String?
    @Published var exportedJson: String?
    @Published var importError: String?
    @Published var searchQuery = ""
    @Published var selectedCategory: WorkflowTemplateCategory?
    @Published var selectedType: WorkflowTemplateType?

    init() {
        vm = KoinHelper.shared.createWorkflowTemplateViewModel()
        store.put(key: "WorkflowTemplateViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            templates = state.templates as? [WorkflowTemplate] ?? []
            filteredTemplates = state.filteredTemplates as? [WorkflowTemplate] ?? []
            isLoading = state.isLoading
            error = state.error
            exportedJson = state.exportedJson
            importError = state.importError
            searchQuery = state.searchQuery
            selectedCategory = state.selectedCategory
            selectedType = state.selectedType
        }
    }

    func onSearchQueryChanged(_ query: String) {
        searchQuery = query
        vm.onSearchQueryChanged(query: query)
    }
    func onCategorySelected(_ category: WorkflowTemplateCategory?) {
        selectedCategory = category
        vm.onCategorySelected(category: category)
    }
    func onTypeSelected(_ type: WorkflowTemplateType?) {
        selectedType = type
        vm.onTypeSelected(type: type)
    }
    func onDeleteTemplate(id: Int64) { vm.onDeleteTemplate(id: id) }
    func onExportTemplate(_ template: WorkflowTemplate) { vm.onExportTemplate(template: template) }
    func onDismissExport() { vm.onDismissExport() }
    func onImportTemplate(jsonString: String) { vm.onImportTemplate(jsonString: jsonString) }
    func onDismissImportError() { vm.onDismissImportError() }
    func onSaveTemplate(_ template: WorkflowTemplate) { vm.onSaveTemplate(template: template) }
    func dismissError() { vm.dismissError() }

    // MARK: - Static factory helpers (kept from KMP companion object)

    static func emptyTemplate(
        type: WorkflowTemplateType = WorkflowTemplateType.txt2Img
    ) -> WorkflowTemplate {
        Feature_comfyuiWorkflowTemplateViewModel.companion.emptyTemplate(type: type)
    }

    static func defaultVariablesFor(type: WorkflowTemplateType) -> [TemplateVariable] {
        Feature_comfyuiWorkflowTemplateViewModel.companion.defaultVariablesFor(type: type) as? [TemplateVariable] ?? []
    }
}
