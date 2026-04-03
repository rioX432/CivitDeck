import SwiftUI
import Shared

struct NotificationCenterView: View {
    @StateObject private var viewModel = NotificationCenterViewModelOwner()
    @Environment(\.civitTheme) private var theme

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingStateView()
            } else if viewModel.notifications.isEmpty {
                EmptyStateView(
                    icon: "bell.slash",
                    title: "No notifications",
                    subtitle: "Model update notifications will appear here."
                )
            } else {
                notificationList
            }
        }
        .task { await viewModel.observeUiState() }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if viewModel.notifications.contains(where: { !$0.isRead }) {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.markAllRead()
                    } label: {
                        Image(systemName: "checkmark.circle")
                    }
                }
            }
        }
        .navigationDestination(for: Int64.self) { modelId in
            ModelDetailScreen(modelId: modelId)
        }
    }

    private var notificationList: some View {
        List(viewModel.notifications, id: \.id) { notification in
            NotificationRow(notification: notification) {
                viewModel.markRead(notificationId: notification.id)
            }
        }
    }
}

private struct NotificationRow: View {
    let notification: ModelUpdateNotification
    let onTap: () -> Void
    @Environment(\.civitTheme) private var theme

    var body: some View {
        NavigationLink(value: notification.modelId) {
            HStack(spacing: Spacing.sm) {
                if !notification.isRead {
                    Circle()
                        .fill(theme.primary)
                        .frame(width: 8, height: 8)
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 8, height: 8)
                }

                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(notification.modelName)
                        .font(.civitBodyMedium)
                        .fontWeight(notification.isRead ? .regular : .semibold)
                        .foregroundColor(.civitOnSurface)

                    Text("New version: \(notification.newVersionName)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)

                    Text(sourceLabel(notification.source))
                        .font(.civitLabelSmall)
                        .foregroundColor(theme.primary.opacity(0.7))
                }
            }
        }
        .simultaneousGesture(TapGesture().onEnded { onTap() })
    }

    private func sourceLabel(_ source: UpdateSource) -> String {
        switch source {
        case .favorite:
            return "Favorite"
        case .followed:
            return "Followed Creator"
        default:
            return "Unknown"
        }
    }
}
