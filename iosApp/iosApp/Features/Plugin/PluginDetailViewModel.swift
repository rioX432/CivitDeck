import Foundation
import Shared

@MainActor
final class PluginDetailViewModel: ObservableObject {
    @Published var plugin: InstalledPlugin?
    @Published var configJson: String = "{}"
    @Published var isLoading = true
    @Published var errorMessage: String?

    private let pluginId: String
    private let observeInstalledPluginsUseCase: ObserveInstalledPluginsUseCase
    private let activatePluginUseCase: ActivatePluginUseCase
    private let deactivatePluginUseCase: DeactivatePluginUseCase
    private let uninstallPluginUseCase: UninstallPluginUseCase
    private let getPluginConfigUseCase: GetPluginConfigUseCase
    private let updatePluginConfigUseCase: UpdatePluginConfigUseCase

    init(pluginId: String) {
        self.pluginId = pluginId
        self.observeInstalledPluginsUseCase = KoinHelper.shared.getObserveInstalledPluginsUseCase()
        self.activatePluginUseCase = KoinHelper.shared.getActivatePluginUseCase()
        self.deactivatePluginUseCase = KoinHelper.shared.getDeactivatePluginUseCase()
        self.uninstallPluginUseCase = KoinHelper.shared.getUninstallPluginUseCase()
        self.getPluginConfigUseCase = KoinHelper.shared.getGetPluginConfigUseCase()
        self.updatePluginConfigUseCase = KoinHelper.shared.getUpdatePluginConfigUseCase()
    }

    func load() async {
        isLoading = true
        do {
            let config = try await getPluginConfigUseCase.invoke(pluginId: pluginId)
            self.configJson = config
            for try await plugins in observeInstalledPluginsUseCase.invoke() {
                let swiftPlugins = plugins.compactMap { $0 as? InstalledPlugin }
                self.plugin = swiftPlugins.first { $0.id == pluginId }
                self.isLoading = false
            }
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }

    func togglePlugin(isActive: Bool) {
        Task {
            do {
                if isActive {
                    try await activatePluginUseCase.invoke(pluginId: pluginId)
                } else {
                    try await deactivatePluginUseCase.invoke(pluginId: pluginId)
                }
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func saveConfig(_ json: String) {
        Task {
            do {
                try await updatePluginConfigUseCase.invoke(pluginId: pluginId, configJson: json)
                configJson = json
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func uninstall() async -> Bool {
        do {
            try await uninstallPluginUseCase.invoke(pluginId: pluginId)
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    var isActive: Bool {
        plugin?.state == .active
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
