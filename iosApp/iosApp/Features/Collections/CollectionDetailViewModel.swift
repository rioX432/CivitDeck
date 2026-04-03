import Foundation
import Shared

@MainActor
final class CollectionDetailViewModelOwner: ObservableObject {
    @Published var displayModels: [FavoriteModelSummary] = []
    @Published var collections: [ModelCollection] = []
    @Published var sortOrder: Core_domainCollectionSortOrder = .dateAdded {
        didSet { vm.updateSortOrder(order: sortOrder) }
    }
    @Published var typeFilter: Core_domainModelType? {
        didSet { vm.updateTypeFilter(type: typeFilter) }
    }
    @Published var isSelectionMode = false
    @Published var selectedIds: Set<Int64> = []

    let collectionId: Int64
    let vm: Feature_collectionsCollectionDetailViewModel
    private let store: ViewModelStore

    init(collectionId: Int64) {
        self.collectionId = collectionId
        store = ViewModelStore()
        vm = KoinHelper.shared.createCollectionDetailViewModel(collectionId: collectionId)
        store.put(key: "CollectionDetailViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func startObserving() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.observeDisplayModels() }
            group.addTask { await self.observeCollections() }
            group.addTask { await self.observeSelectedIds() }
            group.addTask { await self.observeSelectionMode() }
        }
    }

    private func observeDisplayModels() async {
        for await list in vm.displayModels {
            let items = list.compactMap { $0 as? FavoriteModelSummary }
            self.displayModels = items
        }
    }

    private func observeCollections() async {
        for await list in vm.collections {
            let items = list.compactMap { $0 as? ModelCollection }
            self.collections = items
        }
    }

    private func observeSelectedIds() async {
        for await ids in vm.selectedModelIds {
            let set = ids.compactMap { ($0 as? NSNumber)?.int64Value }
            self.selectedIds = Set(set)
        }
    }

    private func observeSelectionMode() async {
        for await mode in vm.isSelectionMode {
            if let boolValue = mode as? Bool {
                self.isSelectionMode = boolValue
            }
        }
    }

    func toggleSelection(_ modelId: Int64) {
        vm.toggleSelection(modelId: modelId)
    }

    func selectAll() { vm.selectAll() }

    func clearSelection() { vm.clearSelection() }

    func enterSelectionMode(_ modelId: Int64) {
        vm.enterSelectionMode(modelId: modelId)
    }

    func removeSelected() { vm.removeSelected() }

    func moveSelectedTo(_ targetId: Int64) {
        vm.moveSelectedTo(targetId: targetId)
    }
}

// swiftlint:disable:next type_name
typealias ModelType_ = Core_domainModelType

extension CollectionDetailViewModelOwner {
    static let allModelTypes: [ModelType_] = Core_domainModelType.allCases
}
