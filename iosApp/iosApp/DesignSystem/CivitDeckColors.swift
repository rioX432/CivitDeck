import SwiftUI
import UIKit

// Generated from seed #1e40af using Material Color Utilities (TonalSpot)
extension Color {
    // MARK: - Primary
    static let civitPrimary = Color(light: hex(0x3755C3), dark: hex(0xB8C4FF))
    static let civitOnPrimary = Color(light: hex(0xFFFFFF), dark: hex(0x002584))
    static let civitPrimaryContainer = Color(light: hex(0xDDE1FF), dark: hex(0x173BAB))
    static let civitOnPrimaryContainer = Color(light: hex(0x001453), dark: hex(0xDDE1FF))

    // MARK: - Secondary
    static let civitSecondary = Color(light: hex(0x5A5D72), dark: hex(0xC2C5DD))
    static let civitOnSecondary = Color(light: hex(0xFFFFFF), dark: hex(0x2C2F42))
    static let civitSecondaryContainer = Color(light: hex(0xDEE1F9), dark: hex(0x424659))
    static let civitOnSecondaryContainer = Color(light: hex(0x171B2C), dark: hex(0xDEE1F9))

    // MARK: - Tertiary
    static let civitTertiary = Color(light: hex(0x75546F), dark: hex(0xE4BAD9))
    static let civitOnTertiary = Color(light: hex(0xFFFFFF), dark: hex(0x43273F))
    static let civitTertiaryContainer = Color(light: hex(0xFFD7F4), dark: hex(0x5C3D56))
    static let civitOnTertiaryContainer = Color(light: hex(0x2C1229), dark: hex(0xFFD7F4))

    // MARK: - Error
    static let civitError = Color(light: hex(0xBA1A1A), dark: hex(0xFFB4AB))
    static let civitOnError = Color(light: hex(0xFFFFFF), dark: hex(0x690005))
    static let civitErrorContainer = Color(light: hex(0xFFDAD6), dark: hex(0x93000A))
    static let civitOnErrorContainer = Color(light: hex(0x410002), dark: hex(0xFFDAD6))

    // MARK: - Surface
    static let civitSurface = Color(light: hex(0xFBF8FD), dark: hex(0x131316))
    static let civitOnSurface = Color(light: hex(0x1B1B1F), dark: hex(0xE4E1E6))
    static let civitSurfaceVariant = Color(light: hex(0xE2E1EC), dark: hex(0x45464F))
    static let civitOnSurfaceVariant = Color(light: hex(0x45464F), dark: hex(0xC6C5D0))

    // MARK: - Surface Containers
    static let civitSurfaceContainer = Color(light: hex(0xF0EDF1), dark: hex(0x1F1F23))
    static let civitSurfaceContainerHigh = Color(light: hex(0xEAE7EC), dark: hex(0x2A2A2D))
    static let civitSurfaceContainerHighest = Color(light: hex(0xE4E1E6), dark: hex(0x343438))

    // MARK: - Outline
    static let civitOutline = Color(light: hex(0x767680), dark: hex(0x90909A))
    static let civitOutlineVariant = Color(light: hex(0xC6C5D0), dark: hex(0x45464F))

    // MARK: - Inverse
    static let civitInverseSurface = Color(light: hex(0x303034), dark: hex(0xE4E1E6))
    static let civitInverseOnSurface = Color(light: hex(0xF2F0F4), dark: hex(0x303034))
    static let civitInversePrimary = Color(light: hex(0xB8C4FF), dark: hex(0x3755C3))

    // MARK: - Scrim
    static let civitScrim = hex(0x000000)

    // MARK: - External Source Badges
    static let huggingFaceBadge = hex(0xFF9D00)
    static let tensorArtBadge = hex(0x9C27B0)

    // MARK: - AMOLED Dark Mode
    static let amoledSurface = hex(0x000000)
    static let amoledSurfaceContainer = hex(0x0F0F0F)
    static let amoledSurfaceContainerHigh = hex(0x171717)
}

