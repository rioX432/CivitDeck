import Foundation
import Shared

@MainActor
final class ModelSearchViewModel: ObservableObject {
    @Published var models: [Model] = []
    @Published var query: String = ""
    @Published var selectedType: ModelType? = nil
    @Published var selectedBaseModels: Set<BaseModel> = []
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var selectedSort: CivitSortOrder = .mostDownloaded
    @Published var selectedPeriod: TimePeriod = .allTime
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String? = nil
    @Published var hasMore: Bool = true
    @Published var searchHistory: [String] = []
    @Published var recommendations: [RecommendationSection] = []

    private let getModelsUseCase: GetModelsUseCase
    private let getRecommendationsUseCase: GetRecommendationsUseCase
    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let setNsfwFilterUseCase: SetNsfwFilterUseCase
    private let observeSearchHistoryUseCase: ObserveSearchHistoryUseCase
    private let addSearchHistoryUseCase: AddSearchHistoryUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private var nextCursor: String? = nil
    private var loadTask: Task<Void, Never>? = nil

    private let pageSize: Int32 = 20

    init() {
        self.getModelsUseCase = KoinHelper.shared.getModelsUseCase()
        self.getRecommendationsUseCase = KoinHelper.shared.getRecommendationsUseCase()
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.setNsfwFilterUseCase = KoinHelper.shared.getSetNsfwFilterUseCase()
        self.observeSearchHistoryUseCase = KoinHelper.shared.getObserveSearchHistoryUseCase()
        self.addSearchHistoryUseCase = KoinHelper.shared.getAddSearchHistoryUseCase()
        self.clearSearchHistoryUseCase = KoinHelper.shared.getClearSearchHistoryUseCase()
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
            nsfwFilterLevel = value
        }
    }

    func onNsfwFilterToggle() {
        let newLevel: NsfwFilterLevel = nsfwFilterLevel == .off ? .all : .off
        nsfwFilterLevel = newLevel
        Task {
            try? await setNsfwFilterUseCase.invoke(level: newLevel)
        }
        loadTask?.cancel()
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
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

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }
        loadModels(isLoadMore: true)
    }

    func refresh() {
        loadTask?.cancel()
        nextCursor = nil
        hasMore = true
        loadModels(isRefresh: true)
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
                let result = try await getModelsUseCase.invoke(
                    query: query.isEmpty ? nil : query,
                    tag: nil,
                    type: selectedType,
                    sort: selectedSort,
                    period: selectedPeriod,
                    baseModels: baseModelList,
                    cursor: isLoadMore ? nextCursor : nil,
                    limit: KotlinInt(int: pageSize)
                )

                guard !Task.isCancelled else { return }

                let allModels = result.items.compactMap { $0 as? Model }
                let newModels = nsfwFilterLevel == .off
                    ? allModels.filter { !$0.nsfw }
                    : allModels
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
