import SwiftUI
import Shared

struct StorageSettingsView: View {
    let viewModel: StorageSettingsViewModel

    var body: some View {
        Observing(viewModel.uiState) {
            ProgressView()
        } content: { state in
            List {
                Section("Offline Cache") {
                    offlineCacheToggle(state: state)
                    if state.offlineCacheEnabled {
                        cacheSizeLimitPicker(state: state)
                    }
                    cacheInfoRow(state: state)
                }
                Section("Backup") {
                    NavigationLink(destination: BackupView()) {
                        Label("Backup & Restore", systemImage: "arrow.up.arrow.down.circle")
                    }
                }
                Section("Data Management") {
                    NavigationLink {
                        HiddenModelsView(
                            models: state.hiddenModels as? [Core_domainHiddenModel] ?? [],
                            onUnhide: { viewModel.onUnhideModel(modelId: $0) }
                        )
                    } label: {
                        HStack {
                            Text("Hidden Models")
                            Spacer()
                            Text("\((state.hiddenModels as? [Core_domainHiddenModel] ?? []).count) models")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                    ClearActionButton(label: "Clear Search History", action: viewModel.onClearSearchHistory)
                    ClearActionButton(label: "Clear Browsing History", action: viewModel.onClearBrowsingHistory)
                    ClearActionButton(label: "Clear Cache", action: viewModel.onClearCache)
                }
            }
            .navigationTitle("Data & Storage")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func offlineCacheToggle(state: StorageSettingsUiState) -> some View {
        Toggle(isOn: Binding(
            get: { state.offlineCacheEnabled },
            set: { viewModel.onOfflineCacheEnabledChanged(enabled: $0) }
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

    private func cacheSizeLimitPicker(state: StorageSettingsUiState) -> some View {
        Picker("Cache Size Limit", selection: Binding(
            get: { state.cacheSizeLimitMb },
            set: { viewModel.onCacheSizeLimitChanged(limitMb: $0) }
        )) {
            Text("50 MB").tag(Int32(50))
            Text("100 MB").tag(Int32(100))
            Text("200 MB").tag(Int32(200))
            Text("500 MB").tag(Int32(500))
        }
    }

    private func cacheInfoRow(state: StorageSettingsUiState) -> some View {
        HStack {
            Text("Cached Entries")
            Spacer()
            Text("\(state.cacheInfo.entryCount) entries (\(state.cacheInfo.formattedSize))")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
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
