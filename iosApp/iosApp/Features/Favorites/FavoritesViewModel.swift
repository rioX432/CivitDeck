import Foundation
import Shared

@MainActor
final class FavoritesViewModel: ObservableObject {
    @Published var favorites: [FavoriteModelSummary] = []
    @Published var gridColumns: Int32 = 2

    private let observeFavoritesUseCase: ObserveFavoritesUseCase
    private let observeGridColumnsUseCase: ObserveGridColumnsUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.observeFavoritesUseCase = KoinHelper.shared.getObserveFavoritesUseCase()
        self.observeGridColumnsUseCase = KoinHelper.shared.getObserveGridColumnsUseCase()
        observeTask = Task { await observe() }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observe() async {
        for await list in observeFavoritesUseCase.invoke() {
            let items = list.compactMap { $0 as? FavoriteModelSummary }
            self.favorites = items
        }
    }

    func observeGridColumns() async {
        for await value in observeGridColumnsUseCase.invoke() {
            gridColumns = value.int32Value
        }
    }
}
