import Foundation
import Shared

@MainActor
final class CollectionsViewModel: ObservableObject {
    @Published var collections: [ModelCollection] = []

    private let observeCollectionsUseCase: ObserveCollectionsUseCase
    private let createCollectionUseCase: CreateCollectionUseCase
    private let renameCollectionUseCase: RenameCollectionUseCase
    private let deleteCollectionUseCase: DeleteCollectionUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.observeCollectionsUseCase = KoinHelper.shared.getObserveCollectionsUseCase()
        self.createCollectionUseCase = KoinHelper.shared.getCreateCollectionUseCase()
        self.renameCollectionUseCase = KoinHelper.shared.getRenameCollectionUseCase()
        self.deleteCollectionUseCase = KoinHelper.shared.getDeleteCollectionUseCase()
        observeTask = Task { await observe() }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observe() async {
        for await list in observeCollectionsUseCase.invoke() {
            let items = list.compactMap { $0 as? ModelCollection }
            self.collections = items
        }
    }

    func createCollection(name: String) {
        Task {
            try await createCollectionUseCase.invoke(name: name)
        }
    }

    func renameCollection(id: Int64, name: String) {
        Task {
            try await renameCollectionUseCase.invoke(id: id, name: name)
        }
    }

    func deleteCollection(id: Int64) {
        Task {
            try await deleteCollectionUseCase.invoke(id: id)
        }
    }
}
