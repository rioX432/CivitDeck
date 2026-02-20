import SwiftUI

enum AdaptiveGrid {
    private static let mediumBonus = 0
    private static let regularBonus = 2

    static func columns(
        userPreference: Int,
        sizeClass: UserInterfaceSizeClass?
    ) -> [GridItem] {
        let count = userPreference + bonus(for: sizeClass)
        return Array(repeating: GridItem(.flexible(), spacing: Spacing.sm), count: count)
    }

    static func columns(sizeClass: UserInterfaceSizeClass?) -> [GridItem] {
        columns(userPreference: 2, sizeClass: sizeClass)
    }

    static func columnCount(
        userPreference: Int,
        sizeClass: UserInterfaceSizeClass?
    ) -> Int {
        userPreference + bonus(for: sizeClass)
    }

    static func columnCount(sizeClass: UserInterfaceSizeClass?) -> Int {
        columnCount(userPreference: 2, sizeClass: sizeClass)
    }

    private static func bonus(for sizeClass: UserInterfaceSizeClass?) -> Int {
        switch sizeClass {
        case .regular:
            return regularBonus
        default:
            return 0
        }
    }
}
