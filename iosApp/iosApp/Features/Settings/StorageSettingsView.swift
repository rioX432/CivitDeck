import SwiftUI
import Shared

struct StorageSettingsView: View {
    @ObservedObject var viewModel: StorageSettingsViewModelOwner

    var body: some View {
        List {
            Section("Offline Cache") {
                offlineCacheToggle
                if viewModel.offlineCacheEnabled {
                    cacheSizeLimitPicker
                }
                cacheInfoRow
            }
            Section("Backup") {
                NavigationLink(destination: BackupView()) {
                    Label("Backup & Restore", systemImage: "arrow.up.arrow.down.circle")
                }
            }
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
                ClearActionButton(label: "Clear Search History", action: viewModel.onClearSearchHistory)
                ClearActionButton(label: "Clear Browsing History", action: viewModel.onClearBrowsingHistory)
                ClearActionButton(label: "Clear Cache", action: viewModel.onClearCache)
            }
        }
        .navigationTitle("Data & Storage")
        .navigationBarTitleDisplayMode(.inline)
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
