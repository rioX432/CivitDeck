import Foundation
import Shared

@MainActor
final class PluginListViewModelOwner: ObservableObject {
    @Published var plugins: [InstalledPlugin] = []
    @Published var isLoading = true
    @Published var errorMessage: String?

    private let vm: PluginManagementViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createPluginManagementViewModel()
        store.put(key: "PluginManagementViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            plugins = state.plugins as? [InstalledPlugin] ?? []
            isLoading = state.isLoading
            errorMessage = state.error
        }
    }

    func togglePlugin(_ plugin: InstalledPlugin, isActive: Bool) {
        vm.togglePlugin(pluginId: plugin.id, isActive: isActive)
    }

    func isActive(_ plugin: InstalledPlugin) -> Bool {
        vm.isPluginActive(plugin: plugin)
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
