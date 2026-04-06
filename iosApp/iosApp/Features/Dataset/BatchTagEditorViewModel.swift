import Foundation
import Shared

@MainActor
final class BatchTagEditorViewModelOwner: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var tagInput = ""
    @Published var suggestions: [String] = []
    @Published var isAddMode = true

    let datasetId: Int64

    private let vm: BatchTagEditorViewModel
    private let store: ViewModelStore

    init(datasetId: Int64) {
        self.datasetId = datasetId
        store = ViewModelStore()
        vm = KoinHelper.shared.createBatchTagEditorViewModel(datasetId: datasetId)
        store.put(key: "BatchTagEditorViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeImages() async {
        for await list in vm.images {
            images = list as? [DatasetImage] ?? []
        }
    }

    func observeSelectionState() async {
        for await ids in vm.selectedImageIds {
            let longIds = ids as? Set<KotlinLong> ?? []
            selectedImageIds = Set(longIds.map { $0.int64Value })
        }
    }

    func observeTagInput() async {
        for await text in vm.tagInput {
            guard let str = text as? String else { continue }
            tagInput = str
        }
    }

    func observeSuggestions() async {
        for await list in vm.tagSuggestions {
            suggestions = list as? [String] ?? []
        }
    }

    func observeMode() async {
        for await mode in vm.isAddMode {
            guard let boolValue = mode as? Bool else { continue }
            isAddMode = boolValue
        }
    }

    func toggleSelection(_ id: Int64) { vm.toggleSelection(imageId: id) }
    func selectAll() { vm.selectAll() }
    func clearSelection() { vm.clearSelection() }
    func toggleMode() { vm.toggleMode() }

    func updateTagInput(_ text: String) {
        vm.setTagInput(text: text)
    }

    func applyTag(_ tag: String) {
        vm.applyTags(tags: [tag])
    }
}
