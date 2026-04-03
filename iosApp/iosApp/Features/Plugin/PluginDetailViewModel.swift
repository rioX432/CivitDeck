import Foundation
import Shared

@MainActor
final class PluginDetailViewModelOwner: ObservableObject {
    @Published var plugins: [InstalledPlugin] = []
    @Published var configJson: String = "{}"
    @Published var isLoading = true
    @Published var errorMessage: String?

    private let pluginId: String
    private let vm: PluginManagementViewModel
    private let store: ViewModelStore

    var plugin: InstalledPlugin? {
        plugins.first { $0.id == pluginId }
    }

    init(pluginId: String) {
        self.pluginId = pluginId
        store = ViewModelStore()
        vm = KoinHelper.shared.createPluginManagementViewModel()
        store.put(key: "PluginDetailViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func load() async {
        vm.loadConfig(pluginId: pluginId)
        for await state in vm.uiState {
            plugins = state.plugins as? [InstalledPlugin] ?? []
            configJson = state.selectedPluginConfig
            isLoading = state.isLoading
            errorMessage = state.error
        }
    }

    func togglePlugin(isActive: Bool) {
        vm.togglePlugin(pluginId: pluginId, isActive: isActive)
    }

    func saveConfig(_ json: String) {
        vm.saveConfig(pluginId: pluginId, configJson: json)
    }

    func uninstall() {
        vm.uninstallPlugin(pluginId: pluginId)
    }

    var isActive: Bool {
        plugin?.state == .active
    }

    func typeLabel(for type: InstalledPluginType) -> String {
        switch type {
        case .workflowEngine: return "Workflow"
        case .exportFormat: return "Export"
        case .theme: return "Theme"
        default: return "Unknown"
        }
    }
}
