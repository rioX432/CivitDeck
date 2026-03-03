import Foundation
import Shared

@MainActor
final class DuplicateReviewViewModel: ObservableObject {
    @Published var groups: [[DatasetImage]] = []

    private let detectDuplicatesUseCase: DetectDuplicatesUseCase
    private let markImageExcludedUseCase: MarkImageExcludedUseCase
    private var observeTask: Task<Void, Never>?

    init(datasetId: Int64) {
        self.detectDuplicatesUseCase = KoinHelper.shared.getDetectDuplicatesUseCase()
        self.markImageExcludedUseCase = KoinHelper.shared.getMarkImageExcludedUseCase()
        observeTask = Task { await self.observeGroups(datasetId: datasetId) }
    }

    deinit {
        observeTask?.cancel()
    }

    private func observeGroups(datasetId: Int64) async {
        for await result in detectDuplicatesUseCase.invoke(datasetId: datasetId, threshold: 10) {
            let groupList = result.compactMap { $0 as? DuplicateGroup }
            groups = groupList.map { group in
                group.images.compactMap { $0 as? DatasetImage }
            }
        }
    }

    func keepImage(_ id: Int64) {
        Task {
            try? await markImageExcludedUseCase.invoke(imageId: id, excluded: false)
        }
    }

    func removeImage(_ id: Int64) {
        Task {
            try? await markImageExcludedUseCase.invoke(imageId: id, excluded: true)
        }
    }
}
