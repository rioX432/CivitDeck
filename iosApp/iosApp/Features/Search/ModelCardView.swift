import SwiftUI
import Shared

struct ModelCardView: View {
    let model: Model

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            thumbnailImage

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(model.name)
                    .font(.civitTitleSmall)
                    .lineLimit(1)

                Text(model.type.name)
                    .font(.civitLabelSmall)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())

                statsRow
            }
            .padding(Spacing.sm)
        }
        .background(Color.civitSurface)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }

    private var thumbnailImage: some View {
        Group {
            let url = model.modelVersions.first?.images.first?.url
            if let urlString = url, let imageUrl = URL(string: urlString) {
                Color.civitSurfaceVariant
                    .aspectRatio(1, contentMode: .fit)
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
                imagePlaceholder
            }
        }
    }

    private var imagePlaceholder: some View {
        Rectangle()
            .fill(Color.civitSurfaceVariant)
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                Image(systemName: "photo")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
    }

    private var statsRow: some View {
        HStack(spacing: Spacing.sm) {
            statItem(
                icon: "arrow.down.circle",
                value: FormatUtils.shared.formatCount(count: model.stats.downloadCount)
            )
            statItem(
                icon: "heart",
                value: FormatUtils.shared.formatCount(count: model.stats.favoriteCount)
            )
            statItem(
                icon: "star",
                value: FormatUtils.shared.formatRating(rating: model.stats.rating)
            )
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
}