// MARK: - Accent Color Tint

/// Light/dark pair for accent color tinting in iOS.
struct AccentColorPair {
    let light: Color
    let dark: Color
}

/// Full primary palette for an accent tint (M3-approximated tonal values).
struct PrimaryPalette {
    let primary: AccentColorPair
    let onPrimary: AccentColorPair
    let primaryContainer: AccentColorPair
    let onPrimaryContainer: AccentColorPair
    let inversePrimary: AccentColorPair
}

/// Curated accent tint colors matching the shared AccentColor enum.
enum AccentTint: String, CaseIterable {
    case blue, indigo, purple, pink, red, orange, amber, green, teal, cyan

    var displayName: String { rawValue.capitalized }

    var colors: AccentColorPair {
        switch self {
        case .blue: return AccentColorPair(light: .hex(0x3755C3), dark: .hex(0xB8C4FF))
        case .indigo: return AccentColorPair(light: .hex(0x5856D6), dark: .hex(0xC5C0FF))
        case .purple: return AccentColorPair(light: .hex(0x7C3AED), dark: .hex(0xD4BBFF))
        case .pink: return AccentColorPair(light: .hex(0xDB2777), dark: .hex(0xFFB1C8))
        case .red: return AccentColorPair(light: .hex(0xDC2626), dark: .hex(0xFFB4AB))
        case .orange: return AccentColorPair(light: .hex(0xEA580C), dark: .hex(0xFFB68E))
        case .amber: return AccentColorPair(light: .hex(0xD97706), dark: .hex(0xFFBB3D))
        case .green: return AccentColorPair(light: .hex(0x16A34A), dark: .hex(0x6CE892))
        case .teal: return AccentColorPair(light: .hex(0x0D9488), dark: .hex(0x4EDBC8))
        case .cyan: return AccentColorPair(light: .hex(0x0891B2), dark: .hex(0x5DD5FC))
        }
    }

