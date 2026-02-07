import Foundation
import Shared

@MainActor
final class FavoritesViewModel: ObservableObject {
    @Published var favorites: [FavoriteModelSummary] = []

    private let observeFavoritesUseCase: ObserveFavoritesUseCase

    init() {
        self.observeFavoritesUseCase = KoinHelper.shared.getObserveFavoritesUseCase()
    }

    func observe() async {
        for await list in observeFavoritesUseCase.invoke() {
            let items = list.compactMap { $0 as? FavoriteModelSummary }
            self.favorites = items
        }
    }
}
