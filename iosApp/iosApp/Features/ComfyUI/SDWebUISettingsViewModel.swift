import Foundation
import Shared

@MainActor
class SDWebUISettingsViewModel: ObservableObject {
    @Published var connections: [SDWebUIConnection] = []
    @Published var activeConnection: SDWebUIConnection?
    @Published var isTesting = false
    @Published var testError: String?
    @Published var showAddSheet = false
    @Published var editingConnection: SDWebUIConnection?

    private let observeConnections = KoinHelper.shared.getObserveSDWebUIConnectionsUseCase()
    private let observeActive = KoinHelper.shared.getObserveActiveSDWebUIConnectionUseCase()
    private let saveConnection = KoinHelper.shared.getSaveSDWebUIConnectionUseCase()
    private let deleteConnection = KoinHelper.shared.getDeleteSDWebUIConnectionUseCase()
    private let activateConnection = KoinHelper.shared.getActivateSDWebUIConnectionUseCase()
    private let testConnection = KoinHelper.shared.getTestSDWebUIConnectionUseCase()

    func observeConnectionsList() async {
        for await list in observeConnections.invoke() {
            self.connections = list
        }
    }

    func observeActiveConn() async {
        for await conn in observeActive.invoke() {
            self.activeConnection = conn
        }
    }

    func onSave(name: String, hostname: String, port: Int32) {
        Task {
            let conn = SDWebUIConnection(
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
