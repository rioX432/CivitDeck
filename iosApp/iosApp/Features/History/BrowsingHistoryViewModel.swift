import Foundation
import Shared

@MainActor
final class BrowsingHistoryViewModelOwner: ObservableObject {
    let vm: BrowsingHistoryViewModel
    private let store = ViewModelStore()

    @Published var groups: [HistoryDateGroup] = []
    @Published var isEmpty: Bool = true

    struct HistoryDateGroup: Identifiable {
        let id: String
        let label: String
        let items: [RecentlyViewedModel]
    }

    init() {
        vm = KoinHelper.shared.createBrowsingHistoryViewModel()
        store.put(key: "BrowsingHistoryViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            let rawGroups = state.groups as? [Feature_searchDateGroup] ?? []
            groups = rawGroups.map { group in
                HistoryDateGroup(
                    id: group.label.lowercased().replacingOccurrences(of: " ", with: "_"),
                    label: group.label,
                    items: group.items as? [RecentlyViewedModel] ?? []
                )
            }
            isEmpty = state.isEmpty
        }
    }

    func deleteItem(historyId: Int64) {
        vm.deleteItem(historyId: historyId)
    }

    func clearAll() {
        vm.clearAll()
    }
}
