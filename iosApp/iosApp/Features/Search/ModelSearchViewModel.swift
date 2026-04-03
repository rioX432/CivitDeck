import Foundation
import UIKit
import Shared
import os

private let logger = Logger(subsystem: "com.riox432.civitdeck", category: "Search")

@MainActor
final class ModelSearchViewModel: ObservableObject {
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

    private let getModelsUseCase: GetModelsUseCase
    private let multiSourceSearchUseCase: MultiSourceSearchUseCase
    private let getRecommendationsUseCase: GetRecommendationsUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let observeSearchHistoryUseCase: ObserveSearchHistoryUseCase
    private let addSearchHistoryUseCase: AddSearchHistoryUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private let deleteSearchHistoryItemUseCase: DeleteSearchHistoryItemUseCase
    private let getViewedModelIdsUseCase: GetViewedModelIdsUseCase
    private let getExcludedTagsUseCase: GetExcludedTagsUseCase
    private let addExcludedTagUseCase: AddExcludedTagUseCase
    private let removeExcludedTagUseCase: RemoveExcludedTagUseCase
    let getHiddenModelIdsUseCase: GetHiddenModelIdsUseCase
    let hideModelUseCase: HideModelUseCase
    private let observeGridColumnsUseCase: ObserveGridColumnsUseCase
    private let observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase
    private let observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase
    private let observeOwnedModelHashesUseCase: ObserveOwnedModelHashesUseCase
    let toggleFavoriteUseCase: ToggleFavoriteUseCase
    let observeFavoritesUseCase: ObserveFavoritesUseCase
    let observeSavedSearchFiltersUseCase: ObserveSavedSearchFiltersUseCase
    let saveSearchFilterUseCase: SaveSearchFilterUseCase
    let deleteSavedSearchFilterUseCase: DeleteSavedSearchFilterUseCase
    private let observeQualityThresholdUseCase: ObserveQualityThresholdUseCase
    private let trackRecommendationClickUseCase: TrackRecommendationClickUseCase
    private var qualityThreshold: Int32 = 0
    private var thresholdObserveTask: Task<Void, Never>?
    private var nextCursor: String?
    var loadTask: Task<Void, Never>?
    var hiddenModelIds: Set<KotlinLong> = []
    private var sortWatermark: Double?
    private var multiSourcePage: Int32 = 1

    private let pageSize: Int32 = 20
    private let maxFetchIterations = 5
    private var memoryWarningToken: NSObjectProtocol?

