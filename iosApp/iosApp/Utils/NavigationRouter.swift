import Foundation
import SwiftUI

@MainActor
final class NavigationRouter: ObservableObject {
    @Published var pendingDeepLink: DeepLink?

    func route(to deepLink: DeepLink) {
        pendingDeepLink = deepLink
    }

    func consume() -> DeepLink? {
        let link = pendingDeepLink
        pendingDeepLink = nil
        return link
    }
}
