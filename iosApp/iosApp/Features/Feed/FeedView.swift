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
                    feedGrid
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

    private var feedGrid: some View {
        ScrollView {
            LazyVGrid(
                columns: AdaptiveGrid.columns(sizeClass: sizeClass),
                spacing: Spacing.sm
            ) {
                ForEach(viewModel.feedItems, id: \.modelId) { item in
                    FeedGridCard(item: item)
                }
            }
            .padding(.horizontal, Spacing.sm)
            .padding(.top, Spacing.sm)
        }
        .refreshable {
            await viewModel.loadFeed(forceRefresh: true)
        }
    }
}

private struct FeedGridCard: View {
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
                                .aspectRatio(3 / 4, contentMode: .fill)
                        case .failure:
                            Rectangle()
                                .fill(Color.civitSurfaceVariant.opacity(0.2))
                                .aspectRatio(3 / 4, contentMode: .fill)
                        default:
                            Rectangle()
                                .fill(Color.civitSurfaceVariant.opacity(0.2))
                                .aspectRatio(3 / 4, contentMode: .fill)
                                .modifier(ShimmerModifier())
                        }
                    }
                    .clipped()
                }

                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(item.title)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurface)
                        .lineLimit(2)

                    HStack(spacing: Spacing.xs) {
                        Text(item.creatorUsername)
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitPrimary)

                        Text(item.type.name)
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitOnSurfaceVariant)

                        if item.isUnread {
                            Circle()
                                .fill(Color.civitPrimary)
                                .frame(width: 6, height: 6)
                        }
                    }
                }
                .padding(Spacing.sm)
            }
            .background(Color.civitSurfaceContainer)
            .cornerRadius(CornerRadius.card)
            .shadow(color: .civitScrim.opacity(0.1), radius: 2, y: 1)
        }
        .buttonStyle(.plain)
    }
}