    init() {
        self.getModelsUseCase = KoinHelper.shared.getModelsUseCase()
        self.multiSourceSearchUseCase = KoinHelper.shared.getMultiSourceSearchUseCase()
        self.getRecommendationsUseCase = KoinHelper.shared.getRecommendationsUseCase()
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.observeSearchHistoryUseCase = KoinHelper.shared.getObserveSearchHistoryUseCase()
        self.addSearchHistoryUseCase = KoinHelper.shared.getAddSearchHistoryUseCase()
        self.clearSearchHistoryUseCase = KoinHelper.shared.getClearSearchHistoryUseCase()
        self.deleteSearchHistoryItemUseCase = KoinHelper.shared.getDeleteSearchHistoryItemUseCase()
        self.getViewedModelIdsUseCase = KoinHelper.shared.getViewedModelIdsUseCase()
        self.getExcludedTagsUseCase = KoinHelper.shared.getExcludedTagsUseCase()
        self.addExcludedTagUseCase = KoinHelper.shared.getAddExcludedTagUseCase()
        self.removeExcludedTagUseCase = KoinHelper.shared.getRemoveExcludedTagUseCase()
        self.getHiddenModelIdsUseCase = KoinHelper.shared.getHiddenModelIdsUseCase()
        self.hideModelUseCase = KoinHelper.shared.getHideModelUseCase()
        self.observeGridColumnsUseCase = KoinHelper.shared.getObserveGridColumnsUseCase()
        self.observeDefaultSortOrderUseCase = KoinHelper.shared.getObserveDefaultSortOrderUseCase()
        self.observeDefaultTimePeriodUseCase = KoinHelper.shared.getObserveDefaultTimePeriodUseCase()
        self.observeOwnedModelHashesUseCase = KoinHelper.shared.getObserveOwnedModelHashesUseCase()
        self.toggleFavoriteUseCase = KoinHelper.shared.getToggleFavoriteUseCase()
        self.observeFavoritesUseCase = KoinHelper.shared.getObserveFavoritesUseCase()
        self.observeSavedSearchFiltersUseCase = KoinHelper.shared.getObserveSavedSearchFiltersUseCase()
        self.saveSearchFilterUseCase = KoinHelper.shared.getSaveSearchFilterUseCase()
        self.deleteSavedSearchFilterUseCase = KoinHelper.shared.getDeleteSavedSearchFilterUseCase()
        self.observeQualityThresholdUseCase = KoinHelper.shared.getObserveQualityThresholdUseCase()
        self.trackRecommendationClickUseCase = KoinHelper.shared.getTrackRecommendationClickUseCase()
        loadExcludedTags()
        loadDefaults()
        observeQualityThreshold()
        loadRecommendations()
        memoryWarningToken = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            let count = self?.models.count ?? 0
            logger.warning("Memory warning received. models.count=\(count)")
        }
    }

    deinit { memoryWarningToken.map { NotificationCenter.default.removeObserver($0) } }

    private func loadDefaults() {
        Task {
            if let sort = try? await observeDefaultSortOrderUseCase.invoke().first(where: { _ in true }) {
                selectedSort = sort
            }
            if let period = try? await observeDefaultTimePeriodUseCase.invoke().first(where: { _ in true }) {
                selectedPeriod = period
            }
            loadModels()
        }
    }

    func observeGridColumns() async {
        for await value in observeGridColumnsUseCase.invoke() {
            gridColumns = value.int32Value
        }
    }

    func observeOwnedHashes() async {
        for await value in observeOwnedModelHashesUseCase.invoke() {
            ownedHashes = Set(value.compactMap { $0 as? String })
        }
    }

    private func loadRecommendations() {
        Task {
            do {
                let sections = try await getRecommendationsUseCase.invoke()
                recommendations = sections
                WidgetDataWriter.writeTrendingModel(from: sections)
            } catch {
                // Non-critical, silently fail
            }
        }
    }

    func trackRecommendationClick(modelId: Int64) {
        Task {
            try? await trackRecommendationClickUseCase.invoke(modelId: modelId)
        }
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            let prev = nsfwFilterLevel
            nsfwFilterLevel = value
            if prev != value {
                loadTask?.cancel()
                resetPaginationState()
                loadModels()
            }
        }
    }

    func observeSearchHistory() async {
        for await value in observeSearchHistoryUseCase.invoke() {
            searchHistory = value
        }
    }

    func onSearch() {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        if !trimmed.isEmpty {
            Task { try? await addSearchHistoryUseCase.invoke(query: trimmed) }
        }
        loadTask?.cancel()
        resetPaginationState()
        loadModels()
    }

    func onHistoryItemClick(_ item: String) {
        query = item
        onSearch()
    }

    func removeSearchHistoryItem(_ item: String) {
        Task { try? await deleteSearchHistoryItemUseCase.invoke(query: item) }
    }
    func clearSearchHistory() {
        Task { try? await clearSearchHistoryUseCase.invoke() }
    }
    func onTypeSelected(_ type: ModelType?) {
        loadTask?.cancel()
        selectedType = type
        resetPaginationState()
        loadModels()
    }

    func onBaseModelToggled(_ baseModel: BaseModel) {
        loadTask?.cancel()
        if selectedBaseModels.contains(baseModel) {
            selectedBaseModels.remove(baseModel)
        } else { selectedBaseModels.insert(baseModel) }
        resetPaginationState()
        loadModels()
    }
    func onSortSelected(_ sort: CivitSortOrder) {
        loadTask?.cancel(); selectedSort = sort; resetPaginationState(); loadModels()
    }
    func onPeriodSelected(_ period: TimePeriod) {
        loadTask?.cancel(); selectedPeriod = period; resetPaginationState(); loadModels()
    }
    func onFreshFindToggled() {
        loadTask?.cancel(); isFreshFindEnabled.toggle(); resetPaginationState(); loadModels()
    }
    func onQualityFilterToggled() {
        loadTask?.cancel(); isQualityFilterEnabled.toggle(); resetPaginationState(); loadModels()
    }
    func toggleSource(_ source: Core_domainModelSource) {
        if selectedSources.contains(source) {
            guard selectedSources.count > 1 else { return }
            selectedSources.remove(source)
        } else { selectedSources.insert(source) }
        reloadModels()
    }

    func resetFilters() {
        loadTask?.cancel()
        selectedType = nil
        selectedSort = .mostDownloaded
        selectedPeriod = .allTime
        selectedBaseModels = []
        isFreshFindEnabled = false
        isQualityFilterEnabled = false
        includedTags = []
        selectedSources = [.civitai]
        resetPaginationState()
        loadModels()
    }

    func addIncludedTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespaces).lowercased()
        guard !trimmed.isEmpty, !includedTags.contains(trimmed) else { return }
        includedTags.append(trimmed)
        reloadModels()
    }

    func removeIncludedTag(_ tag: String) {
        includedTags.removeAll { $0 == tag }
        reloadModels()
    }

    func addExcludedTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespaces).lowercased()
        guard !trimmed.isEmpty else { return }
        Task {
            try? await addExcludedTagUseCase.invoke(tag: trimmed)
            loadExcludedTags()
            reloadModels()
        }
    }

    func removeExcludedTag(_ tag: String) {
        Task {
            try? await removeExcludedTagUseCase.invoke(tag: tag)
            loadExcludedTags()
            reloadModels()
        }
    }

    private func loadExcludedTags() {
        Task {
            excludedTags = (try? await getExcludedTagsUseCase.invoke()) ?? []
            hiddenModelIds = (try? await getHiddenModelIdsUseCase.invoke()) ?? []
        }
    }

    private func observeQualityThreshold() {
        thresholdObserveTask = Task {
            for await threshold in observeQualityThresholdUseCase.invoke() {
                guard !Task.isCancelled else { return }
                if let intThreshold = threshold as? Int32 {
                    self.qualityThreshold = intThreshold
                }
            }
        }
    }

    func reloadModels() {
        loadTask?.cancel()
        resetPaginationState()
        loadModels()
    }

    func onModelAppear(_ modelId: Int64) {
        guard let index = models.firstIndex(where: { $0.id == modelId }) else { return }
        if index >= models.count - 10 {
            loadMore()
        }
    }

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }
        isLoadingMore = true
        loadModels(isLoadMore: true)
    }

    func refresh() async {
        loadTask?.cancel()
        nextCursor = nil
        hasMore = true
        sortWatermark = nil
        loadModels(isRefresh: true)
        await loadTask?.value
    }

    private func resetPaginationState() {
        models = []
        nextCursor = nil
        hasMore = true
        sortWatermark = nil
        multiSourcePage = 1
    }
}

