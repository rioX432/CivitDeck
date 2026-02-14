import Foundation
import Shared

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
    @Published var excludedTags: [String] = []

    private let getModelsUseCase: GetModelsUseCase
    private let getRecommendationsUseCase: GetRecommendationsUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let observeSearchHistoryUseCase: ObserveSearchHistoryUseCase
    private let addSearchHistoryUseCase: AddSearchHistoryUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private let getViewedModelIdsUseCase: GetViewedModelIdsUseCase
    private let getExcludedTagsUseCase: GetExcludedTagsUseCase
    private let addExcludedTagUseCase: AddExcludedTagUseCase
    private let removeExcludedTagUseCase: RemoveExcludedTagUseCase
    private let getHiddenModelIdsUseCase: GetHiddenModelIdsUseCase
    private let hideModelUseCase: HideModelUseCase
    private var nextCursor: String?
    private var loadTask: Task<Void, Never>?
    private var hiddenModelIds: Set<KotlinLong> = []

    private let pageSize: Int32 = 20

    init() {
        self.getModelsUseCase = KoinHelper.shared.getModelsUseCase()
        self.getRecommendationsUseCase = KoinHelper.shared.getRecommendationsUseCase()
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.observeSearchHistoryUseCase = KoinHelper.shared.getObserveSearchHistoryUseCase()
        self.addSearchHistoryUseCase = KoinHelper.shared.getAddSearchHistoryUseCase()
        self.clearSearchHistoryUseCase = KoinHelper.shared.getClearSearchHistoryUseCase()
        self.getViewedModelIdsUseCase = KoinHelper.shared.getViewedModelIdsUseCase()
        self.getExcludedTagsUseCase = KoinHelper.shared.getExcludedTagsUseCase()
        self.addExcludedTagUseCase = KoinHelper.shared.getAddExcludedTagUseCase()
        self.removeExcludedTagUseCase = KoinHelper.shared.getRemoveExcludedTagUseCase()
        self.getHiddenModelIdsUseCase = KoinHelper.shared.getHiddenModelIdsUseCase()
        self.hideModelUseCase = KoinHelper.shared.getHideModelUseCase()
        loadExcludedTags()
        loadModels()
        loadRecommendations()
    }

    private func loadRecommendations() {
        Task {
            do {
                let sections = try await getRecommendationsUseCase.invoke()
                recommendations = sections
            } catch {
                // Non-critical, silently fail
            }
        }
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            let prev = nsfwFilterLevel
            nsfwFilterLevel = value
            if prev != value {
                loadTask?.cancel()
                models = []
                nextCursor = nil
                hasMore = true
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
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onHistoryItemClick(_ item: String) {
        query = item
        onSearch()
    }

    func clearSearchHistory() {
        Task { try? await clearSearchHistoryUseCase.invoke() }
    }

    func onTypeSelected(_ type: ModelType?) {
        loadTask?.cancel()
        selectedType = type
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onBaseModelToggled(_ baseModel: BaseModel) {
        loadTask?.cancel()
        if selectedBaseModels.contains(baseModel) {
            selectedBaseModels.remove(baseModel)
        } else {
            selectedBaseModels.insert(baseModel)
        }
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onSortSelected(_ sort: CivitSortOrder) {
        loadTask?.cancel()
        selectedSort = sort
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onPeriodSelected(_ period: TimePeriod) {
        loadTask?.cancel()
        selectedPeriod = period
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onFreshFindToggled() {
        loadTask?.cancel()
        isFreshFindEnabled.toggle()
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func resetFilters() {
        loadTask?.cancel()
        selectedType = nil
        selectedSort = .mostDownloaded
        selectedPeriod = .allTime
        selectedBaseModels = []
        isFreshFindEnabled = false
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
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

    func hideModel(_ modelId: Int64, name: String) {
        Task {
            try? await hideModelUseCase.invoke(modelId: modelId, modelName: name)
            hiddenModelIds = try await getHiddenModelIdsUseCase.invoke()
            models.removeAll { $0.id == modelId }
        }
    }

    private func loadExcludedTags() {
        Task {
            excludedTags = (try? await getExcludedTagsUseCase.invoke()) ?? []
            hiddenModelIds = (try? await getHiddenModelIdsUseCase.invoke()) ?? []
        }
    }

    private func reloadModels() {
        loadTask?.cancel()
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }
        loadModels(isLoadMore: true)
    }

    func refresh() async {
        loadTask?.cancel()
        nextCursor = nil
        hasMore = true
        loadModels(isRefresh: true)
        await loadTask?.value
    }

    private func loadModels(isLoadMore: Bool = false, isRefresh: Bool = false) {
        loadTask = Task {
            if isLoadMore {
                isLoadingMore = true
            } else {
                isLoading = true
            }
            error = nil

            do {
                let baseModelList: [BaseModel]? = selectedBaseModels.isEmpty ? nil : Array(selectedBaseModels)
                let nsfw: KotlinBoolean? = nsfwFilterLevel == .off ? KotlinBoolean(bool: false) : nil
                let result = try await getModelsUseCase.invoke(
                    query: query.isEmpty ? nil : query,
                    tag: nil,
                    type: selectedType,
                    sort: selectedSort,
                    period: selectedPeriod,
                    baseModels: baseModelList,
                    cursor: isLoadMore ? nextCursor : nil,
                    limit: KotlinInt(int: pageSize),
                    nsfw: nsfw
                )

                guard !Task.isCancelled else { return }

                let allModels = result.items.compactMap { $0 as? Model }
                var newModels = allModels.filterNsfwImages(nsfwFilterLevel)
                if isFreshFindEnabled {
                    let viewedIds = try await getViewedModelIdsUseCase.invoke()
                    newModels = newModels.filter { !viewedIds.contains(KotlinLong(value: $0.id)) }
                }
                if !excludedTags.isEmpty {
                    let excluded = Set(excludedTags)
                    newModels = newModels.filter { model in
                        model.tags.allSatisfy { tag in
                            !excluded.contains((tag as? String)?.lowercased() ?? "")
                        }
                    }
                }
                if !hiddenModelIds.isEmpty {
                    newModels = newModels.filter { !hiddenModelIds.contains(KotlinLong(value: $0.id)) }
                }
                if isLoadMore {
                    models.append(contentsOf: newModels)
                } else {
                    models = newModels
                }
                nextCursor = result.metadata.nextCursor
                hasMore = result.metadata.nextCursor != nil
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
}

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
