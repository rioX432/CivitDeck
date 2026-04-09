import SwiftUI
import Shared

// MARK: - Civitai Link Send Owner
@MainActor
final class CivitaiLinkSendViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiCivitaiLinkSendViewModel
    private let store = ViewModelStore()

    @Published var status: CivitaiLinkStatus = .disconnected

    var isConnected: Bool { status == .connected }

    init() {
        vm = KoinHelper.shared.createCivitaiLinkSendViewModel()
        store.put(key: "CivitaiLinkSendViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeStatus() async {
        for await s in vm.status {
            self.status = s
        }
    }

    func sendToPC(versionId: Int64, modelId: Int64, versionName: String, downloadUrl: String) {
        let resource = CivitaiLinkResource(
            versionId: versionId,
            modelId: modelId,
            versionName: versionName,
            downloadUrl: downloadUrl
        )
        vm.sendToPC(resource: resource)
    }
}

struct CivitaiLinkSendSheet: View {
    let model: Model
    @StateObject private var vmOwner = CivitaiLinkSendViewModelOwner()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            Group {
                if vmOwner.isConnected {
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
        .task { await vmOwner.observeStatus() }
    }

    private var notConnectedView: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "link.circle").font(.civitIconExtraLarge)
                .accessibilityHidden(true)
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
                    vmOwner.sendToPC(
                        versionId: version.id,
                        modelId: model.id,
                        versionName: version.name,
                        downloadUrl: CivitAiUrls.downloadUrl(versionId: version.id)
                    )
                    dismiss()
                }
            }
        }
    }
}
