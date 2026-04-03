import SwiftUI
import Shared

struct AdvancedSettingsView: View {
    let viewModel: AppBehaviorSettingsViewModel
    let displayViewModel: DisplaySettingsViewModel
    @StateObject private var shareHashtagVM = ShareHashtagViewModel()

    var body: some View {
        Observing(viewModel.uiState) {
            ProgressView()
        } content: { state in
            List {
                Section("Power User") {
                    powerUserModeToggle(state: state)
                }
                sharingSection
                if state.powerUserMode {
                    Section("Integrations") {
                        NavigationLink(destination: IntegrationsHubView()) {
                            VStack(alignment: .leading, spacing: Spacing.xs) {
                                Text("Server Integrations")
                                    .font(.civitBodyMedium)
                                Text("ComfyUI, SD WebUI, Civitai Link, Custom Server")
                                    .font(.civitBodySmall)
                                    .foregroundColor(.civitOnSurfaceVariant)
                            }
                        }
                    }
                    Section("Model Files") {
                        NavigationLink {
                            ModelFileBrowserScreen()
                        } label: {
                            Label("Model File Browser", systemImage: "folder.badge.gearshape")
                        }
                    }
                    Section("Navigation") {
                        NavigationLink(
                            destination: NavShortcutsSettingsView(viewModel: displayViewModel)
                        ) {
                            Text("Navigation Shortcuts")
                        }
                    }
                }
                Section("Plugins") {
                    NavigationLink(destination: PluginListView()) {
                        Label("Plugins", systemImage: "puzzlepiece.extension")
                    }
                }
            }
            .navigationTitle("Advanced & Integrations")
            .navigationBarTitleDisplayMode(.inline)
        }
        .task { await shareHashtagVM.startObserving() }
    }

    @Environment(\.civitTheme) private var theme

    private var sharingSection: some View {
        Section {
            Text("Manage default hashtags for sharing generated images.")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
            hashtagChips
            addTagField
        } header: {
            Text("Sharing")
        }
    }

    private var hashtagChips: some View {
        FlowLayout(spacing: Spacing.sm) {
            ForEach(shareHashtagVM.hashtags, id: \.tag) { hashtag in
                Button {
                    HapticFeedback.selection.trigger()
                    shareHashtagVM.toggle(tag: hashtag.tag, isEnabled: !hashtag.isEnabled)
                } label: {
                    HStack(spacing: Spacing.xs) {
                        Text(hashtag.tag)
                            .font(.civitLabelMedium)
                        Button {
                            shareHashtagVM.remove(tag: hashtag.tag)
                        } label: {
                            Image(systemName: "xmark")
                                .font(.civitLabelXSmall)
                                .accessibilityLabel("Remove hashtag")
                        }
                    }
                    .padding(.horizontal, Spacing.md)
                    .padding(.vertical, Spacing.xsPlus)
                    .background(hashtag.isEnabled ? theme.primary.opacity(0.2) : Color.civitSurfaceVariant)
                    .foregroundColor(hashtag.isEnabled ? theme.primary : .civitOnSurface)
                    .clipShape(Capsule())
                }
            }
        }
    }

    @State private var newHashtagInput = ""

    private var addTagField: some View {
        HStack(spacing: Spacing.sm) {
            TextField("Add hashtag", text: $newHashtagInput)
                .textFieldStyle(.roundedBorder)
                .onSubmit { addHashtag() }
            Button { addHashtag() } label: {
                Image(systemName: "plus")
                    .fontWeight(.semibold)
                    .accessibilityLabel("Add hashtag")
            }
            .disabled(newHashtagInput.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }

    private func addHashtag() {
        let trimmed = newHashtagInput.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        shareHashtagVM.add(tag: trimmed)
        newHashtagInput = ""
    }

    private func powerUserModeToggle(state: AppBehaviorSettingsUiState) -> some View {
        Toggle(isOn: Binding(
            get: { state.powerUserMode },
            set: { viewModel.onPowerUserModeChanged(enabled: $0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Power User Mode")
                    .font(.civitBodyMedium)
                Text("Enables ComfyUI, SD WebUI, Civitai Link, model files, and advanced metadata")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }
}

// MARK: - Integrations Hub

private struct IntegrationsHubView: View {
    @StateObject private var viewModel = IntegrationsHubViewModel()

    var body: some View {
        List {
            Section("ComfyUI") {
                NavigationLink(destination: ComfyUISettingsView()) {
                    integrationRow(
                        title: "Server Connections",
                        subtitle: viewModel.comfyUIName ?? "Not configured"
                    )
                }
                NavigationLink(destination: ComfyHubBrowserView()) {
                    integrationRow(
                        title: "ComfyHub Workflows",
                        subtitle: "Browse & import"
                    )
                }
            }
            Section("SD WebUI") {
                NavigationLink(destination: SDWebUISettingsView()) {
                    integrationRow(
                        title: "Connections",
                        subtitle: viewModel.sdWebUIName ?? "Not configured"
                    )
                }
            }
            Section("Civitai Link") {
                NavigationLink(destination: CivitaiLinkSettingsView()) {
                    integrationRow(
                        title: "Setup",
                        subtitle: viewModel.civitaiLinkConnected ? "Connected" : "Not configured"
                    )
                }
            }
            Section("Custom Server") {
                NavigationLink(destination: ExternalServerSettingsView()) {
                    integrationRow(
                        title: "Server Configuration",
                        subtitle: viewModel.externalServerName ?? "Not configured"
                    )
                }
            }
        }
        .navigationTitle("Server Integrations")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observe() }
    }

    private func integrationRow(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title)
                .font(.civitBodyMedium)
            Text(subtitle)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}

@MainActor
private final class IntegrationsHubViewModel: ObservableObject {
    @Published var comfyUIName: String?
    @Published var sdWebUIName: String?
    @Published var civitaiLinkConnected: Bool = false
    @Published var externalServerName: String?

    private let observeComfyUI = KoinHelper.shared.getObserveActiveComfyUIConnectionUseCase()
    private let observeSDWebUI = KoinHelper.shared.getObserveActiveSDWebUIConnectionUseCase()
    private let observeCivitaiLink = KoinHelper.shared.getObserveCivitaiLinkKeyUseCase()
    private let observeExtServer = KoinHelper.shared.getObserveActiveExternalServerConfigUseCase()

    func observe() async {
        async let comfyTask: Void = observeComfyUIConnection()
        async let sdTask: Void = observeSDWebUIConnection()
        async let linkTask: Void = observeCivitaiLinkKey()
        async let extTask: Void = observeExternalServer()
        _ = await (comfyTask, sdTask, linkTask, extTask)
    }

    private func observeComfyUIConnection() async {
        for await conn in observeComfyUI.invoke() {
            self.comfyUIName = conn?.name
        }
    }

    private func observeSDWebUIConnection() async {
        for await conn in observeSDWebUI.invoke() {
            self.sdWebUIName = conn?.name
        }
    }

    private func observeCivitaiLinkKey() async {
        for await key in observeCivitaiLink.invoke() {
            self.civitaiLinkConnected = key != nil
        }
    }

    private func observeExternalServer() async {
        for await config in observeExtServer.invoke() {
            self.externalServerName = config?.name
        }
    }
}
