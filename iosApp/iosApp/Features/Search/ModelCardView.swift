import SwiftUI
import Shared

struct ModelCardView: View {
    let model: Model

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            thumbnailImage

            VStack(alignment: .leading, spacing: 4) {
                Text(model.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .lineLimit(1)

                Text(model.type.name)
                    .font(.caption2)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(Color(.systemGray5))
                    .clipShape(Capsule())

                statsRow
            }
            .padding(8)
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
    }

    private var thumbnailImage: some View {
        Group {
            let url = model.modelVersions.first?.images.first?.url
            if let urlString = url, let imageUrl = URL(string: urlString) {
                Color(.systemGray5)
                    .aspectRatio(1, contentMode: .fit)
                    .overlay {
                        AsyncImage(url: imageUrl) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                            case .failure:
                                Image(systemName: "photo")
                                    .foregroundColor(.secondary)
                            case .empty:
                                ProgressView()
                            @unknown default:
                                Image(systemName: "photo")
                                    .foregroundColor(.secondary)
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
            .fill(Color(.systemGray5))
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                Image(systemName: "photo")
                    .foregroundColor(.secondary)
            }
    }

    private var statsRow: some View {
        HStack(spacing: 8) {
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
                .font(.system(size: 9))
            Text(value)
                .font(.caption2)
        }
        .foregroundColor(.secondary)
    }
}
