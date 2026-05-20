import Foundation

enum CornerRadius {
    static let small: CGFloat = 4
    static let card: CGFloat = 12
    static let large: CGFloat = 16
    static let chip: CGFloat = 50
    static let image: CGFloat = 8
    static let searchBar: CGFloat = 8
}

/// Frame dimension tokens for icon containers and interactive areas.
/// Font-based icon sizes remain in CivitDeckFonts.swift.
enum IconSize {
    static let small: CGFloat = 16
    static let medium: CGFloat = 24
    static let large: CGFloat = 48
    static let xlarge: CGFloat = 56
}

/// Sizes for small indicator dots (unread badges, status indicators).
enum DotSize {
    static let pagination: CGFloat = 6
    static let indicator: CGFloat = 8
}

/// Sizes for floating action buttons.
enum FabSize {
    static let standard: CGFloat = 56
}

/// Sizes for badge counters (e.g. filter count overlay).
enum BadgeSize {
    static let small: CGFloat = 18
}

/// Sizes for content thumbnails (collection covers, history items).
enum ThumbnailSize {
    static let collection: CGFloat = IconSize.xlarge
}
