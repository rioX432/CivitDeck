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
        Group {
            let image = model.modelVersions.first?.images.first
            let urlString = image.flatMap { $0.thumbnailUrl(width: 600) }
            if let urlString, let imageUrl = URL(string: urlString) {
                Color.civitSurfaceVariant
                    .aspectRatio(imageAspectRatio, contentMode: .fit)
                    .overlay {
                        CachedAsyncImage(url: imageUrl) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                                    .transition(.opacity)
                            case .failure:
                                Image(systemName: "photo")
                                    .foregroundColor(.civitOnSurfaceVariant)
                            case .empty:
                                Rectangle()
                                    .fill(Color.civitSurfaceVariant)
                                    .shimmer()
                            @unknown default:
                                Image(systemName: "photo")
                                    .foregroundColor(.civitOnSurfaceVariant)
                            }
                        }
                    }
                    .clipped()
            } else {
                Rectangle()
                    .fill(Color.civitSurfaceVariant)
                    .aspectRatio(imageAspectRatio, contentMode: .fit)
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
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
