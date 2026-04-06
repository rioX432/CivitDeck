import Foundation
import Shared

@MainActor
final class ExternalServerSettingsViewModelOwner: ObservableObject {
    let vm: Feature_externalserverExternalServerSettingsViewModel
    private let store = ViewModelStore()

    @Published var configs: [ExternalServerConfig] = []
    @Published var activeConfig: ExternalServerConfig?
    @Published var connectionStatus: Core_domainExternalServerConnectionStatus = .notConfigured
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConfig: ExternalServerConfig?

    init() {
        vm = KoinHelper.shared.createExternalServerSettingsViewModel()
        store.put(key: "ExternalServerSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            configs = state.configs as? [ExternalServerConfig] ?? []
            activeConfig = state.activeConfig
            connectionStatus = state.connectionStatus
            isTesting = state.isTesting
            testError = state.testError
            showAddSheet = state.showAddDialog
            editingConfig = state.editingConfig
        }
    }

    func onSave(name: String, baseUrl: String, apiKey: String) {
        vm.onSaveConfig(name: name, baseUrl: baseUrl, apiKey: apiKey)
    }
    func onDelete(id: Int64) { vm.onDeleteConfig(id: id) }
    func onActivate(id: Int64) { vm.onActivateConfig(id: id) }
    func onTest() { vm.onTestConnection() }
    func onShowAddDialog() {
        showAddSheet = true
        vm.onShowAddDialog()
    }
    func onEditConfig(_ config: ExternalServerConfig) {
        editingConfig = config
        showAddSheet = true
        vm.onEditConfig(config: config)
    }
    func onDismissDialog() {
        showAddSheet = false
        editingConfig = nil
        vm.onDismissDialog()
    }

    var isConnected: Bool {
        activeConfig?.lastTestSuccess?.boolValue == true
    }
}
