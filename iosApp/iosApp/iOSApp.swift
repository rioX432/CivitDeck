import SwiftUI
import Shared

@main
struct iOSApp: App {
    @StateObject private var themeManager = ThemeManager()
    @StateObject private var spotlightManager = SpotlightIndexManager()
    @StateObject private var router = NavigationRouter()

    init() {
        KoinKt.doInitKoin(appDeclaration: { _ in })
        Task { try? await KoinKt.initializeAuth() }
    }

    var body: some Scene {
        WindowGroup {
            ThemedContentView()
                .environmentObject(themeManager)
                .environmentObject(router)
                .onAppear {
                    ShortcutsRouter.shared.navigationRouter = router
                }
                .task { await themeManager.observeAccentColor() }
                .task { await themeManager.observeAmoledDarkMode() }
                .onOpenURL { url in
                    if let deepLink = DeepLinkHandler.handle(url) {
                        router.route(to: deepLink)
                    }
                }
                .onContinueUserActivity(CSSearchableItemActionType) { activity in
                    guard let identifier = activity.userInfo?[CSSearchableItemActivityIdentifier] as? String,
                          let deepLink = DeepLinkHandler.handleSpotlight(uniqueIdentifier: identifier)
                    else { return }
                    router.route(to: deepLink)
                }
        }
    }
}

/// Wraps ContentView with accent tint applied based on the current color scheme.
private struct ThemedContentView: View {
    @EnvironmentObject private var themeManager: ThemeManager
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ContentView()
            .tint(themeManager.tintColor(for: colorScheme))
    }
}
