import CoreSpotlight
import Foundation
import OSLog
import Shared

@MainActor
final class SpotlightIndexManager: ObservableObject {
    private static let logger = Logger(subsystem: Bundle.main.bundleIdentifier ?? "CivitDeck", category: "Spotlight")
    private let observeFavoritesUseCase: Core_domainObserveFavoritesUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.observeFavoritesUseCase = KoinHelper.shared.getObserveFavoritesUseCase()
        observeTask = Task { await observe() }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observe() async {
        for await list in observeFavoritesUseCase.invoke() {
            let favorites = list.compactMap { $0 as? Core_domainFavoriteModelSummary }
            indexFavorites(favorites)
        }
    }

    private func indexFavorites(_ favorites: [Core_domainFavoriteModelSummary]) {
        let items = favorites.map { SpotlightItemBuilder.build(from: $0) }
        CSSearchableIndex.default().indexSearchableItems(items) { error in
            if let error {
                Self.logger.error("Spotlight indexing failed: \(error.localizedDescription)")
            }
        }
        // Remove items no longer favorited by replacing the full domain set
        let identifiers = favorites.map { "\($0.id)" }
        CSSearchableIndex.default().fetchLastClientState { _, _ in }
        if identifiers.isEmpty {
            CSSearchableIndex.default().deleteSearchableItems(
                withDomainIdentifiers: [SpotlightItemBuilder.domainIdentifier]
            ) { _ in }
        }
    }

    func removeAll() {
        CSSearchableIndex.default().deleteSearchableItems(
            withDomainIdentifiers: [SpotlightItemBuilder.domainIdentifier]
        ) { _ in }
    }
}
