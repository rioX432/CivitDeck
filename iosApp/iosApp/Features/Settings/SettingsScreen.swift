import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModelOwner()

    var body: some View {
        NavigationStack {
            List {
                if !viewModel.isOnline {
                    offlineBanner
                }
                accountSection
                appearanceSection
                contentFilterSection
                notificationsSection
                storageSection
                advancedSection
                aboutSection
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.observeUiState() }
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

    private var accountSection: some View {
        Section("Account") {
            if let username = viewModel.connectedUsername, viewModel.apiKey != nil {
                ConnectedAccountRow(username: username, onClear: viewModel.onClearApiKey)
            } else {
                ApiKeyInputRow(
                    isValidating: viewModel.isValidatingApiKey,
                    error: viewModel.apiKeyError,
                    onValidateAndSave: viewModel.onValidateAndSaveApiKey
                )
            }
        }
    }

    private var appearanceSection: some View {
        Section {
            NavigationLink(destination: AppearanceSettingsView(viewModel: viewModel)) {
                Text("Appearance")
            }
        }
    }

    private var contentFilterSection: some View {
        Section {
            NavigationLink(destination: ContentFilterSettingsView(viewModel: viewModel)) {
                Text("Content & Filters")
            }
        }
    }

    private var notificationsSection: some View {
        Section {
            NavigationLink(destination: NotificationsSettingsView(viewModel: viewModel)) {
                Text("Notifications")
            }
        }
    }

    private var storageSection: some View {
        Section {
            NavigationLink(destination: StorageSettingsView(viewModel: viewModel)) {
                Text("Storage")
            }
        }
    }

    private var advancedSection: some View {
        Section {
            NavigationLink(destination: AdvancedSettingsView(viewModel: viewModel)) {
                Text("Advanced")
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
            VStack(alignment: .leading, spacing: 4) {
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
        VStack(alignment: .leading, spacing: 8) {
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
