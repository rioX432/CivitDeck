import Foundation
import Shared

@MainActor
final class CollectionsViewModelOwner: ObservableObject {
    @Published var collections: [ModelCollection] = []
    @Published var isLoading = true

    let vm: Feature_collectionsCollectionsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createCollectionsViewModel()
        store.put(key: "CollectionsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeCollections() async {
        for await list in vm.collections {
            let items = list.compactMap { $0 as? ModelCollection }
            self.collections = items
            if isLoading { isLoading = false }
        }
    }

    func createCollection(name: String) {
        vm.createCollection(name: name)
    }

    func renameCollection(id: Int64, name: String) {
        vm.renameCollection(id: id, name: name)
    }

    func deleteCollection(id: Int64) {
        vm.deleteCollection(id: id)
    }
}
