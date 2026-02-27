import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        NavigationStack {
            List {
                if !viewModel.isOnline {
                    offlineBanner
                }
                accountSection
                themeSection
                contentFilterSection
                displaySection
                modelFilesSection
                offlineCacheSection
                dataManagementSection
                aboutSection
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.observeApiKey() }
            .task { await viewModel.observeNsfwFilter() }
            .task { await viewModel.observeNsfwBlurSettings() }
            .task { await viewModel.observeSortOrder() }
            .task { await viewModel.observeTimePeriod() }
            .task { await viewModel.observeGridColumns() }
            .task { await viewModel.observePowerUserMode() }
            .task { await viewModel.observeAccentColor() }
            .task { await viewModel.observeAmoledDarkMode() }
            .task { await viewModel.observeNetworkStatus() }
            .task { await viewModel.observeOfflineCacheEnabled() }
            .task { await viewModel.observeCacheSizeLimit() }
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

    private var themeSection: some View {
        Section("Theme") {
            accentColorPicker
            amoledDarkModeToggle
        }
    }

    private var accentColorPicker: some View {
        Picker("Accent Color", selection: Binding(
            get: { viewModel.accentColor },
            set: { viewModel.onAccentColorChanged($0) }
        )) {
            ForEach(AccentColor.entries, id: \.self) { color in
                Text(color.displayName).tag(color)
            }
        }
    }

    private var amoledDarkModeToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.amoledDarkMode },
            set: { viewModel.onAmoledDarkModeChanged($0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("AMOLED Dark Mode")
                    .font(.civitBodyMedium)
                Text("Pure black background for OLED screens")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var contentFilterSection: some View {
        Section("Content Filter") {
            Toggle(isOn: Binding(
                get: { viewModel.nsfwFilterLevel != .off },
                set: { _ in viewModel.onNsfwFilterToggle() }
            )) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("NSFW Content")
                        .font(.civitBodyMedium)
                    Text("Show NSFW content in search results")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            if viewModel.nsfwFilterLevel != .off {
                NsfwBlurSettingsSection(
                    settings: viewModel.nsfwBlurSettings,
                    onChanged: viewModel.onNsfwBlurSettingsChanged
                )
            }
        }
    }

    private var displaySection: some View {
        Section("Display") {
            sortOrderPicker
            timePeriodPicker
            gridColumnsPicker
            powerUserModeToggle
        }
    }

    private var powerUserModeToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.powerUserMode },
            set: { viewModel.onPowerUserModeChanged($0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Power User Mode")
                    .font(.civitBodyMedium)
                Text("Show advanced metadata on detail screens")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var sortOrderPicker: some View {
        Picker("Default Sort", selection: Binding(
            get: { viewModel.defaultSortOrder },
            set: { viewModel.onSortOrderChanged($0) }
        )) {
            ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                Text(SearchFilter.sortLabel(sort)).tag(sort)
            }
        }
    }

    private var timePeriodPicker: some View {
        Picker("Default Period", selection: Binding(
            get: { viewModel.defaultTimePeriod },
            set: { viewModel.onTimePeriodChanged($0) }
        )) {
            ForEach(SearchFilter.periodOptions, id: \.self) { period in
                Text(SearchFilter.periodLabel(period)).tag(period)
            }
        }
    }

    private var gridColumnsPicker: some View {
        Picker("Grid Columns", selection: Binding(
            get: { viewModel.gridColumns },
            set: { viewModel.onGridColumnsChanged($0) }
        )) {
            Text("2").tag(Int32(2))
            Text("3").tag(Int32(3))
        }
        .pickerStyle(.segmented)
    }

    @ViewBuilder
    private var modelFilesSection: some View {
        if viewModel.powerUserMode {
            Section("Model Files") {
                NavigationLink {
                    ModelFileBrowserScreen()
                } label: {
                    Label("Model File Browser", systemImage: "folder.badge.gearshape")
                }
            }
        }
    }

    private var offlineCacheSection: some View {
        Section("Offline & Cache") {
            offlineCacheToggle
            if viewModel.offlineCacheEnabled {
                cacheSizeLimitPicker
            }
            cacheInfoRow
        }
    }

    private var offlineCacheToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.offlineCacheEnabled },
            set: { viewModel.onOfflineCacheEnabledChanged($0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Offline Cache")
                    .font(.civitBodyMedium)
                Text("Keep viewed models available offline")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var cacheSizeLimitPicker: some View {
        Picker("Cache Size Limit", selection: Binding(
            get: { viewModel.cacheSizeLimitMb },
            set: { viewModel.onCacheSizeLimitChanged($0) }
        )) {
            Text("50 MB").tag(Int32(50))
            Text("100 MB").tag(Int32(100))
            Text("200 MB").tag(Int32(200))
            Text("500 MB").tag(Int32(500))
        }
    }

    private var cacheInfoRow: some View {
        HStack {
            Text("Cached Entries")
            Spacer()
            Text("\(viewModel.cacheEntryCount) entries (\(viewModel.cacheFormattedSize))")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    private var dataManagementSection: some View {
        Section("Data Management") {
            NavigationLink {
                HiddenModelsView(
                    models: viewModel.hiddenModels,
                    onUnhide: viewModel.onUnhideModel
                )
            } label: {
                HStack {
                    Text("Hidden Models")
                    Spacer()
                    Text("\(viewModel.hiddenModels.count) models")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            NavigationLink {
                ExcludedTagsView(
                    tags: viewModel.excludedTags,
                    onAdd: viewModel.onAddExcludedTag,
                    onRemove: viewModel.onRemoveExcludedTag
                )
            } label: {
                HStack {
                    Text("Excluded Tags")
                    Spacer()
                    Text("\(viewModel.excludedTags.count) tags")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            ClearActionButton(label: "Clear Search History", action: viewModel.onClearSearchHistory)
            ClearActionButton(label: "Clear Browsing History", action: viewModel.onClearBrowsingHistory)
            ClearActionButton(label: "Clear Cache", action: viewModel.onClearCache)
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

private struct NsfwBlurSettingsSection: View {
    let settings: NsfwBlurSettings
    let onChanged: (NsfwBlurSettings) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("Blur Intensity")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
            BlurSliderRow(label: "Soft", intensity: Int(settings.softIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: Int32(value),
                    matureIntensity: settings.matureIntensity,
                    explicitIntensity: settings.explicitIntensity
                ))
            }
            BlurSliderRow(label: "Mature", intensity: Int(settings.matureIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: settings.softIntensity,
                    matureIntensity: Int32(value),
                    explicitIntensity: settings.explicitIntensity
                ))
            }
            BlurSliderRow(label: "Explicit", intensity: Int(settings.explicitIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: settings.softIntensity,
                    matureIntensity: settings.matureIntensity,
                    explicitIntensity: Int32(value)
                ))
            }
        }
    }
}

private struct BlurSliderRow: View {
    let label: String
    let intensity: Int
    let onChanged: (Int) -> Void

    var body: some View {
        VStack(spacing: 2) {
            HStack {
                Text(label)
                    .font(.civitBodyMedium)
                Spacer()
                Text(intensity == 0 ? "Hidden" : "\(intensity)%")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Slider(
                value: Binding(
                    get: { Double(intensity) },
                    set: { onChanged(Int($0)) }
                ),
                in: 0...100,
                step: 25
            )
        }
    }
}

private struct ClearActionButton: View {
    let label: String
    let action: () -> Void
    @State private var showConfirmation = false

    var body: some View {
        Button(label) {
            showConfirmation = true
        }
        .foregroundColor(.civitError)
        .alert(label, isPresented: $showConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Clear", role: .destructive) { action() }
        } message: {
            Text("Are you sure? This cannot be undone.")
        }
    }
}
