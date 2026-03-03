import Foundation
import Shared

@MainActor
final class BatchTagEditorViewModel: ObservableObject {
    @Published var images: [DatasetImage] = []
    @Published var selectedImageIds: Set<Int64> = []
    @Published var tagInput = ""
    @Published var suggestions: [String] = []
    @Published var isAddMode = true

    let datasetId: Int64

    private let observeImagesUseCase: ObserveDatasetImagesUseCase
    private let batchEditTagsUseCase: BatchEditTagsUseCase
    private let getTagSuggestionsUseCase: GetTagSuggestionsUseCase
    private var observeTask: Task<Void, Never>?

    init(datasetId: Int64) {
        self.datasetId = datasetId
        self.observeImagesUseCase = KoinHelper.shared.getObserveDatasetImagesUseCase()
        self.batchEditTagsUseCase = KoinHelper.shared.getBatchEditTagsUseCase()
        self.getTagSuggestionsUseCase = KoinHelper.shared.getGetTagSuggestionsUseCase()
        observeTask = Task { await observeImages() }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observeImages() async {
        for await list in observeImagesUseCase.invoke(datasetId: datasetId) {
            images = list.compactMap { $0 as? DatasetImage }
        }
    }

    func toggleSelection(_ id: Int64) {
        if selectedImageIds.contains(id) {
            selectedImageIds.remove(id)
        } else {
            selectedImageIds.insert(id)
        }
    }

    func selectAll() {
        selectedImageIds = Set(images.map { $0.id })
    }

    func clearSelection() {
        selectedImageIds.removeAll()
    }

    func toggleMode() {
        isAddMode.toggle()
    }

    func updateTagInput(_ text: String) {
        tagInput = text
        Task { await loadSuggestions(prefix: text) }
    }

    private func loadSuggestions(prefix: String) async {
        guard !prefix.isEmpty else {
            suggestions = []
            return
        }
        let result = try? await getTagSuggestionsUseCase.invoke(datasetId: datasetId, prefix: prefix)
        suggestions = result as? [String] ?? []
    }

    func applyTag(_ tag: String) {
        guard !selectedImageIds.isEmpty else { return }
        let ids = Array(selectedImageIds).map { KotlinLong(value: $0) }
        Task {
            if isAddMode {
                try? await batchEditTagsUseCase.invoke(
                    imageIds: ids,
                    addTags: [tag],
                    removeTags: []
                )
            } else {
                try? await batchEditTagsUseCase.invoke(
                    imageIds: ids,
                    addTags: [],
                    removeTags: [tag]
                )
            }
            tagInput = ""
            suggestions = []
        }
    }
}
