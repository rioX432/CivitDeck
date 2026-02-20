import SwiftUI
import Shared

struct FavoritesScreen: View {
    @StateObject private var viewModel = FavoritesViewModel()
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var columns: [GridItem] {
        AdaptiveGrid.columns(userPreference: Int(viewModel.gridColumns), sizeClass: sizeClass)
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.favorites.isEmpty {
                    emptyView
                } else {
                    favoritesGrid
                }
            }
            .navigationBarHidden(true)
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
            }
        }
        .task { await viewModel.observeGridColumns() }
    }

    private var favoritesGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.favorites, id: \.id) { model in
                    NavigationLink(value: model.id) {
                        FavoriteCardView(model: model)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Spacing.md)
        }
    }

    private var emptyView: some View {
        VStack(spacing: Spacing.sm) {
            Image(systemName: "heart.slash")
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No favorites yet")
                .font(.civitTitleMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Models you favorite will appear here")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}

private struct FavoriteCardView: View {
    let model: FavoriteModelSummary

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
            if let urlString = model.thumbnailUrl, let imageUrl = URL(string: urlString) {
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
                Rectangle()
                    .fill(Color.civitSurfaceVariant)
                    .aspectRatio(1, contentMode: .fit)
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
            }
        }
    }

    private var statsRow: some View {
        HStack(spacing: Spacing.sm) {
            statItem(
                icon: "arrow.down.circle",
                value: FormatUtils.shared.formatCount(count: model.downloadCount)
            )
            statItem(
                icon: "heart",
                value: FormatUtils.shared.formatCount(count: model.favoriteCount)
            )
            statItem(
                icon: "star",
                value: FormatUtils.shared.formatRating(rating: model.rating)
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
