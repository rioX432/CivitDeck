import SwiftUI

extension Font {
    // MARK: - Text fonts (Dynamic Type enabled)

    // headlineSmall: ~24pt Regular — scales with .title2
    static let civitHeadlineSmall = Font.title2

    // titleMedium: ~16pt Medium — scales with .headline
    static let civitTitleMedium = Font.headline

    // titleSmall: ~14pt Medium — scales with .subheadline (weight applied at call site)
    static let civitTitleSmall = Font.subheadline.weight(.medium)

    // bodyMedium: ~14pt Regular — scales with .subheadline
    static let civitBodyMedium = Font.subheadline

    // bodySmall: ~12pt Regular — scales with .caption
    static let civitBodySmall = Font.caption

    // labelMedium: ~12pt Medium — scales with .caption (weight applied at call site)
    static let civitLabelMedium = Font.caption.weight(.medium)

    // labelSmall: ~11pt Medium — scales with .caption2
    static let civitLabelSmall = Font.caption2.weight(.medium)

    // badgeLabel: ~11pt Bold — scales with .caption2
    static let civitBadgeLabel = Font.caption2.weight(.bold)

    // labelXSmall: ~10pt Bold — tiny tag remove icons
    static let civitLabelXSmall = Font.caption2.weight(.bold)

    // labelXSmallSemibold: ~10pt Semibold — tag action icons
    static let civitLabelXSmallSemibold = Font.caption2.weight(.semibold)

    // monoCaption: caption monospaced — code/JSON editors
    static let civitMonoCaption = Font.system(.caption, design: .monospaced)

    // MARK: - Icon fonts (fixed size — decorative, no Dynamic Type)

    // iconMedium: 22pt Regular — action icon buttons
    static let civitIconMedium = Font.system(size: 22)

    // iconLarge: 44pt Regular — large icon overlays (e.g. play buttons)
    static let civitIconLarge = Font.system(size: 44)

    // iconExtraLarge: 48pt Regular — large status/placeholder icons
    static let civitIconExtraLarge = Font.system(size: 48)

    // iconXSmall: 9pt Bold — tiny compact badges
    static let civitIconXSmall = Font.system(size: 9, weight: .bold)

    // iconSmallSemibold: 16pt Semibold — small icon overlays (e.g. comparison slider)
    static let civitIconSmallSemibold = Font.system(size: 16, weight: .semibold)
}
