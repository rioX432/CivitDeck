import Foundation
import Shared
import os

private let logger = Logger(subsystem: "com.riox432.civitdeck", category: "Discovery")

struct DismissedCard {
    let model: Model
    let wasFavorited: Bool
}

@MainActor
final class SwipeDiscoveryViewModel: ObservableObject {
    @Published var cards: [Model] = []
    @Published var isLoading: Bool = false
    @Published var error: String?
    @Published var lastDismissed: DismissedCard?

    private let getDiscoveryModels: GetDiscoveryModelsUseCase
    private let toggleFavorite: ToggleFavoriteUseCase
    private let prefetchThreshold = 3

    /// Persists dismissed model IDs across ViewModel recreations within the same session.
    private static var sessionDismissedIds: Set<Int64> = []
    private var dismissedIds: Set<Int64> {
        get { Self.sessionDismissedIds }
        set { Self.sessionDismissedIds = newValue }
    }

    init() {
        self.getDiscoveryModels = KoinHelper.shared.getDiscoveryModelsUseCase()
        self.toggleFavorite = KoinHelper.shared.getToggleFavoriteUseCase()
        loadModels()
    }

    func loadModels() {
        guard !isLoading else { return }
        isLoading = true
        error = nil

        Task {
            do {
                let models = try await getDiscoveryModels.invoke(
                    cursor: nil,
                    limit: 20
                )
                let existingIds = Set(self.cards.map { $0.id })
                let allSeenIds = existingIds.union(self.dismissedIds)
                let newModels = models.filter { !allSeenIds.contains($0.id) }
                self.cards.append(contentsOf: newModels)
                self.isLoading = false
                self.prefetchUpcomingImages()
            } catch {
                logger.error("Failed to load discovery models: \(error)")
                self.error = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    func onSwipeRight(_ model: Model) {
        removeTopCard(model, wasFavorited: true)
        Task {
            do {
                try await toggleFavorite.invoke(model: model)
            } catch {
                logger.error("Failed to favorite: \(error)")
            }
        }
    }

    func onSwipeLeft(_ model: Model) {
        removeTopCard(model, wasFavorited: false)
    }

    func onSwipeUp(_ model: Model) -> Int64 {
        removeTopCard(model, wasFavorited: false)
        return model.id
    }

    func undoLastSwipe() {
        guard let dismissed = lastDismissed else { return }
        cards.insert(dismissed.model, at: 0)
        lastDismissed = nil

        if dismissed.wasFavorited {
            Task {
                do {
                    try await toggleFavorite.invoke(model: dismissed.model)
                } catch {
                    logger.error("Failed to undo favorite: \(error)")
                }
            }
        }
    }

    private func removeTopCard(_ model: Model, wasFavorited: Bool) {
        cards.removeAll { $0.id == model.id }
        dismissedIds.insert(model.id)
        lastDismissed = DismissedCard(model: model, wasFavorited: wasFavorited)
        prefetchUpcomingImages()

        if cards.count <= prefetchThreshold {
            loadModels()
        }
    }

    func prefetchUpcomingImages() {
        let upcoming = cards.dropFirst(1).prefix(5)
        let urls: [URL] = upcoming.compactMap { model in
            guard let urlString = model.modelVersions.first?.images.first.flatMap({ $0.thumbnailUrl(width: 450) }),
                  let url = URL(string: urlString) else { return nil }
            return url
        }
        ImagePrefetcher.prefetch(urls: urls)
    }
}
