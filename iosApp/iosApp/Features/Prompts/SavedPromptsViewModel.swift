import Foundation
import Shared

@MainActor
final class SavedPromptsViewModel: ObservableObject {
    @Published var prompts: [SavedPrompt] = []

    private let observeSavedPromptsUseCase: ObserveSavedPromptsUseCase
    private let deleteSavedPromptUseCase: DeleteSavedPromptUseCase

    private var observeTask: Task<Void, Never>?

    init() {
        self.observeSavedPromptsUseCase = KoinHelper.shared.getObserveSavedPromptsUseCase()
        self.deleteSavedPromptUseCase = KoinHelper.shared.getDeleteSavedPromptUseCase()
        observeTask = Task { await observe() }
    }

    deinit {
        observeTask?.cancel()
    }

    func observe() async {
        for await list in observeSavedPromptsUseCase.invoke() {
            let items = list.compactMap { $0 as? SavedPrompt }
            self.prompts = items
        }
    }

    func delete(id: Int64) {
        Task {
            try await deleteSavedPromptUseCase.invoke(id: id)
        }
    }
}
