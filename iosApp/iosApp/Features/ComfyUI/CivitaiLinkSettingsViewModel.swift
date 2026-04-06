import Foundation
import Shared

@MainActor
final class CivitaiLinkSettingsViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiCivitaiLinkSettingsViewModel
    private let store = ViewModelStore()

    @Published var linkKey: String = ""
    @Published var status: CivitaiLinkStatus = .disconnected
    @Published var activities: [CivitaiLinkActivity] = []
    @Published var isSaving = false

    init() {
        vm = KoinHelper.shared.createCivitaiLinkSettingsViewModel()
        store.put(key: "CivitaiLinkSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            linkKey = state.linkKey
            status = state.status
            activities = state.activities as? [CivitaiLinkActivity] ?? []
            isSaving = state.isSaving
        }
    }

    func onKeyChanged(_ key: String) {
        linkKey = key
        vm.onKeyChanged(key: key)
    }
    func saveAndConnect() { vm.onSaveAndConnect() }
    func onDisconnect() { vm.onDisconnect() }
    func onCancelActivity(id: String) { vm.onCancelActivity(activityId: id) }

    var isConnected: Bool { status == .connected }
}
