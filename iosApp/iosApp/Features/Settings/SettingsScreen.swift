import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var vmStore = SettingsViewModelStore()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        if sizeClass == .regular {
            settingsContent
        } else {
            NavigationStack {
                settingsContent
            }
        }
    }

    private var settingsContent: some View {
        Observing(vmStore.storage.uiState, vmStore.appBehavior.uiState, vmStore.auth.uiState) {
            ProgressView()
        } content: { storageState, appBehaviorState, authState in
            List {
                if !storageState.isOnline {
                    offlineBanner
                }
                accountSection(authState: authState)
                appearanceSection
                notificationsSection
                contentBehaviorSection
                historySection
                dataStorageSection
                advancedIntegrationsSection
                if appBehaviorState.powerUserMode {
                    analyticsSection
                    datasetsSection
                }
                aboutSection
                if !appBehaviorState.powerUserMode {
                    Section {
                        Text("Enable Power User Mode in Advanced & Integrations for more features")
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var offlineBanner: some View {
        HStack {
            Spacer()
            Text("You are offline — showing cached data")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnErrorContainer)
            Spacer()
        }
        .listRowBackground(Color.civitErrorContainer)
    }

    private func accountSection(authState: AuthSettingsUiState) -> some View {
        Section("Account") {
            if let username = authState.connectedUsername, authState.apiKey != nil {
                ConnectedAccountRow(username: username, onClear: vmStore.auth.onClearApiKey)
            } else {
                ApiKeyInputRow(
                    isValidating: authState.isValidatingApiKey,
                    error: authState.apiKeyError,
                    onValidateAndSave: { vmStore.auth.onValidateAndSaveApiKey(apiKey: $0) }
                )
            }
        }
    }

    private var appearanceSection: some View {
        Section {
            NavigationLink(destination: AppearanceSettingsView(viewModel: vmStore.display)) {
                Text("Appearance")
            }
        }
    }

    private var notificationsSection: some View {
        Section("Notifications") {
            NavigationLink(destination: NotificationCenterView()) {
                Label("Model Updates", systemImage: "bell")
            }
        }
    }

    private var contentBehaviorSection: some View {
        Section {
            NavigationLink(destination: ContentFilterSettingsView(
                viewModel: vmStore.contentFilter,
                displayViewModel: vmStore.display,
                appBehaviorViewModel: vmStore.appBehavior
            )) {
                Text("Content & Behavior")
            }
        }
    }

    private var historySection: some View {
        Section("History") {
            NavigationLink(destination: BrowsingHistoryView()) {
                Label("Browsing History", systemImage: "clock.arrow.circlepath")
            }
        }
    }

    private var dataStorageSection: some View {
        Section {
            NavigationLink(destination: StorageSettingsView(viewModel: vmStore.storage)) {
                Text("Data & Storage")
            }
        }
    }

    private var advancedIntegrationsSection: some View {
        Section {
            NavigationLink(destination: AdvancedSettingsView(
                viewModel: vmStore.appBehavior,
                displayViewModel: vmStore.display
            )) {
                Text("Advanced & Integrations")
            }
        }
    }

    private var analyticsSection: some View {
        Section("Analytics") {
            NavigationLink(destination: AnalyticsView()) {
                Label("Usage Stats", systemImage: "chart.bar.xaxis")
            }
        }
    }

    private var datasetsSection: some View {
        Section("Training") {
            NavigationLink(destination: DatasetListView()) {
                Label("Datasets", systemImage: "photo.on.rectangle.angled")
            }
        }
    }

    private var aboutSection: some View {
        Section("About") {
            HStack {
                Text("App Version")
                Spacer()
                Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            NavigationLink("Open Source Licenses") {
                LicensesView()
            }
            Button {
                vmStore.replayGestureTutorial()
            } label: {
                Text("Replay Gesture Tutorial")
                    .foregroundColor(.civitOnSurface)
            }
        }
    }
}

private struct ConnectedAccountRow: View {
    let username: String
    let onClear: () -> Void
    @State private var showConfirmation = false

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Connected as")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Text(username)
                    .font(.civitBodyMedium)
            }
            Spacer()
            Button("Disconnect") { showConfirmation = true }
                .foregroundColor(.civitError)
        }
        .alert("Disconnect", isPresented: $showConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Remove", role: .destructive) { onClear() }
        } message: {
            Text("Remove your CivitAI API key?")
        }
    }
}

private struct ApiKeyInputRow: View {
    let isValidating: Bool
    let error: String?
    let onValidateAndSave: (String) -> Void
    @State private var keyInput = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                SecureField("Paste API key", text: $keyInput)
                    .textContentType(.password)
                if isValidating {
                    ProgressView()
                } else {
                    Button("Verify") {
                        onValidateAndSave(keyInput)
                        keyInput = ""
                    }
                    .disabled(keyInput.isEmpty)
                }
            }
            if let error {
                Text(error)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitError)
            }
            Text("Get your key at civitai.com/user/account")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}
