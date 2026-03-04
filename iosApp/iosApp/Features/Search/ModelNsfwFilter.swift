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
                modelVersions: filteredVersions
            )
        }
    }
}
