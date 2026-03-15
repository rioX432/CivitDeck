import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var authViewModel = AuthSettingsViewModelOwner()
    @StateObject private var appBehaviorViewModel = AppBehaviorSettingsViewModelOwner()
    @StateObject private var storageViewModel = StorageSettingsViewModelOwner()
    @StateObject private var displayViewModel = DisplaySettingsViewModelOwner()
    @StateObject private var contentFilterViewModel = ContentFilterSettingsViewModelOwner()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        // On iPad (regular size class), NavigationSplitView already provides navigation context.
        // Adding NavigationStack here causes double back buttons in sub-screens.
        if sizeClass == .regular {
            settingsContent
        } else {
            NavigationStack {
                settingsContent
            }
        }
    }

    private var settingsContent: some View {
        List {
            if !storageViewModel.isOnline {
                offlineBanner
            }
            accountSection
            appearanceSection
            contentBehaviorSection
            dataStorageSection
            advancedIntegrationsSection
            if appBehaviorViewModel.powerUserMode {
                analyticsSection
                datasetsSection
            }
            aboutSection
            if !appBehaviorViewModel.powerUserMode {
                Section {
                    Text("Enable Power User Mode in Advanced & Integrations for more features")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .task { await authViewModel.observeUiState() }
        .task { await appBehaviorViewModel.observeUiState() }
        .task { await storageViewModel.observeUiState() }
        .task { await displayViewModel.observeUiState() }
        .task { await contentFilterViewModel.observeUiState() }
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

    private var accountSection: some View {
        Section("Account") {
            if let username = authViewModel.connectedUsername, authViewModel.apiKey != nil {
                ConnectedAccountRow(username: username, onClear: authViewModel.onClearApiKey)
            } else {
                ApiKeyInputRow(
                    isValidating: authViewModel.isValidatingApiKey,
                    error: authViewModel.apiKeyError,
                    onValidateAndSave: authViewModel.onValidateAndSaveApiKey
                )
            }
        }
    }

    private var appearanceSection: some View {
        Section {
            NavigationLink(destination: AppearanceSettingsView(viewModel: displayViewModel)) {
                Text("Appearance")
            }
        }
    }

    private var contentBehaviorSection: some View {
        Section {
            NavigationLink(destination: ContentFilterSettingsView(
                viewModel: contentFilterViewModel,
                displayViewModel: displayViewModel,
                appBehaviorViewModel: appBehaviorViewModel
            )) {
                Text("Content & Behavior")
            }
        }
    }

    private var dataStorageSection: some View {
        Section {
            NavigationLink(destination: StorageSettingsView(viewModel: storageViewModel)) {
                Text("Data & Storage")
            }
        }
    }

    private var advancedIntegrationsSection: some View {
        Section {
            NavigationLink(destination: AdvancedSettingsView(
                viewModel: appBehaviorViewModel,
                displayViewModel: displayViewModel
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
