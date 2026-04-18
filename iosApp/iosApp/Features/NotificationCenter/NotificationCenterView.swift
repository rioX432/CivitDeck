import SwiftUI
import Shared

/// Size of the unread indicator dot.
// TODO: Unify with shared design token
private let unreadDotSize: CGFloat = 8

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
                            .accessibilityLabel("Mark all as read")
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
                        .frame(width: unreadDotSize, height: unreadDotSize)
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: unreadDotSize, height: unreadDotSize)
                }

                VStack(alignment: .leading, spacing: Spacing.xxs) {
                    Text(notification.modelName)
                        .font(.civitBodyMedium)
                        .fontWeight(notification.isRead ? .regular : .semibold)
                        .foregroundColor(.civitOnSurface)

                    Text("New version: \(notification.newVersionName)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)

                    HStack(spacing: Spacing.sm) {
                        Text(sourceLabel(notification.source))
                            .font(.civitLabelSmall)
                            .foregroundColor(theme.primary.opacity(0.7))
                        Text(relativeTimeLabel(notification.createdAt))
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
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

    private func relativeTimeLabel(_ epochMs: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000.0)
        let diff = Date().timeIntervalSince(date)
        let minutes = Int(diff / 60)
        let hours = Int(diff / 3600)
        let days = Int(diff / 86400)
        switch true {
        case minutes < 1:
            return "Just now"
        case minutes < 60:
            return "\(minutes)m ago"
        case hours < 24:
            return "\(hours)h ago"
        case days < 2:
            return "Yesterday"
        case days < 30:
            return "\(days)d ago"
        default:
            return "\(days / 30)mo ago"
        }
    }
}
