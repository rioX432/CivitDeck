import AppIntents

struct ShowTrendingModelsIntent: AppIntent {
    static var title: LocalizedStringResource = "Show Trending Models"
    static var description = IntentDescription("Opens CivitDeck and shows trending AI models.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        await ShortcutsRouter.shared.navigateToTrending()
        return .result()
    }
}

struct SearchModelsIntent: AppIntent {
    static var title: LocalizedStringResource = "Search Models"
    static var description = IntentDescription("Opens CivitDeck and searches for AI models.")
    static var openAppWhenRun: Bool = true

    @Parameter(title: "Search Query", description: "The term to search for.")
    var query: String

    func perform() async throws -> some IntentResult {
        await ShortcutsRouter.shared.navigateToSearch(query: query)
        return .result()
    }
}

struct ShowFavoritesIntent: AppIntent {
    static var title: LocalizedStringResource = "Show My Favorites"
    static var description = IntentDescription("Opens CivitDeck and shows your favorited models.")
    static var openAppWhenRun: Bool = true

    func perform() async throws -> some IntentResult {
        await ShortcutsRouter.shared.navigateToFavorites()
        return .result()
    }
}
