import Foundation
import Shared

@MainActor
final class ComfyUISettingsViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiComfyUISettingsViewModel
    private let store = ViewModelStore()

    @Published var connections: [ComfyUIConnection] = []
    @Published var activeConnection: ComfyUIConnection?
    @Published var connectionStatus: Core_domainComfyUIConnectionStatus = .notConfigured
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConnection: ComfyUIConnection?

    init() {
        vm = KoinHelper.shared.createComfyUISettingsViewModel()
        store.put(key: "ComfyUISettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            connections = state.connections as? [ComfyUIConnection] ?? []
            activeConnection = state.activeConnection
            connectionStatus = state.connectionStatus
            isTesting = state.isTesting
            testError = state.testError
            showAddSheet = state.showAddDialog
            editingConnection = state.editingConnection
        }
    }

    func onSave(name: String, hostname: String, port: Int32) {
        vm.onSaveConnection(name: name, hostname: hostname, port: port)
    }
    func onDelete(id: Int64) { vm.onDeleteConnection(id: id) }
    func onActivate(id: Int64) { vm.onActivateConnection(id: id) }
    func onTest() { vm.onTestConnection() }
    func onShowAddDialog() {
        showAddSheet = true
        vm.onShowAddDialog()
    }
    func onEditConnection(_ connection: ComfyUIConnection) {
        editingConnection = connection
        showAddSheet = true
        vm.onEditConnection(connection: connection)
    }
    func onDismissDialog() {
        showAddSheet = false
        editingConnection = nil
        vm.onDismissDialog()
    }

    var isConnected: Bool {
        activeConnection?.lastTestSuccess?.boolValue == true
    }
}
