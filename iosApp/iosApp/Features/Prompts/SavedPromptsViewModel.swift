import Foundation
import Shared

enum PromptTab: String, CaseIterable {
    case all = "All"
    case history = "History"
    case templates = "Templates"
}

@MainActor
final class SavedPromptsViewModel: ObservableObject {
    @Published var prompts: [SavedPrompt] = []
    @Published var templates: [SavedPrompt] = []
    @Published var selectedTab: PromptTab = .all
    @Published var searchQuery: String = ""

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

    private let observeSavedPromptsUseCase: ObserveSavedPromptsUseCase
    private let deleteSavedPromptUseCase: DeleteSavedPromptUseCase
    private let observeTemplatesUseCase: ObserveTemplatesUseCase
    private let toggleTemplateUseCase: ToggleTemplateUseCase
    private let updatePromptCategoryUseCase: UpdatePromptCategoryUseCase

    private var observeTask: Task<Void, Never>?
    private var observeTemplatesTask: Task<Void, Never>?

    init() {
        self.observeSavedPromptsUseCase = KoinHelper.shared.getObserveSavedPromptsUseCase()
        self.deleteSavedPromptUseCase = KoinHelper.shared.getDeleteSavedPromptUseCase()
        self.observeTemplatesUseCase = KoinHelper.shared.getObserveTemplatesUseCase()
        self.toggleTemplateUseCase = KoinHelper.shared.getToggleTemplateUseCase()
        self.updatePromptCategoryUseCase = KoinHelper.shared.getUpdatePromptCategoryUseCase()
        observeTask = Task { await observeAll() }
        observeTemplatesTask = Task { await observeTemplates() }
    }

    deinit {
        observeTask?.cancel()
        observeTemplatesTask?.cancel()
    }

    func observeAll() async {
        for await list in observeSavedPromptsUseCase.invoke() {
            let items = list.compactMap { $0 as? SavedPrompt }
            self.prompts = items
        }
    }

    func observeTemplates() async {
        for await list in observeTemplatesUseCase.invoke() {
            let items = list.compactMap { $0 as? SavedPrompt }
            self.templates = items
        }
    }

    func delete(id: Int64) {
        Task {
            try await deleteSavedPromptUseCase.invoke(id: id)
        }
    }

    func toggleTemplate(id: Int64, isTemplate: Bool, templateName: String?) {
        Task {
            try await toggleTemplateUseCase.invoke(id: id, isTemplate: isTemplate, templateName: templateName)
        }
    }

    func updateCategory(id: Int64, category: String?) {
        Task {
            try await updatePromptCategoryUseCase.invoke(id: id, category: category)
        }
    }
}
