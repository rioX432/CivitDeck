import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinKt.doInitKoin(appDeclaration: { _ in })
        Task { try? await KoinKt.doInitializeAuth() }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
