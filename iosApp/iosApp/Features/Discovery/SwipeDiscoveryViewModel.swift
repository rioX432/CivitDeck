import Foundation
import Shared

@MainActor
final class SwipeDiscoveryViewModelOwner: ObservableObject {
    let vm: SwipeDiscoveryViewModel
    private let store = ViewModelStore()

    @Published var cards: [Model] = []
    @Published var isLoading: Bool = false
    @Published var error: String?
    @Published var hasLastDismissed: Bool = false

    init() {
        vm = KoinHelper.shared.createSwipeDiscoveryViewModel()
        store.put(key: "SwipeDiscoveryViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeState() async {
        for await state in vm.state {
            cards = state.cards as? [Model] ?? []
            isLoading = state.isLoading
            error = state.error
            hasLastDismissed = state.lastDismissed != nil
        }
    }

    func loadModels() {
        vm.loadModels()
    }

    func onSwipeRight(_ model: Model) {
        vm.onSwipeRight(model: model)
    }

    func onSwipeLeft(_ model: Model) {
        vm.onSwipeLeft(model: model)
    }

    func onSwipeUp(_ model: Model) -> Int64 {
        return vm.onSwipeUp(model: model)
    }

    func undoLastSwipe() {
        vm.undoLastSwipe()
    }

    func prefetchUpcomingImages() {
        let upcoming = cards.dropFirst(1).prefix(5)
        let urls: [URL] = upcoming.compactMap { model in
            guard let urlString = model.modelVersions.first?.images.first.flatMap({ $0.thumbnailUrl(width: 450) }),
                  let url = URL(string: urlString) else { return nil }
            return url
        }
        ImagePrefetcher.prefetch(urls: urls)
    }
}
