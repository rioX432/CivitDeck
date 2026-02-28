import Foundation
import Shared

@MainActor
class CivitaiLinkSettingsViewModel: ObservableObject {
    @Published var linkKey: String = ""
    @Published var status: CivitaiLinkStatus = .disconnected
    @Published var activities: [CivitaiLinkActivity] = []
    @Published var isSaving = false

    private let observeKey = KoinHelper.shared.getObserveCivitaiLinkKeyUseCase()
    private let setKey = KoinHelper.shared.getSetCivitaiLinkKeyUseCase()
    private let observeStatus = KoinHelper.shared.getObserveCivitaiLinkStatusUseCase()
    private let observeActivities = KoinHelper.shared.getObserveCivitaiLinkActivitiesUseCase()
    private let connect = KoinHelper.shared.getConnectCivitaiLinkUseCase()
    private let disconnect = KoinHelper.shared.getDisconnectCivitaiLinkUseCase()
    private let cancelActivity = KoinHelper.shared.getCancelLinkActivityUseCase()

    func observeLinkKey() async {
        for await key in observeKey.invoke() {
            if self.linkKey.isEmpty {
                self.linkKey = key ?? ""
            }
        }
    }

    func observeLinkStatus() async {
        for await s in observeStatus.invoke() {
            self.status = s
        }
    }

    func observeLinkActivities() async {
        for await acts in observeActivities.invoke() {
            self.activities = acts
        }
    }

    func saveAndConnect() {
        guard !linkKey.isEmpty else { return }
        isSaving = true
        Task {
            try? await setKey.invoke(key: linkKey)
            _ = try? await connect.invoke()
            isSaving = false
        }
    }

    func onDisconnect() {
        disconnect.invoke()
    }

    func onCancelActivity(id: String) {
        Task { try? await cancelActivity.invoke(activityId: id) }
    }

    var isConnected: Bool { status == .connected }
}
