import Foundation
import Shared

@MainActor
final class DatasetListViewModel: ObservableObject {
    @Published var datasets: [DatasetCollection] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    private let observeCollectionsUseCase: ObserveDatasetCollectionsUseCase
    private let createUseCase: CreateDatasetCollectionUseCase
    private let renameUseCase: RenameDatasetCollectionUseCase
    private let deleteUseCase: DeleteDatasetCollectionUseCase
    private var observeTask: Task<Void, Never>?

    init() {
        self.observeCollectionsUseCase = KoinHelper.shared.getObserveDatasetCollectionsUseCase()
        self.createUseCase = KoinHelper.shared.getCreateDatasetCollectionUseCase()
        self.renameUseCase = KoinHelper.shared.getRenameDatasetCollectionUseCase()
        self.deleteUseCase = KoinHelper.shared.getDeleteDatasetCollectionUseCase()
    }

    deinit {
        observeTask?.cancel()
    }

    func startObserving() {
        observeTask?.cancel()
        observeTask = Task { await observe() }
    }

    func stopObserving() {
        observeTask?.cancel()
        observeTask = nil
    }

    private func observe() async {
        for await list in observeCollectionsUseCase.invoke() {
            let items = list.compactMap { $0 as? DatasetCollection }
            self.datasets = items
        }
    }

    func createDataset(name: String) {
        Task {
            do {
                try await createUseCase.invoke(name: name, description: "")
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func renameDataset(id: Int64, name: String) {
        Task {
            do {
                try await renameUseCase.invoke(id: id, name: name)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func deleteDataset(id: Int64) {
        Task {
            do {
                try await deleteUseCase.invoke(id: id)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}
