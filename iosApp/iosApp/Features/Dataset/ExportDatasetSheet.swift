import SwiftUI
import Shared

struct ExportDatasetSheet: View {
    let imageCount: Int
    let nonTrainableCount: Int
    let availableFormats: [PluginExportFormat]
    @Binding var selectedFormatId: String?
    let onExport: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showLicenseWarning = false

    private var effectiveFormatId: String? {
        selectedFormatId ?? availableFormats.first?.id
    }

    var body: some View {
        NavigationView {
            VStack(alignment: .leading, spacing: Spacing.md) {
                exportFormatSelector
                Text("\(imageCount) trainable images will be exported")
                    .font(.civitBodyMedium)
                if nonTrainableCount > 0 {
                    Text("\(nonTrainableCount) images excluded (non-trainable or flagged)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitError)
                }
                Spacer()
                HStack {
                    Button("Cancel") { dismiss() }
                        .buttonStyle(.bordered)
                    Spacer()
                    Button("Export") {
                        if nonTrainableCount > 0 {
                            showLicenseWarning = true
                        } else {
                            onExport()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(effectiveFormatId == nil)
                }
            }
            .padding(Spacing.lg)
            .navigationTitle("Export Dataset")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium])
        .alert("License Warning", isPresented: $showLicenseWarning) {
            Button("Cancel", role: .cancel) {}
            Button("Export Anyway", role: .destructive) { onExport() }
        } message: {
            Text("\(nonTrainableCount) images are non-trainable or have license restrictions. They will be excluded from the export.")
        }
    }

    @ViewBuilder
    private var exportFormatSelector: some View {
        if availableFormats.count <= 1 {
            let format = availableFormats.first
            Text("Format: \(format?.name ?? "ZIP (kohya-ss compatible)")")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        } else {
            Text("Format")
                .font(.civitLabelMedium)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    ForEach(availableFormats, id: \.id) { format in
                        formatChip(format: format, isSelected: format.id == effectiveFormatId)
                    }
                }
            }
            if let selected = availableFormats.first(where: { $0.id == effectiveFormatId }) {
                Text(selected.description_)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    @ViewBuilder
    private func formatChip(format: PluginExportFormat, isSelected: Bool) -> some View {
        if isSelected {
            Button {
                selectedFormatId = format.id
            } label: {
                Text(format.name)
                    .font(.civitBodySmall)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
            }
            .buttonStyle(.borderedProminent)
        } else {
            Button {
                selectedFormatId = format.id
            } label: {
                Text(format.name)
                    .font(.civitBodySmall)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
            }
            .buttonStyle(.bordered)
        }
    }
}

struct ExportProgressOverlay: View {
    let progress: ExportProgress
    let onDismiss: () -> Void

    var body: some View {
        Group {
            if progress is ExportProgressPreparing {
                progressAlert(title: "Preparing export...", value: nil, label: nil)
            } else if let downloading = progress as? ExportProgressDownloading {
                progressAlert(
                    title: "Downloading images...",
                    value: Float(downloading.current) / Float(downloading.total),
                    label: "\(downloading.current) / \(downloading.total)"
                )
            } else if progress is ExportProgressWritingManifest {
                progressAlert(title: "Writing manifest...", value: nil, label: nil)
            } else if let completed = progress as? ExportProgressCompleted {
                completedView(path: completed.outputPath, warnings: Int(completed.warningCount))
            } else if let failed = progress as? ExportProgressFailed {
                failedView(message: failed.message)
            }
        }
    }

    private func progressAlert(title: String, value: Float?, label: String?) -> some View {
        VStack(spacing: Spacing.md) {
            Spacer()
            VStack(spacing: Spacing.sm) {
                Text(title).font(.civitTitleMedium)
                if let value {
                    ProgressView(value: value)
                } else {
                    ProgressView()
                }
                if let label {
                    Text(label).font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                }
            }
            .padding(Spacing.xl)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(Spacing.xl)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.3))
    }

    private func completedView(path: String, warnings: Int) -> some View {
        let fileName = (path as NSString).lastPathComponent
        return VStack(spacing: Spacing.md) {
            Spacer()
            VStack(spacing: Spacing.md) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.civitIconExtraLarge)
                    .foregroundColor(.civitPrimary)
                Text("Export Complete").font(.civitTitleMedium)
                Text(fileName).font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                if warnings > 0 {
                    Text("\(warnings) images were excluded")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                HStack(spacing: Spacing.md) {
                    Button("Close") { onDismiss() }.buttonStyle(.bordered)
                    ShareLink(item: URL(fileURLWithPath: path)) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding(Spacing.xl)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(Spacing.xl)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.3))
    }

    private func failedView(message: String) -> some View {
        VStack(spacing: Spacing.md) {
            Spacer()
            VStack(spacing: Spacing.md) {
                Image(systemName: "xmark.circle.fill")
                    .font(.civitIconExtraLarge)
                    .foregroundColor(.civitError)
                Text("Export Failed").font(.civitTitleMedium)
                Text(message).font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                Button("OK") { onDismiss() }.buttonStyle(.borderedProminent)
            }
            .padding(Spacing.xl)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(Spacing.xl)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.black.opacity(0.3))
    }
}
