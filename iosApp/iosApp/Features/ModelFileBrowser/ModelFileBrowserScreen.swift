import SwiftUI
import Shared

struct ModelFileBrowserScreen: View {
    @StateObject private var viewModel = ModelFileBrowserViewModel()
    @State private var showAddDialog = false
    @State private var newDirectoryPath = ""

    var body: some View {
        List {
            directoriesSection
            if !viewModel.files.isEmpty {
                filesSection
            }
        }
        .navigationTitle("Model Files")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .task { await viewModel.observeDirectories() }
        .task { await viewModel.observeFiles() }
        .overlay { emptyOverlay }
        .alert("Add Model Directory", isPresented: $showAddDialog) {
            addDirectoryAlert
        }
        .alert("Scan Error", isPresented: .init(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.onDismissError() } }
        )) {
            Button("OK") { viewModel.onDismissError() }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    // MARK: - Sections

    private var directoriesSection: some View {
        Section("Directories") {
            ForEach(viewModel.directories, id: \.id) { directory in
                DirectoryRow(directory: directory) {
                    viewModel.onRemoveDirectory(directory.id)
                }
            }
            Button {
                showAddDialog = true
            } label: {
                Label("Add Directory", systemImage: "plus")
            }
        }
    }

    private var filesSection: some View {
        Section("Scanned Files (\(viewModel.files.count))") {
            if viewModel.scanStatus == .scanning || viewModel.scanStatus == .verifying {
                HStack(spacing: Spacing.sm) {
                    ProgressView()
                    Text(viewModel.scanProgress)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            ForEach(viewModel.files, id: \.id) { file in
                ModelFileRow(file: file)
            }
        }
    }

    // MARK: - Toolbar

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            if viewModel.scanStatus == .scanning || viewModel.scanStatus == .verifying {
                ProgressView()
            } else if !viewModel.directories.isEmpty {
                Button {
                    viewModel.onScanAll()
                } label: {
                    Image(systemName: "arrow.clockwise")
                }
            }
        }
    }

    // MARK: - Empty State

    @ViewBuilder
    private var emptyOverlay: some View {
        if viewModel.directories.isEmpty {
            VStack(spacing: Spacing.sm) {
                Text("No model directories configured")
                    .font(.civitTitleMedium)
                Text("Tap + to add a directory path")
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    // MARK: - Add Dialog

    @AlertActionsBuilder
    private var addDirectoryAlert: some View {
        TextField("/path/to/models", text: $newDirectoryPath)
        Button("Add") {
            viewModel.onAddDirectory(newDirectoryPath)
            newDirectoryPath = ""
        }
        Button("Cancel", role: .cancel) {
            newDirectoryPath = ""
        }
    }
}

// MARK: - Subviews

private struct DirectoryRow: View {
    let directory: ModelDirectory
    let onRemove: () -> Void
    @State private var showConfirm = false

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(directory.label ?? directory.path)
                .font(.civitBodyMedium)
                .lineLimit(1)
            Text(directory.path)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .lineLimit(1)
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                showConfirm = true
            } label: {
                Label("Remove", systemImage: "trash")
            }
        }
        .alert("Remove Directory", isPresented: $showConfirm) {
            Button("Remove", role: .destructive) { onRemove() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Remove this directory and all its scanned data?")
        }
    }
}

private struct ModelFileRow: View {
    let file: LocalModelFile

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(file.fileName)
                    .font(.civitBodyMedium)
                    .lineLimit(1)
                Text(formatFileSize(file.sizeBytes))
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                if let match = file.matchedModel {
                    Text("\(match.modelName) - \(match.versionName)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitPrimary)
                        .lineLimit(1)
                    if match.hasUpdate {
                        Label("Update available", systemImage: "exclamationmark.triangle")
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitTertiary)
                    }
                }
            }
            Spacer()
            if file.matchedModel != nil {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.civitPrimary)
            }
        }
    }

    private func formatFileSize(_ bytes: Int64) -> String {
        let kb = Double(bytes) / 1024.0
        let mb = kb / 1024.0
        let gb = mb / 1024.0
        if gb >= 1.0 {
            return String(format: "%.1f GB", gb)
        } else if mb >= 1.0 {
            return String(format: "%.1f MB", mb)
        } else {
            return String(format: "%.0f KB", kb)
        }
    }
}
