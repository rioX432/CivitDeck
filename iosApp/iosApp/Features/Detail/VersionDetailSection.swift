import SwiftUI
import Shared

struct VersionDetailSection: View {
    let version: ModelVersion
    let powerUserMode: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Divider()
                .padding(.horizontal, Spacing.lg)

            baseModelRow
            trainedWordsSection
            filesSection

            if powerUserMode {
                advancedVersionInfo
            }
        }
        .padding(.horizontal, Spacing.lg)
    }

    // MARK: - Base Model

    @ViewBuilder
    private var baseModelRow: some View {
        if let baseModel = version.baseModel, !baseModel.isEmpty {
            detailRow(label: "Base Model", value: baseModel)
        }
    }

    // MARK: - Trained Words

    @ViewBuilder
    private var trainedWordsSection: some View {
        if !version.trainedWords.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Trained Words")
                    .font(.civitTitleSmall)
                Text(version.trainedWords.joined(separator: ", "))
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    // MARK: - Files

    @ViewBuilder
    private var filesSection: some View {
        if !version.files.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Files")
                    .font(.civitTitleSmall)
                ForEach(version.files, id: \.id) { file in
                    fileInfoRow(file: file)
                    if powerUserMode {
                        advancedFileInfo(file: file)
                    }
                }
            }
        }
    }

    // MARK: - File Info Row

    private func fileInfoRow(file: ModelFile) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(file.name)
                .font(.civitBodySmall)
                .lineLimit(1)
                .truncationMode(.middle)
            HStack(spacing: Spacing.sm) {
                Text(FormatUtils.shared.formatFileSize(sizeKB: file.sizeKB))
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                if let format = file.format {
                    Text(format)
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                if let fp = file.fp {
                    Text(fp)
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .padding(.vertical, Spacing.xs)
    }

    // MARK: - Advanced File Info

    private func advancedFileInfo(file: ModelFile) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            let hashes = file.hashes as? [String: String] ?? [:]
            ForEach(Array(hashes.keys.sorted()), id: \.self) { key in
                if let value = hashes[key] {
                    detailRow(label: key, value: value)
                }
            }
            if let scan = file.pickleScanResult {
                detailRow(label: "Pickle Scan", value: scan)
            }
            if let scan = file.virusScanResult {
                detailRow(label: "Virus Scan", value: scan)
            }
            if let scannedAt = file.scannedAt {
                detailRow(label: "Scanned At", value: scannedAt)
            }
        }
        .padding(.leading, Spacing.sm)
    }

    // MARK: - Advanced Version Info

    @State private var isAdvancedExpanded = false

    private var advancedVersionInfo: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Divider()
            Button {
                withAnimation(MotionAnimation.standard) {
                    isAdvancedExpanded.toggle()
                }
            } label: {
                HStack {
                    Text("Advanced Info")
                        .font(.civitTitleSmall)
                        .foregroundColor(.civitOnSurface)
                    Spacer()
                    Text(isAdvancedExpanded ? "Hide" : "Show")
                        .font(.civitLabelMedium)
                        .foregroundColor(.civitPrimary)
                }
            }

            if isAdvancedExpanded {
                VStack(alignment: .leading, spacing: 2) {
                    if !version.createdAt.isEmpty {
                        detailRow(label: "Created", value: version.createdAt)
                    }
                    if let stats = version.stats {
                        detailRow(
                            label: "Downloads",
                            value: "\(stats.downloadCount)"
                        )
                        detailRow(
                            label: "Rating",
                            value: "\(FormatUtils.shared.formatRating(rating: stats.rating))"
                                + " (\(stats.ratingCount))"
                        )
                    }
                    if let desc = version.description_, !desc.isEmpty {
                        Text("Version Notes")
                            .font(.civitLabelMedium)
                            .foregroundColor(.civitPrimary)
                            .padding(.top, Spacing.xs)
                        Text(htmlToPlainText(desc))
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
                .transition(.opacity)
            }
        }
    }

    // MARK: - Detail Row

    private func detailRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Spacer()
            Text(value)
                .font(.civitBodyMedium)
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }
}
