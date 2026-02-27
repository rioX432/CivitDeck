import SwiftUI
import Shared

@main
struct iOSApp: App {
    @StateObject private var themeManager = ThemeManager()

    init() {
        KoinKt.doInitKoin(appDeclaration: { _ in })
        Task { try? await KoinKt.initializeAuth() }
    }

    var body: some Scene {
        WindowGroup {
            ThemedContentView()
                .environmentObject(themeManager)
                .task { await themeManager.observeAccentColor() }
                .task { await themeManager.observeAmoledDarkMode() }
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
