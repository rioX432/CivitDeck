import Foundation
import Shared

@MainActor
final class DatasetListViewModelOwner: ObservableObject {
    @Published var datasets: [DatasetCollection] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    private let vm: DatasetListViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createDatasetListViewModel()
        store.put(key: "DatasetListViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeDatasets() async {
        for await list in vm.datasets {
            datasets = list as? [DatasetCollection] ?? []
        }
    }

    func observeLoading() async {
        for await loading in vm.isLoading {
            guard let boolValue = loading as? Bool else { continue }
            isLoading = boolValue
        }
    }

    func createDataset(name: String) { vm.createDataset(name: name) }
    func renameDataset(id: Int64, name: String) { vm.renameDataset(id: id, name: name) }
    func deleteDataset(id: Int64) { vm.deleteDataset(id: id) }
}