// MARK: - Pagination & Filtering

private extension ModelSearchViewModel {
    func sortValueOf(_ model: Model) -> Double {
        switch selectedSort {
        case .mostDownloaded: return Double(model.stats.downloadCount)
        case .highestRated: return model.stats.rating
        case .newest: return Double(model.id)
        default: return Double(model.id)
        }
    }

    struct FetchResult { let models: [Model]; let cursor: String? }

    func loadModels(isLoadMore: Bool = false, isRefresh: Bool = false) {
        loadTask = Task {
            if isLoadMore { isLoadingMore = true } else { isLoading = true }
            error = nil
            do {
                let result = try await fetchAndAccumulate(isLoadMore: isLoadMore)
                guard !Task.isCancelled else { return }
                if isLoadMore {
                    models.append(contentsOf: result.models)
                } else {
                    models = result.models
                }
                logger.debug("Load complete. total models=\(self.models.count) isLoadMore=\(isLoadMore)")
                nextCursor = result.cursor
                hasMore = result.cursor != nil
                isLoading = false
                isLoadingMore = false
            } catch is CancellationError {
                return
            } catch {
                guard !Task.isCancelled else { return }
                self.error = error.localizedDescription
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    private var isCivitaiOnly: Bool {
        selectedSources == [.civitai]
    }

    func fetchAndAccumulate(isLoadMore: Bool) async throws -> FetchResult {
        if isCivitaiOnly {
            return try await fetchCivitaiOnly(isLoadMore: isLoadMore)
        } else {
            return try await fetchMultiSource(isLoadMore: isLoadMore)
        }
    }

    private func fetchCivitaiOnly(isLoadMore: Bool) async throws -> FetchResult {
        let baseModelList: [BaseModel]? = selectedBaseModels.isEmpty ? nil : Array(selectedBaseModels)
        let nsfw: KotlinBoolean? = nsfwFilterLevel == .off ? KotlinBoolean(bool: false) : nil
        let viewedIds: Set<KotlinLong> = isFreshFindEnabled
            ? try await getViewedModelIdsUseCase.invoke() : []
        var accumulated: [Model] = []
        var accumulatedIds = Set<Int64>()
        var currentCursor: String? = isLoadMore ? nextCursor : nil
        var fetchedNextCursor: String?
        let pageWatermark = sortWatermark
        if isLoadMore { accumulatedIds = Set(models.map { $0.id }) }
        for iteration in 0..<maxFetchIterations {
            guard !Task.isCancelled else { break }
            if accumulated.count >= Int(pageSize) { break }
            let result = try await getModelsUseCase.invoke(
                query: query.isEmpty ? nil : query,
                tag: includedTags.first, type: selectedType, sort: selectedSort,
                period: selectedPeriod, baseModels: baseModelList,
                cursor: currentCursor, limit: KotlinInt(int: pageSize), nsfw: nsfw
            )
            let allModels = result.items.compactMap { $0 as? Model }
            var filtered = applyClientFilters(allModels, viewedIds: viewedIds)
            if let watermark = pageWatermark {
                filtered = filtered.filter { sortValueOf($0) <= watermark }
            }
            for model in filtered where !accumulatedIds.contains(model.id) {
                accumulated.append(model)
                accumulatedIds.insert(model.id)
            }
            logger.debug("Iteration \(iteration): fetched=\(allModels.count) filtered=\(filtered.count) accumulated=\(accumulated.count)")
            fetchedNextCursor = result.metadata.nextCursor
            if fetchedNextCursor == nil || fetchedNextCursor == currentCursor { break }
            currentCursor = fetchedNextCursor
        }
        accumulated.sort { sortValueOf($0) > sortValueOf($1) }
        if !accumulated.isEmpty { sortWatermark = accumulated.map { sortValueOf($0) }.min()! }
        return FetchResult(models: accumulated, cursor: fetchedNextCursor)
    }

    /// Multi-source: queries CivitAI, HuggingFace, and/or TensorArt in parallel.
    /// Limitation: HuggingFace/TensorArt use page-based pagination while CivitAI uses cursor.
    private func fetchMultiSource(isLoadMore: Bool) async throws -> FetchResult {
        let viewedIds: Set<KotlinLong> = isFreshFindEnabled
            ? try await getViewedModelIdsUseCase.invoke() : []
        let currentCursor: String? = isLoadMore ? nextCursor : nil
        if !isLoadMore { multiSourcePage = 1 }
        let result = try await multiSourceSearchUseCase.invoke(
            query: query.isEmpty ? nil : query,
            selectedSources: selectedSources,
            cursor: currentCursor,
            page: multiSourcePage,
            limit: pageSize
        )
        let allModels = result.models.compactMap { $0 as? Model }
        let filtered = applyClientFilters(allModels, viewedIds: viewedIds)
        multiSourcePage += 1
        return FetchResult(models: filtered, cursor: result.nextCursor)
    }

    func applyClientFilters(_ models: [Model], viewedIds: Set<KotlinLong>) -> [Model] {
        var filtered = models.filterNsfwImages(nsfwFilterLevel)
        if isFreshFindEnabled {
            filtered = filtered.filter { !viewedIds.contains(KotlinLong(value: $0.id)) }
        }
        if includedTags.count > 1 {
            let remaining = Set(includedTags.dropFirst().map { $0.lowercased() })
            filtered = filtered.filter { model in
                let modelTags = Set(model.tags.compactMap { ($0 as? String)?.lowercased() })
                return modelTags.isSuperset(of: remaining)
            }
        }
        if !excludedTags.isEmpty {
            let excluded = Set(excludedTags)
            filtered = filtered.filter { model in
                model.tags.allSatisfy { !excluded.contains(($0 as? String)?.lowercased() ?? "") }
            }
        }
        if !hiddenModelIds.isEmpty {
            filtered = filtered.filter { !hiddenModelIds.contains(KotlinLong(value: $0.id)) }
        }
        if isQualityFilterEnabled && qualityThreshold > 0 {
            filtered = filtered.filter {
                FeedQualityScoreHelper.calculate(stats: $0.stats) >= qualityThreshold
            }
        }
        return filtered
    }
}
