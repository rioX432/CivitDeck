import Foundation
import Shared

@MainActor
class ComfyUISettingsViewModel: ObservableObject {
    @Published var connections: [ComfyUIConnection] = []
    @Published var activeConnection: ComfyUIConnection?
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConnection: ComfyUIConnection?

    private let observeConnections = KoinHelper.shared.getObserveComfyUIConnectionsUseCase()
    private let observeActive = KoinHelper.shared.getObserveActiveComfyUIConnectionUseCase()
    private let saveConnection = KoinHelper.shared.getSaveComfyUIConnectionUseCase()
    private let deleteConnection = KoinHelper.shared.getDeleteComfyUIConnectionUseCase()
    private let activateConnection = KoinHelper.shared.getActivateComfyUIConnectionUseCase()
    private let testConnection = KoinHelper.shared.getTestComfyUIConnectionUseCase()

    func observeConnectionsList() async {
        for await list in observeConnections.invoke() {
            self.connections = (list as? [ComfyUIConnection]) ?? []
        }
    }

    func observeActiveConn() async {
        for await conn in observeActive.invoke() {
            self.activeConnection = conn as? ComfyUIConnection
        }
    }

    func onSave(name: String, hostname: String, port: Int32) {
        Task {
            let conn = ComfyUIConnection(
                id: editingConnection?.id ?? 0,
                name: name,
                hostname: hostname,
                port: port,
                isActive: false,
                lastTestedAt: nil,
                lastTestSuccess: nil
            )
            _ = try await saveConnection.invoke(connection: conn)
            showAddSheet = false
            editingConnection = nil
        }
    }

    func onDelete(id: Int64) {
        Task { try await deleteConnection.invoke(id: id) }
    }

    func onActivate(id: Int64) {
        Task { try await activateConnection.invoke(id: id) }
    }

    func onTest() {
        guard let active = activeConnection else { return }
        isTesting = true
        testError = nil
        Task {
            let success = try await testConnection.invoke(connection: active)
            isTesting = false
            testError = success.boolValue ? nil : "Connection failed"
        }
    }

    var isConnected: Bool {
        activeConnection?.lastTestSuccess?.boolValue == true
    }
}
