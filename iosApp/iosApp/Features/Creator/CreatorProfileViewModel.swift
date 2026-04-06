import Foundation
import Shared

@MainActor
final class CreatorProfileViewModelOwner: ObservableObject {
    @Published var models: [Model] = []
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var isRefreshing: Bool = false
    @Published var isFollowing: Bool = false
    @Published var error: String?
    @Published var hasMore: Bool = true

    let username: String
    let vm: Feature_creatorCreatorProfileViewModel
    private let store: ViewModelStore

    init(username: String) {
        self.username = username
        store = ViewModelStore()
        vm = KoinHelper.shared.createCreatorProfileViewModel(username: username)
        store.put(key: "CreatorProfileViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            self.models = state.models as? [Model] ?? []
            self.isLoading = state.isLoading
            self.isLoadingMore = state.isLoadingMore
            self.isRefreshing = state.isRefreshing
            self.isFollowing = state.isFollowing
            self.error = state.error
            self.hasMore = state.hasMore
        }
    }

    func loadMore() { vm.loadMore() }

    func refresh() { vm.refresh() }

    func toggleFollow() { vm.toggleFollow() }
}
