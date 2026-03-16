import SwiftUI
import Shared

/// Observes theme preferences from the shared module and provides
/// accent tint color, AMOLED mode, color scheme override, and custom theme to the SwiftUI view hierarchy.
@MainActor
final class ThemeManager: ObservableObject {
    @Published var accentTint: AccentTint = .blue
    @Published var amoledDarkMode: Bool = false
    @Published var colorSchemeOverride: ColorScheme?
    @Published var customThemePrimary: Color?
    @Published var hasCustomTheme: Bool = false

    private let observeAccentColorUseCase: ObserveAccentColorUseCase
    private let observeAmoledDarkModeUseCase: ObserveAmoledDarkModeUseCase
    private let observeThemeModeUseCase: ObserveThemeModeUseCase
    private let getActiveThemeUseCase: GetActiveThemeUseCase

    init() {
        self.observeAccentColorUseCase = KoinHelper.shared.getObserveAccentColorUseCase()
        self.observeAmoledDarkModeUseCase = KoinHelper.shared.getObserveAmoledDarkModeUseCase()
        self.observeThemeModeUseCase = KoinHelper.shared.getObserveThemeModeUseCase()
        self.getActiveThemeUseCase = KoinHelper.shared.getGetActiveThemeUseCase()
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

    func observeThemeMode() async {
        for await value in observeThemeModeUseCase.invoke() {
            switch value {
            case ThemeMode.light:
                colorSchemeOverride = .light
            case ThemeMode.dark:
                colorSchemeOverride = .dark
            default:
                colorSchemeOverride = nil
            }
        }
    }

    func observeActiveTheme() async {
        for await plugin in getActiveThemeUseCase.invoke() {
            if let themePlugin = plugin {
                let isDark = colorSchemeOverride == .dark
                let scheme = themePlugin.getColorScheme(isDark: isDark)
                customThemePrimary = Color(argb: scheme.primary)
                hasCustomTheme = true
            } else {
                customThemePrimary = nil
                hasCustomTheme = false
            }
        }
    }

    /// The resolved tint color based on current color scheme.
    func tintColor(for colorScheme: ColorScheme) -> Color {
        if let custom = customThemePrimary {
            return custom
        }
        return colorScheme == .dark ? accentTint.colors.dark : accentTint.colors.light
    }

    /// The full dynamic primary palette resolved for the current accent and color scheme.
    func theme(for colorScheme: ColorScheme) -> CivitTheme {
        if let custom = customThemePrimary {
            return CivitTheme(
                primary: custom,
                onPrimary: colorScheme == .dark ? Color.hex(0x002584) : .white,
                primaryContainer: custom.opacity(0.15),
                onPrimaryContainer: custom,
                inversePrimary: custom
            )
        }
        return CivitTheme.from(accent: accentTint, colorScheme: colorScheme)
    }
}

// MARK: - ARGB Color Helper

extension Color {
    init(argb value: Int64) {
        let a = Double((value >> 24) & 0xFF) / 255.0
        let r = Double((value >> 16) & 0xFF) / 255.0
        let g = Double((value >> 8) & 0xFF) / 255.0
        let b = Double(value & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
