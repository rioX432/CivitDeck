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

                HStack(spacing: Spacing.xs) {
                    Text(model.type.name)
                        .font(.civitLabelSmall)
                        .padding(.horizontal, Spacing.sm)
                        .padding(.vertical, Spacing.xxs)
                        .background(Color.civitSurfaceVariant)
                        .clipShape(Capsule())
                    SourceBadgeView(source: model.source)
                }

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
        // Safest static image first (never a video URL); mirrors Android's ModelCardLayout.
        let candidate = model.browseThumbnailCandidates().first
        let urlString = candidate?.thumbnailUrl(width: 450)
        let blurRadius = ModelCardView.cardBlurRadius(for: candidate?.nsfwLevel)
        return CivitAsyncImageView(imageUrl: urlString, aspectRatio: 1)
            .blur(radius: blurRadius)
            .clipped()
            .overlay(alignment: .topLeading) {
                if blurRadius > 0 {
                    NsfwBadgeView()
                        .padding(Spacing.sm)
                }
            }
            .parallaxEffect(offset: parallaxOffset)
    }

    /// Fixed per-level blur for browse cards (matches Android: Mature 16, X 24);
    /// the gallery blur sliders intentionally do not apply here.
    static func cardBlurRadius(for level: NsfwLevel?) -> CGFloat {
        switch level {
        case .mature: return 16
        case .x: return 24
        default: return 0
        }
    }

    private var statsRow: some View {
        ModelStatsRow(
            downloadCount: model.stats.downloadCount,
            favoriteCount: model.stats.favoriteCount,
            rating: model.stats.rating
        )
    }
}

struct NsfwBadgeView: View {
    var body: some View {
        Text("NSFW")
            .font(.civitLabelXSmall)
            .foregroundColor(.white)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xxs)
            .background(Color.black.opacity(0.6))
            .clipShape(Capsule())
            .accessibilityLabel("NSFW content")
    }
}

struct SourceBadgeView: View {
    let source: Core_domainModelSource

    var body: some View {
        if source != .civitai {
            Text(badgeLabel)
                .font(.civitLabelXSmall)
                .foregroundColor(.white)
                .padding(.horizontal, Spacing.sm)
                .padding(.vertical, Spacing.xxs)
                .background(badgeColor)
                .clipShape(Capsule())
        }
    }

    private var badgeLabel: String {
        switch source {
        case .huggingFace: return "HF"
        case .tensorArt: return "TA"
        default: return ""
        }
    }

    private var badgeColor: Color {
        switch source {
        case .huggingFace: return .huggingFaceBadge
        case .tensorArt: return .tensorArtBadge
        default: return .clear
        }
    }
}
