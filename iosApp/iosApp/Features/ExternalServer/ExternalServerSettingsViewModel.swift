import Foundation
import Shared

@MainActor
class ExternalServerSettingsViewModel: ObservableObject {
    @Published var configs: [ExternalServerConfig] = []
    @Published var activeConfig: ExternalServerConfig?
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConfig: ExternalServerConfig?

    private let observeConfigs = KoinHelper.shared.getObserveExternalServerConfigsUseCase()
    private let observeActive = KoinHelper.shared.getObserveActiveExternalServerConfigUseCase()
    private let saveConfig = KoinHelper.shared.getSaveExternalServerConfigUseCase()
    private let deleteConfig = KoinHelper.shared.getDeleteExternalServerConfigUseCase()
    private let activateConfig = KoinHelper.shared.getActivateExternalServerConfigUseCase()
    private let testConnection = KoinHelper.shared.getTestExternalServerConnectionUseCase()

    func observeConfigsList() async {
        for await list in observeConfigs.invoke() {
            self.configs = list
        }
    }

    func observeActiveConfig() async {
        for await config in observeActive.invoke() {
            self.activeConfig = config
        }
    }

    func onSave(name: String, baseUrl: String, apiKey: String) {
        Task {
            let config = ExternalServerConfig(
                id: editingConfig?.id ?? 0,
                name: name,
                baseUrl: baseUrl,
                apiKey: apiKey,
                isActive: false,
                lastTestedAt: nil,
                lastTestSuccess: nil,
                createdAt: 0
            )
            _ = try await saveConfig.invoke(config: config)
            showAddSheet = false
            editingConfig = nil
        }
    }

    func onDelete(id: Int64) {
        Task { try await deleteConfig.invoke(id: id) }
    }

    func onActivate(id: Int64) {
        Task { try await activateConfig.invoke(id: id) }
    }

    func onTest() {
        guard let active = activeConfig else { return }
        isTesting = true
        testError = nil
        Task {
            let success = try await testConnection.invoke(config: active)
            isTesting = false
            testError = success.boolValue ? nil : "Connection failed. Check the server URL and API key."
        }
    }

    var isConnected: Bool {
        activeConfig?.lastTestSuccess?.boolValue == true
    }
}
