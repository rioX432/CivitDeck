import SwiftUI
import Shared

// MARK: - Civitai Link Send
@MainActor
class CivitaiLinkSendViewModel: ObservableObject {
    @Published var status: CivitaiLinkStatus = .disconnected
    private let observeStatus = KoinHelper.shared.getObserveCivitaiLinkStatusUseCase()
    private let sendResource = KoinHelper.shared.getSendResourceToPCUseCase()

    var isConnected: Bool { status == .connected }

    func observeLinkStatus() async {
        for await s in observeStatus.invoke() { self.status = s }
    }

    func sendToPC(versionId: Int64, modelId: Int64, versionName: String, downloadUrl: String) {
        Task {
            let resource = CivitaiLinkResource(
                versionId: versionId,
                modelId: modelId,
                versionName: versionName,
                downloadUrl: downloadUrl
            )
            try? await sendResource.invoke(resource: resource)
        }
    }
}

struct CivitaiLinkSendSheet: View {
    let model: Model
    @StateObject private var vm = CivitaiLinkSendViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            Group {
                if vm.isConnected {
                    connectedView
                } else {
                    notConnectedView
                }
            }
            .navigationTitle("Send to PC")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .task { await vm.observeLinkStatus() }
    }

    private var notConnectedView: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "link.circle").font(.civitIconExtraLarge)
            Text("Civitai Link not configured").font(.civitTitleMedium)
            Text("Set up Civitai Link in Settings \u{2192} Advanced to send models to your PC")
                .font(.civitBodySmall)
                .multilineTextAlignment(.center)
                .foregroundColor(.civitOnSurfaceVariant)
        }
        .padding(Spacing.lg)
    }

    private var connectedView: some View {
        List {
            if let version = model.modelVersions.first {
                Button("Send \(version.name) to PC") {
                    vm.sendToPC(
                        versionId: version.id,
                        modelId: model.id,
                        versionName: version.name,
                        downloadUrl: "https://civitai.com/api/download/models/\(version.id)"
                    )
                    dismiss()
                }
            }
        }
    }
}
