import Foundation
import Shared

@MainActor
final class ComfyUIQueueViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiComfyUIQueueViewModel
    private let store = ViewModelStore()

    @Published var jobs: [QueueJob] = []
    @Published var isLoading = true
    @Published var error: String?
    @Published var cancellingIds: Set<String> = []

    init() {
        vm = KoinHelper.shared.createComfyUIQueueViewModel()
        store.put(key: "ComfyUIQueueViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            jobs = state.jobs as? [QueueJob] ?? []
            isLoading = state.isLoading
            error = state.error
            cancellingIds = Set((state.cancellingIds as? Set<String>) ?? [])
        }
    }

    func onCancelJob(promptId: String) { vm.onCancelJob(promptId: promptId) }
    func dismissError() { vm.dismissError() }
}
