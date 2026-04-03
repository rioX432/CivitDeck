import Foundation
import Shared

enum PromptTab: String, CaseIterable {
    case all = "All"
    case history = "History"
    case templates = "Templates"
}

@MainActor
final class SavedPromptsViewModelOwner: ObservableObject {
    @Published var prompts: [SavedPrompt] = []
    @Published var templates: [SavedPrompt] = []
    @Published var selectedTab: PromptTab = .all
    @Published var searchQuery: String = "" {
        didSet { vm.onSearchQueryChanged(query: searchQuery) }
    }

    var displayedPrompts: [SavedPrompt] {
        let source: [SavedPrompt]
        switch selectedTab {
        case .all:
            source = prompts
        case .history:
            source = prompts.filter { $0.autoSaved }
        case .templates:
            source = templates
        }
        guard !searchQuery.isEmpty else { return source }
        let query = searchQuery.lowercased()
        return source.filter { prompt in
            prompt.prompt.lowercased().contains(query) ||
            (prompt.negativePrompt?.lowercased().contains(query) ?? false) ||
            (prompt.modelName?.lowercased().contains(query) ?? false) ||
            (prompt.templateName?.lowercased().contains(query) ?? false)
        }
    }

    let vm: Feature_promptsSavedPromptsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createSavedPromptsViewModel()
        store.put(key: "SavedPromptsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func startObserving() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observePrompts() }
            group.addTask { await self.observeTemplates() }
        }
    }

    private func observePrompts() async {
        for await list in vm.prompts {
            let items = list.compactMap { $0 as? SavedPrompt }
            self.prompts = items
        }
    }

    private func observeTemplates() async {
        for await list in vm.templates {
            let items = list.compactMap { $0 as? SavedPrompt }
            self.templates = items
        }
    }

    func delete(id: Int64) {
        vm.delete(id: id)
    }

    func toggleTemplate(id: Int64, isTemplate: Bool, templateName: String?) {
        vm.toggleTemplate(id: id, isTemplate: isTemplate, templateName: templateName)
    }
}
