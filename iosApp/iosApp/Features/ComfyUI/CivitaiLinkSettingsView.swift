import SwiftUI
import Shared

struct CivitaiLinkSettingsView: View {
    @StateObject private var viewModel = CivitaiLinkSettingsViewModelOwner()

    var body: some View {
        List {
            subscriptionRequiredBanner
            statusSection
            configSection
            if !viewModel.activities.isEmpty {
                activitiesSection
            }
        }
        .navigationTitle("Civitai Link")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observeUiState() }
    }

    private var subscriptionRequiredBanner: some View {
        Section {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Requires CivitAI Supporter+ subscription")
                    .font(.civitBodyMedium)
                Text("Civitai Link is only available to CivitAI Supporter+ members. Subscribe at civitai.com/pricing")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            .padding(.vertical, Spacing.xs)
        }
    }

    private var statusSection: some View {
        Section {
            HStack {
                Circle()
                    .fill(statusColor)
                    // Status indicator dot — intentionally small, one-off size
                    .frame(width: Spacing.smPlus, height: Spacing.smPlus)
                Text(statusLabel)
                    .font(.civitBodyMedium)
                Spacer()
                if viewModel.isConnected {
                    Button("Disconnect", action: viewModel.onDisconnect)
                        .foregroundColor(.civitError)
                }
            }
        } header: {
            Text("Status")
        }
    }

    private var configSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Link Key").font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                TextField("Paste your Civitai Link key here", text: $viewModel.linkKey)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
            }
            Button(viewModel.isSaving ? "Connecting..." : "Save & Connect") {
                viewModel.saveAndConnect()
            }
            .disabled(viewModel.linkKey.isEmpty || viewModel.isSaving)
        } header: {
            Text("Configuration")
        } footer: {
            Text("Get your link key from civitai.com \u{2192} Account Settings \u{2192} Civitai Link")
                .font(.civitBodySmall)
        }
    }

    private var activitiesSection: some View {
        Section("Downloads on PC") {
            ForEach(viewModel.activities, id: \.id) { activity in
                CivitaiLinkActivityRow(activity: activity) {
                    viewModel.onCancelActivity(id: activity.id)
                }
            }
        }
    }

    private var statusColor: Color {
        switch viewModel.status {
        case .connected: return .green
        case .connecting: return .yellow
        case .error: return .red
        default: return .gray
        }
    }

    private var statusLabel: String {
        switch viewModel.status {
        case .connected: return "Connected"
        case .connecting: return "Connecting..."
        case .error: return "Connection Error"
        default: return "Disconnected"
        }
    }
}

private struct CivitaiLinkActivityRow: View {
    let activity: CivitaiLinkActivity
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(activity.type).font(.civitBodyMedium)
                Spacer()
                Text(activity.status).font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
            }
            if activity.status == "Running" {
                ProgressView(value: activity.progress)
                Button("Cancel", action: onCancel).font(.civitBodySmall).foregroundColor(.civitError)
            }
        }
    }
}
