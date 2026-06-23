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
                        Text("settings_power_user_hint")
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                            .lineLimit(3)
                    }
                }
            }
            .navigationTitle("settings_title")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var offlineBanner: some View {
        HStack {
            Spacer()
            Text("settings_offline_banner")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnErrorContainer)
            Spacer()
        }
        .listRowBackground(Color.civitErrorContainer)
    }

    private func accountSection(authState: AuthSettingsUiState) -> some View {
        Section("settings_section_account") {
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
                Text("settings_appearance")
            }
        }
    }

    private var notificationsSection: some View {
        Section("settings_section_notifications") {
            NavigationLink(destination: NotificationCenterView()) {
                Label("settings_model_updates", systemImage: "bell")
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
                Text("settings_content_behavior")
            }
        }
    }

    private var historySection: some View {
        Section("settings_section_history") {
            NavigationLink(destination: BrowsingHistoryView()) {
                Label("settings_browsing_history", systemImage: "clock.arrow.circlepath")
            }
        }
    }

    private var dataStorageSection: some View {
        Section {
            NavigationLink(destination: StorageSettingsView(viewModel: vmStore.storage)) {
                Text("settings_data_storage")
            }
        }
    }

    private var advancedIntegrationsSection: some View {
        Section {
            NavigationLink(destination: AdvancedSettingsView(
                viewModel: vmStore.appBehavior,
                displayViewModel: vmStore.display
            )) {
                Text("settings_advanced_integrations")
            }
        }
    }

    private var analyticsSection: some View {
        Section("settings_section_analytics") {
            NavigationLink(destination: AnalyticsView()) {
                Label("settings_usage_stats", systemImage: "chart.bar.xaxis")
            }
        }
    }

    private var datasetsSection: some View {
        Section("settings_section_training") {
            NavigationLink(destination: DatasetListView()) {
                Label("settings_datasets", systemImage: "photo.on.rectangle.angled")
            }
        }
    }

    private var aboutSection: some View {
        Section("settings_section_about") {
            HStack {
                Text("settings_app_version")
                Spacer()
                Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            NavigationLink("settings_open_source_licenses") {
                LicensesView()
            }
            Button {
                vmStore.replayGestureTutorial()
            } label: {
                Text("settings_replay_gesture_tutorial")
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
                Text("settings_connected_as")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Text(username)
                    .font(.civitBodyMedium)
            }
            Spacer()
            Button("settings_disconnect") { showConfirmation = true }
                .foregroundColor(.civitError)
        }
        .alert("settings_disconnect", isPresented: $showConfirmation) {
            Button("action_cancel", role: .cancel) {}
            Button("action_remove", role: .destructive) { onClear() }
        } message: {
            Text("settings_disconnect_confirm_message")
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
                SecureField("settings_api_key_placeholder", text: $keyInput)
                    .textContentType(.password)
                if isValidating {
                    ProgressView()
                } else {
                    Button("settings_api_key_verify") {
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
            Text("settings_api_key_help")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}
