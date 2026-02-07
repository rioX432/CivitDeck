import Foundation
import Shared

@MainActor
final class ModelSearchViewModel: ObservableObject {
    @Published var models: [Model] = []
    @Published var query: String = ""
    @Published var selectedType: ModelType? = nil
    @Published var selectedSort: SortOrder = .mostDownloaded
    @Published var selectedPeriod: TimePeriod = .allTime
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String? = nil
    @Published var hasMore: Bool = true

    private let getModelsUseCase: GetModelsUseCase
    private var nextCursor: String? = nil
    private var loadTask: Task<Void, Never>? = nil

    private let pageSize: Int32 = 20

    init() {
        self.getModelsUseCase = KoinHelper.shared.getModelsUseCase()
        loadModels()
    }

    func onSearch() {
        loadTask?.cancel()
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onTypeSelected(_ type: ModelType?) {
        loadTask?.cancel()
        selectedType = type
        models = []
        nextCursor = nil
        hasMore = true
        loadModels()
    }

    func onSortSelected(_ sort: SortOrder) {
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
                let result = try await getModelsUseCase.invoke(
                    query: query.isEmpty ? nil : query,
                    tag: nil,
                    type: selectedType,
                    sort: selectedSort,
                    period: selectedPeriod,
                    cursor: isLoadMore ? nextCursor : nil,
                    limit: KotlinInt(int: pageSize)
                )

                guard !Task.isCancelled else { return }

                let newModels = result.items.compactMap { $0 as? Model }
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
