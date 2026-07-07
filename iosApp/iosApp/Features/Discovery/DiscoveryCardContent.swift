import SwiftUI
import Shared

private let imageAspectRatio: CGFloat = 0.75

struct DiscoveryCardContent: View {
    let model: Model

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            cardImage
            cardInfo
        }
        .background(Color.civitSurface)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
        .shadow(color: .black.opacity(0.15), radius: 8, x: 0, y: 4)
    }

    private var cardImage: some View {
        // Safest static image first (never a video URL); mirrors Android's card behavior.
        let candidate = model.browseThumbnailCandidates().first
        let urlString = candidate?.thumbnailUrl(width: 450)
        let blurRadius = ModelCardView.cardBlurRadius(for: candidate?.nsfwLevel)
        return CivitAsyncImageView(imageUrl: urlString, aspectRatio: imageAspectRatio)
            .blur(radius: blurRadius)
            .clipped()
            .overlay(alignment: .topLeading) {
                if blurRadius > 0 {
                    NsfwBadgeView()
                        .padding(Spacing.sm)
                }
            }
    }

    private var cardInfo: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(model.name)
                .font(.civitTitleMedium)
                .lineLimit(1)

            Text(model.type.name)
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)

            if let creator = model.creator {
                Text("by \(creator.username)")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .padding(Spacing.md)
    }
}
