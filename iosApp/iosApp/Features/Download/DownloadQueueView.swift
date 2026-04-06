import SwiftUI
import Shared

struct DownloadQueueView: View {
    @StateObject private var viewModel = DownloadQueueViewModelOwner()

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
            } else if isEmpty {
                emptyState
            } else {
                downloadList
            }
        }
        .navigationTitle("Downloads")
        .task { await viewModel.observeUiState() }
    }

    private var isEmpty: Bool {
        viewModel.activeDownloads.isEmpty &&
        viewModel.completedDownloads.isEmpty &&
        viewModel.failedDownloads.isEmpty
    }

    private var emptyState: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "arrow.down.circle")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text("No downloads yet")
                .font(.headline)
            Text("Downloads from model pages will appear here")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var downloadList: some View {
        List {
            activeSection
            failedSection
            completedSection
            storageFooter
        }
    }

    // MARK: - Active Section

    @ViewBuilder
    private var activeSection: some View {
        if !viewModel.activeDownloads.isEmpty {
            Section("Active") {
                ForEach(viewModel.activeDownloads, id: \.id) { download in
                    ActiveDownloadRow(
                        download: download,
                        onPause: { viewModel.pauseDownload(download.id) },
                        onResume: { viewModel.resumeDownload(download.id) },
                        onCancel: { viewModel.cancelDownload(download.id) }
                    )
                }
            }
        }
    }

    // MARK: - Failed Section

    @ViewBuilder
    private var failedSection: some View {
        if !viewModel.failedDownloads.isEmpty {
            Section("Failed") {
                ForEach(viewModel.failedDownloads, id: \.id) { download in
                    FailedDownloadRow(
                        download: download,
                        onRetry: { viewModel.retryDownload(download.id) },
                        onDelete: { viewModel.deleteDownload(download.id) }
                    )
                }
            }
        }
    }

    // MARK: - Completed Section

    @ViewBuilder
    private var completedSection: some View {
        if !viewModel.completedDownloads.isEmpty {
            Section {
                ForEach(viewModel.completedDownloads, id: \.id) { download in
                    CompletedDownloadRow(
                        download: download,
                        onDelete: { viewModel.deleteDownload(download.id) }
                    )
                }
            } header: {
                HStack {
                    Text("Completed")
                    Spacer()
                    Button("Clear All") {
                        viewModel.clearCompleted()
                    }
                    .font(.caption)
                }
            }
        }
    }

    // MARK: - Storage Footer

    @ViewBuilder
    private var storageFooter: some View {
        if viewModel.totalStorageBytes > 0 {
            Section {
                Text("Storage used: \(formatBytes(viewModel.totalStorageBytes))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }

    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useAll]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

// MARK: - Row Components

private struct ActiveDownloadRow: View {
    let download: ModelDownload
    let onPause: () -> Void
    let onResume: () -> Void
    let onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(download.modelName)
                        .font(.body)
                        .lineLimit(1)
                    Text(download.fileName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
                Spacer()
                actionButtons
            }
            progressBar
        }
        .padding(.vertical, Spacing.xs)
    }

    @ViewBuilder
    private var actionButtons: some View {
        HStack(spacing: Spacing.xs) {
            if download.status == .downloading {
                Button { onPause() } label: {
                    Image(systemName: "pause.fill")
                        .foregroundColor(.civitPrimary)
                }
                .buttonStyle(.plain)
            } else if download.status == .paused {
                Button { onResume() } label: {
                    Image(systemName: "play.fill")
                        .foregroundColor(.civitPrimary)
                }
                .buttonStyle(.plain)
            } else {
                ProgressView()
                    .controlSize(.small)
            }
            Button { onCancel() } label: {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
    }

    private var progressBar: some View {
        let progress: Double = download.fileSizeBytes > 0
            ? Double(download.downloadedBytes) / Double(download.fileSizeBytes)
            : 0

        return HStack(spacing: Spacing.sm) {
            if download.status == .paused {
                Text("Paused")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            ProgressView(value: min(max(progress, 0), 1))
            Text("\(Int(progress * 100))%")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

private struct FailedDownloadRow: View {
    let download: ModelDownload
    let onRetry: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(download.modelName)
                    .font(.body)
                    .lineLimit(1)
                Text(download.errorMessage ?? download.status.name)
                    .font(.caption)
                    .foregroundColor(.red)
                    .lineLimit(1)
            }
            Spacer()
            Button { onRetry() } label: {
                Image(systemName: "arrow.clockwise")
                    .foregroundColor(.civitPrimary)
            }
            .buttonStyle(.plain)
            Button { onDelete() } label: {
                Image(systemName: "trash")
                    .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, Spacing.xs)
    }
}

private struct CompletedDownloadRow: View {
    let download: ModelDownload
    let onDelete: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(download.modelName)
                    .font(.body)
                    .lineLimit(1)
                HStack(spacing: Spacing.sm) {
                    Text(formatBytes(download.fileSizeBytes))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    hashBadge
                }
            }
            Spacer()
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(.civitPrimary)
            Button { onDelete() } label: {
                Image(systemName: "trash")
                    .foregroundColor(.red)
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, Spacing.xs)
    }

    @ViewBuilder
    private var hashBadge: some View {
        if let verified = download.hashVerified {
            if verified.boolValue {
                Text("Hash verified")
                    .font(.caption2)
                    .foregroundColor(.civitPrimary)
            } else {
                Text("Hash mismatch")
                    .font(.caption2)
                    .foregroundColor(.red)
            }
        }
    }

    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = [.useAll]
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}
