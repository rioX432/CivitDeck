import Foundation
import Shared

@MainActor
final class ShareHashtagViewModelOwner: ObservableObject {
    @Published var hashtags: [ShareHashtag] = []

    private let vm: ShareViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createShareViewModel()
        store.put(key: "ShareViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeHashtags() async {
        for await list in vm.hashtags {
            hashtags = list as? [ShareHashtag] ?? []
        }
    }

    func toggle(tag: String, isEnabled: Bool) {
        vm.onToggle(tag: tag, isEnabled: isEnabled)
    }

    func add(tag: String) {
        vm.onAdd(tag: tag)
    }

    func remove(tag: String) {
        vm.onRemove(tag: tag)
    }
}
