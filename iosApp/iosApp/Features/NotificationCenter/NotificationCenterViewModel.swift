import Foundation
import Shared

@MainActor
final class NotificationCenterViewModel: ObservableObject {
    @Published var notifications: [ModelUpdateNotification] = []
    @Published var isLoading = true

    private let getNotificationsUseCase: GetModelUpdateNotificationsUseCase
    private let markReadUseCase: MarkNotificationReadUseCase
    private let markAllReadUseCase: MarkAllNotificationsReadUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.getNotificationsUseCase = KoinHelper.shared.getModelUpdateNotificationsUseCase()
        self.markReadUseCase = KoinHelper.shared.getMarkNotificationReadUseCase()
        self.markAllReadUseCase = KoinHelper.shared.getMarkAllNotificationsReadUseCase()
        observeNotifications()
    }

    func markRead(notificationId: Int64) {
        Task {
            try? await markReadUseCase.invoke(notificationId: notificationId)
        }
    }

    func markAllRead() {
        Task {
            try? await markAllReadUseCase.invoke()
        }
    }

    private func observeNotifications() {
        observeTask = Task {
            for await items in getNotificationsUseCase.invoke() {
                guard !Task.isCancelled else { return }
                if let list = items as? [ModelUpdateNotification] {
                    self.notifications = list
                    self.isLoading = false
                }
            }
        }
    }
}
