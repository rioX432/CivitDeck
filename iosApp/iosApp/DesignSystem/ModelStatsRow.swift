import SwiftUI

/// Displays model statistics (downloads, favorites, rating, optional comments).
///
/// When `commentCount` is nil, renders a compact icon+value row (3 items).
/// When `commentCount` is provided, renders an expanded value+label column row (4 items).
struct ModelStatsRow: View {
    let downloadCount: Int
    let favoriteCount: Int
    let rating: Double?
    var commentCount: Int? = nil

    var body: some View {
        if let commentCount {
            expandedRow(commentCount: commentCount)
        } else {
            compactRow
        }
    }

    // MARK: - Compact (icon + value inline)

    private var compactRow: some View {
        HStack(spacing: Spacing.sm) {
            statItem(icon: "arrow.down.circle",
                     value: FormatUtils.shared.formatCount(count: downloadCount))
            statItem(icon: "heart",
                     value: FormatUtils.shared.formatCount(count: favoriteCount))
            statItem(icon: "star",
                     value: ratingText)
        }
    }

    private func statItem(icon: String, value: String) -> some View {
        HStack(spacing: 2) {
            Image(systemName: icon)
                .font(.system(size: IconSize.statIcon))
            Text(value)
                .font(.civitLabelSmall)
        }
        .foregroundColor(.civitOnSurfaceVariant)
    }

    // MARK: - Expanded (value above label, spaced evenly)

    private func expandedRow(commentCount: Int) -> some View {
        HStack {
            statColumn(value: FormatUtils.shared.formatCount(count: downloadCount),
                       label: "Downloads")
            Spacer()
            statColumn(value: FormatUtils.shared.formatCount(count: favoriteCount),
                       label: "Favorites")
            Spacer()
            statColumn(value: ratingText, label: "Rating")
            Spacer()
            statColumn(value: FormatUtils.shared.formatCount(count: commentCount),
                       label: "Comments")
        }
    }

    private func statColumn(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.civitTitleMedium)
            Text(label)
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    // MARK: - Helpers

    private var ratingText: String {
        guard let rating else { return "-" }
        return FormatUtils.shared.formatRating(rating: rating)
    }
}
