import Foundation

enum DeepLink {
    case modelDetail(id: Int64)
    case search(query: String)
    case favorites
    case trending
}

enum DeepLinkHandler {
    static func handle(_ url: URL) -> DeepLink? {
        guard url.scheme == "civitdeck" else { return nil }
        switch url.host {
        case "model":
            guard let idStr = url.pathComponents.dropFirst().first,
                  let id = Int64(idStr) else { return nil }
            return .modelDetail(id: id)
        case "search":
            let query = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "q" })?.value ?? ""
            return .search(query: query)
        case "favorites":
            return .favorites
        case "trending":
            return .trending
        default:
            return nil
        }
    }

    static func handleSpotlight(uniqueIdentifier: String) -> DeepLink? {
        guard let id = Int64(uniqueIdentifier) else { return nil }
        return .modelDetail(id: id)
    }
}
