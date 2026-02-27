import SwiftUI
import Shared

/// Observes theme preferences from the shared module and provides
/// accent tint color and AMOLED mode to the SwiftUI view hierarchy.
@MainActor
final class ThemeManager: ObservableObject {
    @Published var accentTint: AccentTint = .blue
    @Published var amoledDarkMode: Bool = false

    private let observeAccentColorUseCase: ObserveAccentColorUseCase
    private let observeAmoledDarkModeUseCase: ObserveAmoledDarkModeUseCase

    init() {
        self.observeAccentColorUseCase = KoinHelper.shared.getObserveAccentColorUseCase()
        self.observeAmoledDarkModeUseCase = KoinHelper.shared.getObserveAmoledDarkModeUseCase()
    }

    func observeAccentColor() async {
        for await value in observeAccentColorUseCase.invoke() {
            accentTint = AccentTint(rawValue: value.name.lowercased()) ?? .blue
        }
    }

    func observeAmoledDarkMode() async {
        for await value in observeAmoledDarkModeUseCase.invoke() {
            amoledDarkMode = value.boolValue
        }
    }

    /// The resolved tint color based on current color scheme.
    func tintColor(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? accentTint.colors.dark : accentTint.colors.light
    }
}
