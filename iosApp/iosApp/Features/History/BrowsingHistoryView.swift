import SwiftUI
import Shared

struct BrowsingHistoryView: View {
    @StateObject private var viewModel = BrowsingHistoryViewModelOwner()
    @State private var showClearAlert = false

    var body: some View {
        Group {
            if viewModel.isEmpty {
                EmptyStateView(icon: "clock.arrow.circlepath", title: "No browsing history")
            } else {
                List {
                    ForEach(viewModel.groups) { group in
                        Section(group.label) {
                            ForEach(group.items, id: \.historyId) { item in
                                NavigationLink(value: item.modelId) {
                                    HistoryRow(item: item)
                                }
                                .swipeActions(edge: .trailing) {
                                    Button(role: .destructive) {
                                        viewModel.deleteItem(historyId: item.historyId)
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
                                }
                            }
                        }
                    }
                }
                .navigationDestination(for: Int64.self) { modelId in
                    ModelDetailScreen(modelId: modelId)
                }
            }
        }
        .navigationTitle("Browsing History")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !viewModel.isEmpty {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear All") { showClearAlert = true }
                }
            }
        }
        .alert("Clear All History", isPresented: $showClearAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Clear", role: .destructive) { viewModel.clearAll() }
        } message: {
            Text("Are you sure? This cannot be undone.")
        }
        .task { await viewModel.observeUiState() }
    }
}

private struct HistoryRow: View {
    let item: RecentlyViewedModel

    var body: some View {
        HStack(spacing: Spacing.md) {
            CachedAsyncImage(url: URL(string: item.thumbnailUrl ?? "")) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().aspectRatio(contentMode: .fill)
                default:
                    Color.civitSurfaceContainerHigh
                }
            }
            .frame(width: 48, height: 48)
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.small))

            VStack(alignment: .leading, spacing: Spacing.xxs) {
                Text(item.modelName)
                    .font(.civitBodyMedium)
                    .lineLimit(1)

                HStack(spacing: Spacing.sm) {
                    Text(item.modelType)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)

                    if let creator = item.creatorName {
                        Text(creator)
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                            .lineLimit(1)
                    }
                }
            }

            Spacer()

            Text(formatRelativeTime(item.viewedAt))
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    private func formatRelativeTime(_ timestamp: Int64) -> String {
        let diff = Int64(Date().timeIntervalSince1970 * 1000) - timestamp
        let minutes = diff / 60_000
        let hours = minutes / 60
        let days = hours / 24
        if minutes < 1 { return "now" }
        if minutes < 60 { return "\(minutes)m" }
        if hours < 24 { return "\(hours)h" }
        if days < 7 { return "\(days)d" }
        return "\(days / 7)w"
    }
}
