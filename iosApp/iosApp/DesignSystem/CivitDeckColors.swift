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
}

// MARK: - Helpers

private extension Color {
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