    var primaryPalette: PrimaryPalette {
        switch self {
        case .blue:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x3755C3), dark: .hex(0xB8C4FF)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x002584)),
                primaryContainer: AccentColorPair(light: .hex(0xDDE1FF), dark: .hex(0x173BAB)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x001453), dark: .hex(0xDDE1FF)),
                inversePrimary: AccentColorPair(light: .hex(0xB8C4FF), dark: .hex(0x3755C3))
            )
        case .indigo:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x5856D6), dark: .hex(0xC5C0FF)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x2A0099)),
                primaryContainer: AccentColorPair(light: .hex(0xE6DEFF), dark: .hex(0x3F00B3)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x19005C), dark: .hex(0xE6DEFF)),
                inversePrimary: AccentColorPair(light: .hex(0xC5C0FF), dark: .hex(0x5856D6))
            )
        case .purple:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x7C3AED), dark: .hex(0xD4BBFF)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x4400A8)),
                primaryContainer: AccentColorPair(light: .hex(0xEDDCFF), dark: .hex(0x5B10C9)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x270057), dark: .hex(0xEDDCFF)),
                inversePrimary: AccentColorPair(light: .hex(0xD4BBFF), dark: .hex(0x7C3AED))
            )
        case .pink:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0xDB2777), dark: .hex(0xFFB1C8)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x8E004A)),
                primaryContainer: AccentColorPair(light: .hex(0xFFD9E2), dark: .hex(0xB80060)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x3F0021), dark: .hex(0xFFD9E2)),
                inversePrimary: AccentColorPair(light: .hex(0xFFB1C8), dark: .hex(0xDB2777))
            )
        case .red:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0xDC2626), dark: .hex(0xFFB4AB)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x690005)),
                primaryContainer: AccentColorPair(light: .hex(0xFFDAD6), dark: .hex(0xBA1A1A)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x410002), dark: .hex(0xFFDAD6)),
                inversePrimary: AccentColorPair(light: .hex(0xFFB4AB), dark: .hex(0xDC2626))
            )
        case .orange:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0xEA580C), dark: .hex(0xFFB68E)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x7A2E00)),
                primaryContainer: AccentColorPair(light: .hex(0xFFDBC8), dark: .hex(0xC04000)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x2E0E00), dark: .hex(0xFFDBC8)),
                inversePrimary: AccentColorPair(light: .hex(0xFFB68E), dark: .hex(0xEA580C))
            )
        case .amber:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0xD97706), dark: .hex(0xFFBB3D)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x704300)),
                primaryContainer: AccentColorPair(light: .hex(0xFFDEA1), dark: .hex(0xB86100)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x241A00), dark: .hex(0xFFDEA1)),
                inversePrimary: AccentColorPair(light: .hex(0xFFBB3D), dark: .hex(0xD97706))
            )
        case .green:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x16A34A), dark: .hex(0x6CE892)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x00391A)),
                primaryContainer: AccentColorPair(light: .hex(0xC8F5D5), dark: .hex(0x008538)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x00210D), dark: .hex(0xC8F5D5)),
                inversePrimary: AccentColorPair(light: .hex(0x6CE892), dark: .hex(0x16A34A))
            )
        case .teal:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x0D9488), dark: .hex(0x4EDBC8)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x003731)),
                primaryContainer: AccentColorPair(light: .hex(0xBDF5EC), dark: .hex(0x007A6E)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x00201D), dark: .hex(0xBDF5EC)),
                inversePrimary: AccentColorPair(light: .hex(0x4EDBC8), dark: .hex(0x0D9488))
            )
        case .cyan:
            return PrimaryPalette(
                primary: AccentColorPair(light: .hex(0x0891B2), dark: .hex(0x5DD5FC)),
                onPrimary: AccentColorPair(light: .white, dark: .hex(0x003545)),
                primaryContainer: AccentColorPair(light: .hex(0xBEEBFF), dark: .hex(0x006C8E)),
                onPrimaryContainer: AccentColorPair(light: .hex(0x001E2B), dark: .hex(0xBEEBFF)),
                inversePrimary: AccentColorPair(light: .hex(0x5DD5FC), dark: .hex(0x0891B2))
            )
        }
    }
}

// MARK: - Dynamic Theme

/// Environment-driven theme that resolves primary colors based on the active accent tint.
struct CivitTheme {
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let inversePrimary: Color

    static func from(accent: AccentTint, colorScheme: ColorScheme) -> CivitTheme {
        let palette = accent.primaryPalette
        let isDark = colorScheme == .dark
        return CivitTheme(
            primary: isDark ? palette.primary.dark : palette.primary.light,
            onPrimary: isDark ? palette.onPrimary.dark : palette.onPrimary.light,
            primaryContainer: isDark ? palette.primaryContainer.dark : palette.primaryContainer.light,
            onPrimaryContainer: isDark ? palette.onPrimaryContainer.dark : palette.onPrimaryContainer.light,
            inversePrimary: isDark ? palette.inversePrimary.dark : palette.inversePrimary.light
        )
    }
}

private struct CivitThemeKey: EnvironmentKey {
    static let defaultValue = CivitTheme.from(accent: .blue, colorScheme: .light)
}

extension EnvironmentValues {
    var civitTheme: CivitTheme {
        get { self[CivitThemeKey.self] }
        set { self[CivitThemeKey.self] = newValue }
    }
}

// MARK: - Helpers

extension Color {
    init(light: Color, dark: Color) {
        self.init(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
    }

    static func hex(_ value: UInt) -> Color {
        Color(
            red: Double((value >> 16) & 0xFF) / 255.0,
            green: Double((value >> 8) & 0xFF) / 255.0,
            blue: Double(value & 0xFF) / 255.0
        )
    }
}
