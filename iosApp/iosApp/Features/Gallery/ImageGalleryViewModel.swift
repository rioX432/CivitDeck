import Foundation
import Shared

typealias CivitSortOrder = Shared.SortOrder
typealias CivitImage = Shared.Image

@MainActor
final class ImageGalleryViewModel: ObservableObject {
    @Published var images: [CivitImage] = []
    @Published var selectedSort: CivitSortOrder = .highestRated
    @Published var selectedPeriod: TimePeriod = .allTime
    @Published var showNsfw: Bool = false
    @Published var isLoading: Bool = false
    @Published var isLoadingMore: Bool = false
    @Published var error: String? = nil
    @Published var hasMore: Bool = true
    @Published var selectedImageIndex: Int? = nil

    private let modelVersionId: Int64
    private let getImagesUseCase: GetImagesUseCase
    private var nextCursor: String? = nil
    private var loadTask: Task<Void, Never>? = nil

    private let pageSize: Int32 = 20
    private static let timeoutSeconds: UInt64 = 30

    init(modelVersionId: Int64) {
        self.modelVersionId = modelVersionId
        self.getImagesUseCase = KoinHelper.shared.getImagesUseCase()
        loadImages()
    }

    func onSortSelected(_ sort: CivitSortOrder) {
        loadTask?.cancel()
        selectedSort = sort
        images = []
        nextCursor = nil
        hasMore = true
        loadImages()
    }

    func onPeriodSelected(_ period: TimePeriod) {
        loadTask?.cancel()
        selectedPeriod = period
        images = []
        nextCursor = nil
        hasMore = true
        loadImages()
    }

    func onNsfwToggle() {
        loadTask?.cancel()
        showNsfw.toggle()
        images = []
        nextCursor = nil
        hasMore = true
        loadImages()
    }

    func loadMore() {
        guard !isLoading, !isLoadingMore, hasMore else { return }
        loadImages(isLoadMore: true)
    }

    func onImageSelected(_ index: Int) {
        selectedImageIndex = index
    }

    func onDismissViewer() {
        selectedImageIndex = nil
    }

    func retry() {
        loadImages()
    }

    private func loadImages(isLoadMore: Bool = false) {
        loadTask?.cancel()
        loadTask = Task {
            if isLoadMore {
                isLoadingMore = true
            } else {
                isLoading = true
            }
            error = nil

            // Timeout guard: SKIE may not propagate Kotlin exceptions to Swift,
            // leaving `try await` hanging forever. This task sets an error state
            // if the API call doesn't complete in time.
            let timeoutTask = Task { @MainActor in
                try await Task.sleep(nanoseconds: Self.timeoutSeconds * 1_000_000_000)
                if self.isLoading || self.isLoadingMore {
                    self.error = "Request timed out. Please try again."
                    self.isLoading = false
                    self.isLoadingMore = false
                }
            }

            do {
                let nsfwLevel: NsfwLevel = showNsfw ? .soft : NsfwLevel.none
                let result = try await getImagesUseCase.invoke(
                    modelId: nil,
                    modelVersionId: KotlinLong(longLong: modelVersionId),
                    username: nil,
                    sort: selectedSort,
                    period: selectedPeriod,
                    nsfwLevel: nsfwLevel,
                    limit: KotlinInt(int: pageSize),
                    cursor: isLoadMore ? nextCursor : nil
                )
                timeoutTask.cancel()

                guard !Task.isCancelled else { return }
                // If timeout already fired, skip updating state
                guard error == nil else { return }

                let newImages = result.items.compactMap { $0 as? CivitImage }
                if isLoadMore {
                    images.append(contentsOf: newImages)
                } else {
                    images = newImages
                }
                nextCursor = result.metadata.nextCursor
                hasMore = result.metadata.nextCursor != nil
                isLoading = false
                isLoadingMore = false
            } catch is CancellationError {
                timeoutTask.cancel()
                return
            } catch {
                timeoutTask.cancel()
                guard !Task.isCancelled else { return }
                self.error = error.localizedDescription
                isLoading = false
                isLoadingMore = false
            }
        }
    }
}
