import Foundation
import Shared

@MainActor
final class FeedViewModelOwner: ObservableObject {
    @Published var feedItems: [FeedItem] = []
    @Published var isLoading = true
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var unreadCount: Int32 = 0

    private let vm: FeedViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createFeedViewModel()
        store.put(key: "FeedViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            feedItems = state.feedItems as? [FeedItem] ?? []
            isLoading = state.isLoading
            isRefreshing = state.isRefreshing
            errorMessage = state.error
            unreadCount = state.unreadCount
        }
    }

    func refresh() { vm.refresh() }
    func markAsRead() { vm.markAsRead() }
}
