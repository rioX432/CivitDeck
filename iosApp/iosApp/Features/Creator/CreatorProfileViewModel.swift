import Foundation
import Shared

@MainActor
final class CreatorProfileViewModel: ObservableObject {
    @Published var models: [Model] = []
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String?
    @Published var hasMore: Bool = true

    let username: String

    private let getCreatorModelsUseCase: GetCreatorModelsUseCase
    private var nextCursor: String?
    private var loadTask: Task<Void, Never>?

    private let pageSize: Int32 = 20

    init(username: String) {
        self.username = username
        self.getCreatorModelsUseCase = KoinHelper.shared.getCreatorModelsUseCase()
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
                let result = try await getCreatorModelsUseCase.invoke(
                    username: username,
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
