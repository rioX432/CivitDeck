import SwiftUI
import Shared

struct FeedView: View {
    @StateObject private var viewModel = FeedViewModel()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading && viewModel.feedItems.isEmpty {
                    LoadingStateView()
                } else if let error = viewModel.errorMessage, viewModel.feedItems.isEmpty {
                    ErrorStateView(message: error) {
                        Task { await viewModel.loadFeed(forceRefresh: true) }
                    }
                } else if viewModel.feedItems.isEmpty {
                    emptyState
                } else {
                    feedList
                }
            }
            .navigationTitle("Feed")
            .task { await viewModel.loadFeed() }
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
            }
            .navigationDestination(for: String.self) { username in
                CreatorProfileScreen(username: username)
            }
        }
    }

    private var emptyState: some View {
        EmptyStateView(
            icon: "dot.radiowaves.up.forward",
            title: "No feed items yet",
            subtitle: "Follow creators to see their latest models here."
        )
    }

    private var feedList: some View {
        ScrollView {
            LazyVStack(spacing: Spacing.sm) {
                ForEach(viewModel.feedItems, id: \.modelId) { item in
                    FeedItemCard(item: item)
                }
            }
            .padding(.horizontal, Spacing.md)
        }
        .refreshable {
            await viewModel.loadFeed(forceRefresh: true)
        }
    }
}

private struct FeedItemCard: View {
    let item: FeedItem

    var body: some View {
        NavigationLink(value: item.modelId) {
            VStack(alignment: .leading, spacing: 0) {
                if let url = item.thumbnailUrl {
                    CachedAsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(16 / 9, contentMode: .fill)
                        case .failure:
                            Rectangle()
                                .fill(Color.civitSurfaceVariant.opacity(0.2))
                                .aspectRatio(16 / 9, contentMode: .fill)
                        default:
                            Rectangle()
                                .fill(Color.civitSurfaceVariant.opacity(0.2))
                                .aspectRatio(16 / 9, contentMode: .fill)
                                .modifier(ShimmerModifier())
                        }
                    }
                    .clipped()
                }

                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text(item.title)
                        .font(.civitTitleMedium)
                        .foregroundColor(.civitOnSurface)
                        .lineLimit(2)

                    HStack(spacing: Spacing.sm) {
                        Text(item.creatorUsername)
                            .font(.civitBodySmall)
                            .foregroundColor(.civitPrimary)

                        Text(item.type.name)
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitOnSurfaceVariant)

                        if item.isUnread {
                            Circle()
                                .fill(Color.civitPrimary)
                                .frame(width: 8, height: 8)
                        }
                    }
                }
                .padding(Spacing.md)
            }
            .background(Color.civitSurfaceContainer)
            .cornerRadius(CornerRadius.card)
            .shadow(color: .black.opacity(0.1), radius: 2, y: 1)
        }
        .buttonStyle(.plain)
    }
}
