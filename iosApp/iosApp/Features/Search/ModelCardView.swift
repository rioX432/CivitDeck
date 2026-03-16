import SwiftUI
import Shared

struct ModelCardView: View {
    let model: Model
    var isOwned: Bool = false
    var parallaxOffset: CGFloat = 0
    @Environment(\.civitTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            thumbnailImage

            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack(spacing: Spacing.xs) {
                    Text(model.name)
                        .font(.civitTitleSmall)
                        .lineLimit(1)
                    if isOwned {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.civitLabelXSmall)
                            .foregroundColor(theme.primary)
                            .accessibilityLabel("Owned")
                    }
                }

                Text(model.type.name)
                    .font(.civitLabelSmall)
                    .padding(.horizontal, Spacing.xsPlus)
                    .padding(.vertical, Spacing.xxs)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())

                statsRow
            }
            .padding(Spacing.sm)
        }
        .background(Color.civitSurface)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .springPress()
    }

    private var thumbnailImage: some View {
        let urlString = model.modelVersions.first?.images.first.flatMap { $0.thumbnailUrl(width: 450) }
        return CivitAsyncImageView(imageUrl: urlString, aspectRatio: 1)
            .parallaxEffect(offset: parallaxOffset)
    }

    private var statsRow: some View {
        ModelStatsRow(
            downloadCount: model.stats.downloadCount,
            favoriteCount: model.stats.favoriteCount,
            rating: model.stats.rating
        )
    }
}
