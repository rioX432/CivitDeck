import Foundation
import Shared

@MainActor
final class ModelFileBrowserViewModelOwner: ObservableObject {
    let vm: ModelFileBrowserViewModel
    private let store = ViewModelStore()

    @Published var directories: [ModelDirectory] = []
    @Published var files: [LocalModelFile] = []
    @Published var scanStatus: Core_domainScanStatus = .idle
    @Published var scanProgress: String = ""
    @Published var errorMessage: String?

    init() {
        vm = KoinHelper.shared.createModelFileBrowserViewModel()
        store.put(key: "ModelFileBrowserViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            directories = state.directories as? [ModelDirectory] ?? []
            files = state.files as? [LocalModelFile] ?? []
            scanStatus = state.scanStatus
            scanProgress = state.scanProgress
            errorMessage = state.errorMessage
        }
    }

    func onAddDirectory(_ path: String) { vm.onAddDirectory(path: path) }
    func onRemoveDirectory(_ id: Int64) { vm.onRemoveDirectory(id: id) }
    func onScanAll() { vm.onScanAll() }
    func onDismissError() { vm.onDismissError() }
}
