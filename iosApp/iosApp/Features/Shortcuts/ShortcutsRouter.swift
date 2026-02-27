import Foundation

@MainActor
final class ShortcutsRouter {
    static let shared = ShortcutsRouter()
    private init() {}

    // The router is injected at app launch
    var navigationRouter: NavigationRouter?

    func navigateToTrending() {
        navigationRouter?.route(to: .trending)
    }

    func navigateToSearch(query: String) {
        navigationRouter?.route(to: .search(query: query))
    }

    func navigateToFavorites() {
        navigationRouter?.route(to: .favorites)
    }
}
