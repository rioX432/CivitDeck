import Foundation
import UIKit
import Shared
import os

private let logger = Logger(subsystem: "com.riox432.civitdeck", category: "Search")

@MainActor
final class ModelSearchViewModel: ObservableObject {
    let vm: Feature_searchModelSearchViewModel
    private let store = ViewModelStore()

    @Published var models: [Model] = []
    @Published var query: String = ""
    @Published var selectedType: ModelType?
    @Published var selectedBaseModels: Set<BaseModel> = []
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var selectedSort: CivitSortOrder = .mostDownloaded
    @Published var selectedPeriod: TimePeriod = .allTime
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String?
    @Published var hasMore: Bool = true
    @Published var searchHistory: [String] = []
    @Published var recommendations: [RecommendationSection] = []
    @Published var isFreshFindEnabled: Bool = false
    @Published var isQualityFilterEnabled: Bool = false
    @Published var includedTags: [String] = []
    @Published var excludedTags: [String] = []
    @Published var gridColumns: Int32 = 2
    @Published var ownedHashes: Set<String> = []
    @Published var favoriteIds: Set<Int64> = []
    @Published var savedFilters: [SavedSearchFilter] = []
    @Published var selectedSources: Set<Core_domainModelSource> = [.civitai]

    private var memoryWarningToken: NSObjectProtocol?

    init() {
        vm = KoinHelper.shared.createModelSearchViewModel()
        store.put(key: "ModelSearchViewModel", viewModel: vm)
        memoryWarningToken = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            let count = self?.models.count ?? 0
            logger.warning("Memory warning received. models.count=\(count)")
        }
    }

    deinit {
        memoryWarningToken.map { NotificationCenter.default.removeObserver($0) }
        store.clear()
    }

    // MARK: - State observation

    func observeUiState() async {
        for await state in vm.uiState {
            models = state.models as? [Model] ?? []
            query = state.query
            selectedType = state.selectedType
            selectedBaseModels = Set(state.selectedBaseModels.compactMap { $0 as? BaseModel })
            nsfwFilterLevel = state.nsfwFilterLevel
            selectedSort = state.selectedSort
            selectedPeriod = state.selectedPeriod
            isLoading = state.isLoading
            isLoadingMore = state.isLoadingMore
            error = state.error
            hasMore = state.hasMore
            isFreshFindEnabled = state.isFreshFindEnabled
            isQualityFilterEnabled = state.isQualityFilterEnabled
            includedTags = state.includedTags.compactMap { $0 as? String }
            excludedTags = state.excludedTags.compactMap { $0 as? String }
            recommendations = state.recommendations as? [RecommendationSection] ?? []
            selectedSources = Set(state.selectedSources.compactMap { $0 as? Core_domainModelSource })
            WidgetDataWriter.writeTrendingModel(from: recommendations)
        }
    }

    func observeSearchHistory() async {
        for await value in vm.searchHistory {
            searchHistory = value as? [String] ?? []
        }
    }

    func observeGridColumns() async {
        for await value in vm.gridColumns {
            gridColumns = value.int32Value
        }
    }

    func observeOwnedHashes() async {
        for await value in vm.ownedHashes {
            ownedHashes = Set(value.compactMap { $0 as? String })
        }
    }

    func observeFavorites() async {
        for await value in vm.favoriteIds {
            favoriteIds = Set(value.compactMap { ($0 as? KotlinLong)?.int64Value })
        }
    }

    func observeSavedFilters() async {
        for await value in vm.savedFilters {
            savedFilters = value as? [SavedSearchFilter] ?? []
        }
    }

    // MARK: - Search & filter actions

    func onQueryChange(_ query: String) {
        self.query = query
        vm.onQueryChange(query: query)
    }

    func onSearch() {
        vm.onSearch()
    }

    func onHistoryItemClick(_ item: String) {
        vm.onHistoryItemClick(query: item)
    }

    func removeSearchHistoryItem(_ item: String) {
        vm.removeSearchHistoryItem(query: item)
    }

    func clearSearchHistory() {
        vm.clearSearchHistory()
    }

    func resetFilters() {
        vm.resetFilters()
    }

    func onTypeSelected(_ type: ModelType?) {
        vm.onTypeSelected(type: type)
    }

    func onBaseModelToggled(_ baseModel: BaseModel) {
        vm.onBaseModelToggled(baseModel: baseModel)
    }

    func onSortSelected(_ sort: CivitSortOrder) {
        vm.onSortSelected(sort: sort)
    }

    func onPeriodSelected(_ period: TimePeriod) {
        vm.onPeriodSelected(period: period)
    }

    func onFreshFindToggled() {
        vm.onFreshFindToggled()
    }

    func onQualityFilterToggled() {
        vm.onQualityFilterToggled()
    }

    func toggleSource(_ source: Core_domainModelSource) {
        vm.toggleSource(source: source)
    }

    func addIncludedTag(_ tag: String) {
        vm.onAddIncludedTag(tag: tag)
    }

    func removeIncludedTag(_ tag: String) {
        vm.onRemoveIncludedTag(tag: tag)
    }

    func addExcludedTag(_ tag: String) {
        vm.onAddExcludedTag(tag: tag)
    }

    func removeExcludedTag(_ tag: String) {
        vm.onRemoveExcludedTag(tag: tag)
    }

    // MARK: - Hidden models

    func hideModel(_ modelId: Int64, name: String) {
        vm.onHideModel(modelId: modelId, modelName: name)
        // Also remove from local list for immediate UI feedback
        models.removeAll { $0.id == modelId }
    }

    // MARK: - Pagination

    func onModelAppear(_ modelId: Int64) {
        guard let index = models.firstIndex(where: { $0.id == modelId }) else { return }
        if index >= models.count - 10 {
            vm.loadMore()
        }
    }

    func loadMore() {
        vm.loadMore()
    }

    func refresh() async {
        vm.refresh()
        // Wait briefly for state to propagate
        try? await Task.sleep(nanoseconds: 100_000_000)
    }

    // MARK: - Favorites

    func toggleFavorite(_ model: Model) {
        vm.toggleFavorite(model: model)
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

    // MARK: - Saved filters

    func saveCurrentFilter(name: String) {
        vm.saveCurrentFilter(name: name)
    }

    func applyFilter(_ filter: SavedSearchFilter) {
        vm.applyFilter(filter: filter)
    }

    func deleteSavedFilter(id: Int64) {
        vm.deleteSavedFilter(id: id)
    }

    // MARK: - Recommendations

    func trackRecommendationClick(modelId: Int64) {
        vm.trackRecommendationClick(modelId: modelId)
    }
}
