import Foundation
import Shared

@MainActor
final class PluginListViewModel: ObservableObject {
    @Published var plugins: [InstalledPlugin] = []
    @Published var isLoading = true
    @Published var errorMessage: String?

    private let observeInstalledPluginsUseCase: ObserveInstalledPluginsUseCase
    private let activatePluginUseCase: ActivatePluginUseCase
    private let deactivatePluginUseCase: DeactivatePluginUseCase

    init() {
        self.observeInstalledPluginsUseCase = KoinHelper.shared.getObserveInstalledPluginsUseCase()
        self.activatePluginUseCase = KoinHelper.shared.getActivatePluginUseCase()
        self.deactivatePluginUseCase = KoinHelper.shared.getDeactivatePluginUseCase()
    }

    func observePlugins() async {
        isLoading = true
        do {
            for try await plugins in observeInstalledPluginsUseCase.invoke() {
                let swiftPlugins = plugins.compactMap { $0 as? InstalledPlugin }
                self.plugins = swiftPlugins
                self.isLoading = false
            }
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }

    func togglePlugin(_ plugin: InstalledPlugin, isActive: Bool) {
        Task {
            do {
                if isActive {
                    try await activatePluginUseCase.invoke(pluginId: plugin.id)
                } else {
                    try await deactivatePluginUseCase.invoke(pluginId: plugin.id)
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func isActive(_ plugin: InstalledPlugin) -> Bool {
        plugin.state == .active
    }

    func typeLabel(for type: InstalledPluginType) -> String {
        switch type {
        case .workflowEngine:
            return "Workflow"
        case .exportFormat:
            return "Export"
        case .theme:
            return "Theme"
        default:
            return "Unknown"
        }
    }
}
