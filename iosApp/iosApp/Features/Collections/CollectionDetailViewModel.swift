import Foundation
import Shared

@MainActor
final class CollectionDetailViewModel: ObservableObject {
    @Published var models: [FavoriteModelSummary] = []
    @Published var sortOrder: CollectionSortOrder_ = .dateAdded
    @Published var typeFilter: ModelType_?
    @Published var isSelectionMode = false
    @Published var selectedIds: Set<Int64> = []
    @Published var collections: [ModelCollection] = []

    let collectionId: Int64

    private let observeCollectionModelsUseCase: ObserveCollectionModelsUseCase
    private let observeCollectionsUseCase: ObserveCollectionsUseCase
    private let bulkRemoveModelsUseCase: BulkRemoveModelsUseCase
    private let bulkMoveModelsUseCase: BulkMoveModelsUseCase
    private var observeTask: Task<Void, Never>?
    private var collectionsTask: Task<Void, Never>?

    init(collectionId: Int64) {
        self.collectionId = collectionId
        self.observeCollectionModelsUseCase = KoinHelper.shared.getObserveCollectionModelsUseCase()
        self.observeCollectionsUseCase = KoinHelper.shared.getObserveCollectionsUseCase()
        self.bulkRemoveModelsUseCase = KoinHelper.shared.getBulkRemoveModelsUseCase()
        self.bulkMoveModelsUseCase = KoinHelper.shared.getBulkMoveModelsUseCase()

        observeTask = Task { await observeModels() }
        collectionsTask = Task { await observeCollections() }
    }

    deinit {
        observeTask?.cancel()
        collectionsTask?.cancel()
    }

    var displayModels: [FavoriteModelSummary] {
        var result = models
        if let filter = typeFilter {
            result = result.filter { $0.type == filter }
        }
        return sortModels(result)
    }

    func toggleSelection(_ modelId: Int64) {
        if selectedIds.contains(modelId) {
            selectedIds.remove(modelId)
        } else {
            selectedIds.insert(modelId)
        }
        if selectedIds.isEmpty { isSelectionMode = false }
    }

    func selectAll() {
        selectedIds = Set(displayModels.map { $0.id })
    }

    func clearSelection() {
        selectedIds.removeAll()
        isSelectionMode = false
    }

    func enterSelectionMode(_ modelId: Int64) {
        isSelectionMode = true
        selectedIds = [modelId]
    }

    func removeSelected() {
        let ids = Array(selectedIds).map { KotlinLong(value: $0) }
        guard !ids.isEmpty else { return }
        Task {
            try await bulkRemoveModelsUseCase.invoke(collectionId: collectionId, modelIds: ids)
            clearSelection()
        }
    }

    func moveSelectedTo(_ targetId: Int64) {
        let ids = Array(selectedIds).map { KotlinLong(value: $0) }
        guard !ids.isEmpty else { return }
        Task {
            try await bulkMoveModelsUseCase.invoke(from: collectionId, to: targetId, modelIds: ids)
            clearSelection()
        }
    }

    private func observeModels() async {
        for await list in observeCollectionModelsUseCase.invoke(collectionId: collectionId) {
            let items = list.compactMap { $0 as? FavoriteModelSummary }
            self.models = items
        }
    }

    private func observeCollections() async {
        for await list in observeCollectionsUseCase.invoke() {
            let items = list.compactMap { $0 as? ModelCollection }
            self.collections = items
        }
    }

    private func sortModels(_ models: [FavoriteModelSummary]) -> [FavoriteModelSummary] {
        switch sortOrder {
        case .dateAdded:
            return models.sorted { $0.favoritedAt > $1.favoritedAt }
        case .rating:
            return models.sorted { $0.rating > $1.rating }
        case .type:
            return models.sorted { $0.type.name < $1.type.name }
        case .name:
            return models.sorted { $0.name.lowercased() < $1.name.lowercased() }
        }
    }
}

// swiftlint:disable:next type_name
enum CollectionSortOrder_: String, CaseIterable {
    case dateAdded = "Date Added"
    case rating = "Rating"
    case type = "Type"
    case name = "Name"
}

// swiftlint:disable:next type_name
typealias ModelType_ = Shared.ModelType

extension CollectionDetailViewModel {
    static let allModelTypes: [ModelType_] = [
        .checkpoint, .textualInversion, .hypernetwork, .aestheticGradient,
        .lora, .loCon, .controlnet, .upscaler, .motionModule,
        .vae, .poses, .wildcards, .workflows, .other,
    ]
}
