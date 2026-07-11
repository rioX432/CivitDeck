import SwiftUI
import Shared

// MARK: - NSFW Image Filtering

extension ModelImage {
    func isAllowedByFilter(_ level: NsfwFilterLevel) -> Bool {
        switch level {
        case .off: return nsfwLevel == .none
        case .soft: return nsfwLevel == .none || nsfwLevel == .soft
        default: return true
        }
    }
}

// MARK: - NSFW Level Picker

/// Three-level content control (Safe / Moderate / Everything), mirroring the
/// Android chip row. The persisted preference is the single source of truth —
/// Settings and the search filter both write through the same use case.
struct NsfwLevelPicker: View {
    let level: NsfwFilterLevel
    let onChanged: (NsfwFilterLevel) -> Void

    private static let options: [NsfwFilterLevel] = [.off, .soft, .all]

    static func label(_ level: NsfwFilterLevel) -> String {
        switch level {
        case .off: return "Safe"
        case .soft: return "Moderate"
        default: return "Everything"
        }
    }

    var body: some View {
        Picker("NSFW level", selection: Binding(
            get: { level },
            set: { onChanged($0) }
        )) {
            ForEach(Self.options, id: \.self) { option in
                Text(Self.label(option)).tag(option)
            }
        }
        .pickerStyle(.segmented)
    }
}

// MARK: - Search Filter Sheet NSFW Section

extension ModelSearchScreen {
    var nsfwLevelSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("NSFW level")
                .font(.civitTitleSmall)
                .foregroundColor(.civitOnSurfaceVariant)
            NsfwLevelPicker(
                level: viewModel.nsfwFilterLevel,
                onChanged: { viewModel.onNsfwFilterLevelSelected($0) }
            )
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.sm)
    }
}

extension Array where Element == Model {
    func filterNsfwImages(_ level: NsfwFilterLevel) -> [Model] {
        if level == .all { return self }
        return compactMap { model in
            let filteredVersions = model.modelVersions.map { version in
                let safeImages = version.images.filter { $0.isAllowedByFilter(level) }
                return ModelVersion(
                    id: version.id,
                    modelId: version.modelId,
                    name: version.name,
                    description: version.description_,
                    createdAt: version.createdAt,
                    baseModel: version.baseModel,
                    trainedWords: version.trainedWords,
                    downloadUrl: version.downloadUrl,
                    files: version.files,
                    images: safeImages,
                    stats: version.stats
                )
            }
            let hasAnyImages = filteredVersions.contains { !$0.images.isEmpty }
            guard hasAnyImages else { return nil }
            return Model(
                id: model.id,
                name: model.name,
                description: model.description_,
                type: model.type,
                nsfw: model.nsfw,
                tags: model.tags,
                mode: model.mode,
                creator: model.creator,
                stats: model.stats,
                modelVersions: filteredVersions,
                source: model.source
            )
        }
    }
}
