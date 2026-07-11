import SwiftUI
import Shared

/// Create hub with live per-server connection status (parity with the
/// Android/Desktop Create hub): configured servers lead with a status card
/// and quick actions, unconfigured ones collapse into "Connect" rows.
struct CreateHubView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @StateObject private var comfyViewModel = ComfyUISettingsViewModelOwner()
    @StateObject private var sdViewModel = SDWebUISettingsViewModelOwner()
    @StateObject private var externalViewModel = ExternalServerSettingsViewModelOwner()

    var body: some View {
        if sizeClass == .regular {
            createContent
        } else {
            NavigationStack {
                createContent
            }
        }
    }

    private var comfyConfigured: Bool { comfyViewModel.activeConnection != nil }
    private var sdConfigured: Bool { sdViewModel.activeConnection != nil }
    private var externalConfigured: Bool { externalViewModel.activeConfig != nil }
    private var nothingConfigured: Bool {
        !comfyConfigured && !sdConfigured && !externalConfigured
    }

    private var createContent: some View {
        List {
            if nothingConfigured {
                connectHeroSection
            }
            if comfyConfigured {
                comfyServerSection
            }
            if sdConfigured {
                sdServerSection
            }
            if externalConfigured {
                externalServerSection
            }
            connectMoreSection
            Section {
                NavigationLink {
                    ModelFileBrowserScreen()
                } label: {
                    hubRow(
                        icon: "doc.text",
                        title: "Model Files",
                        description: "Browse and manage local model files"
                    )
                }
            }
        }
        .navigationTitle("Create")
        .task { await comfyViewModel.observeUiState() }
        .task { await sdViewModel.observeUiState() }
        .task { await externalViewModel.observeUiState() }
    }

    // MARK: - Empty state

    private var connectHeroSection: some View {
        Section {
            VStack(spacing: Spacing.sm) {
                Image(systemName: "sparkles")
                    .font(.largeTitle)
                    .foregroundColor(.civitPrimary)
                    .accessibilityHidden(true)
                Text("Connect a generation server")
                    .font(.civitTitleSmall)
                Text("Control ComfyUI or SD WebUI on your PC from CivitDeck")
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .multilineTextAlignment(.center)
                NavigationLink {
                    ConnectionOnboardingView()
                } label: {
                    Text("Set up ComfyUI")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.md)
        }
    }

    // MARK: - Configured servers

    private var comfyServerSection: some View {
        Section {
            NavigationLink {
                ComfyUISettingsView()
            } label: {
                serverRow(
                    icon: "sparkles",
                    title: "ComfyUI",
                    subtitle: comfyViewModel.activeConnection.map { "\($0.hostname):\($0.port)" },
                    status: comfyHubStatus
                )
            }
            NavigationLink("Generate") { ComfyUIGenerationView() }
            NavigationLink("Queue") { ComfyUIQueueView() }
            NavigationLink("Outputs") { ComfyUIHistoryView() }
        }
    }

    private var sdServerSection: some View {
        Section {
            NavigationLink {
                SDWebUISettingsView()
            } label: {
                serverRow(
                    icon: "paintbrush",
                    title: "SD WebUI",
                    subtitle: sdViewModel.activeConnection.map { "\($0.hostname):\($0.port)" },
                    status: sdHubStatus
                )
            }
            NavigationLink("Generate") { SDWebUIGenerationView() }
        }
    }

    private var externalServerSection: some View {
        Section {
            NavigationLink {
                ExternalServerSettingsView()
            } label: {
                serverRow(
                    icon: "server.rack",
                    title: externalViewModel.activeConfig?.name ?? "External Server",
                    subtitle: externalViewModel.activeConfig?.baseUrl,
                    status: externalHubStatus
                )
            }
            NavigationLink("Gallery") {
                ExternalServerGalleryView(
                    serverName: externalViewModel.activeConfig?.name ?? "External Server"
                )
            }
        }
    }

    // MARK: - Connect more

    @ViewBuilder
    private var connectMoreSection: some View {
        // The hero already offers ComfyUI setup when nothing is configured.
        let showComfy = !comfyConfigured && !nothingConfigured
        if showComfy || !sdConfigured || !externalConfigured {
            Section("Connect") {
                if showComfy {
                    NavigationLink {
                        ComfyUISettingsView()
                    } label: {
                        hubRow(
                            icon: "sparkles",
                            title: "ComfyUI",
                            description: "Node-based image generation workflow"
                        )
                    }
                }
                if !sdConfigured {
                    NavigationLink {
                        SDWebUISettingsView()
                    } label: {
                        hubRow(
                            icon: "paintbrush",
                            title: "SD WebUI",
                            description: "Stable Diffusion web interface"
                        )
                    }
                }
                if !externalConfigured {
                    NavigationLink {
                        ExternalServerSettingsView()
                    } label: {
                        hubRow(
                            icon: "server.rack",
                            title: "External Server",
                            description: "Connect to a custom generation server"
                        )
                    }
                }
            }
        }
    }

    // MARK: - Status mapping ("Disconnected" = saved but untested, not a failed test)

    private var comfyHubStatus: (Color, String) {
        switch comfyViewModel.connectionStatus {
        case .connected: return (.green, "Online")
        case .error: return (.red, "Offline")
        default: return (.secondary, "Not tested")
        }
    }

    private var sdHubStatus: (Color, String) {
        switch sdViewModel.connectionStatus {
        case .connected: return (.green, "Online")
        case .error: return (.red, "Offline")
        default: return (.secondary, "Not tested")
        }
    }

    private var externalHubStatus: (Color, String) {
        switch externalViewModel.connectionStatus {
        case .connected: return (.green, "Online")
        case .error: return (.red, "Offline")
        default: return (.secondary, "Not tested")
        }
    }

    // MARK: - Rows

    private func serverRow(
        icon: String,
        title: String,
        subtitle: String?,
        status: (Color, String)
    ) -> some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.civitPrimary)
                .frame(width: 32, height: 32)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .font(.civitTitleSmall)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }

            Spacer()

            HStack(spacing: Spacing.xs) {
                Circle()
                    .fill(status.0)
                    .frame(width: 8, height: 8)
                Text(status.1)
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .padding(.vertical, Spacing.xs)
    }

    private func hubRow(icon: String, title: String, description: String) -> some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.civitPrimary)
                .frame(width: 32, height: 32)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .font(.civitTitleSmall)

                Text(description)
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .padding(.vertical, Spacing.xs)
    }
}
