import Foundation
import Shared

@MainActor
final class SDWebUISettingsViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiSDWebUISettingsViewModel
    private let store = ViewModelStore()

    @Published var connections: [SDWebUIConnection] = []
    @Published var activeConnection: SDWebUIConnection?
    @Published var connectionStatus: Core_domainSDWebUIConnectionStatus = .notConfigured
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConnection: SDWebUIConnection?

    init() {
        vm = KoinHelper.shared.createSDWebUISettingsViewModel()
        store.put(key: "SDWebUISettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            connections = state.connections as? [SDWebUIConnection] ?? []
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
    func onEditConnection(_ conn: SDWebUIConnection) {
        editingConnection = conn
        showAddSheet = true
        vm.onEditConnection(conn: conn)
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
