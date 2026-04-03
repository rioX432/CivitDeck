import Foundation
import Shared

@MainActor
final class NotificationCenterViewModelOwner: ObservableObject {
    @Published var notifications: [ModelUpdateNotification] = []
    @Published var isLoading = true

    private let vm: NotificationCenterViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createNotificationCenterViewModel()
        store.put(key: "NotificationCenterViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            notifications = state.notifications as? [ModelUpdateNotification] ?? []
            isLoading = state.isLoading
        }
    }

    func markRead(notificationId: Int64) { vm.markRead(notificationId: notificationId) }
    func markAllRead() { vm.markAllRead() }
}
