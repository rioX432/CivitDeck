import Foundation
import Shared

// MARK: - Favorites, Hidden Models & Saved Filters

extension ModelSearchViewModel {
    func toggleFavorite(_ model: Model) {
        Task { try? await toggleFavoriteUseCase.invoke(model: model) }
    }
    func observeFavorites() async {
        for await list in observeFavoritesUseCase.invoke() {
            let summaries = list.compactMap { $0 as? FavoriteModelSummary }
            favoriteIds = Set(summaries.map { $0.id })
        }
    }
    func hideModel(_ modelId: Int64, name: String) {
        Task {
            try? await hideModelUseCase.invoke(modelId: modelId, modelName: name)
            hiddenModelIds = try await getHiddenModelIdsUseCase.invoke()
            models.removeAll { $0.id == modelId }
        }
    }
    func isModelOwned(_ model: Model) -> Bool {
        guard !ownedHashes.isEmpty else { return false }
        return model.modelVersions.contains { version in
            version.files.contains { file in
                if let sha256 = file.hashes["SHA256"] as? String {
                    return ownedHashes.contains(sha256.lowercased())
                }
                return false
            }
        }
    }
    func observeSavedFilters() async {
        for await list in observeSavedSearchFiltersUseCase.invoke() {
            savedFilters = list.compactMap { $0 as? SavedSearchFilter }
        }
    }
    func saveCurrentFilter(name: String) {
        let filter = SavedSearchFilter(
            id: 0, name: name, query: query, selectedType: selectedType,
            selectedSort: selectedSort, selectedPeriod: selectedPeriod,
            selectedBaseModels: selectedBaseModels, nsfwFilterLevel: nsfwFilterLevel,
            isFreshFindEnabled: isFreshFindEnabled, excludedTags: excludedTags,
            includedTags: includedTags, selectedSources: selectedSources, savedAt: 0
        )
        Task { try? await saveSearchFilterUseCase.invoke(name: name, filter: filter) }
    }
    func applyFilter(_ filter: SavedSearchFilter) {
        loadTask?.cancel()
        query = filter.query
        selectedType = filter.selectedType
        selectedSort = filter.selectedSort
        selectedPeriod = filter.selectedPeriod
        selectedBaseModels = Set(filter.selectedBaseModels.compactMap { $0 as? BaseModel })
        nsfwFilterLevel = filter.nsfwFilterLevel
        isFreshFindEnabled = filter.isFreshFindEnabled
        includedTags = filter.includedTags.compactMap { $0 as? String }
        excludedTags = filter.excludedTags.compactMap { $0 as? String }
        selectedSources = Set(filter.selectedSources.compactMap { $0 as? Core_domainModelSource })
        reloadModels()
    }
    func deleteSavedFilter(id: Int64) {
        Task { try? await deleteSavedSearchFilterUseCase.invoke(id: id) }
    }
}
