import Foundation
import Shared

@MainActor
final class ExternalServerGalleryViewModel: ObservableObject {
    @Published var images: [ServerImage] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var error: String?

    private var currentPage = 1
    private var totalPages = 1
    private let perPage: Int32 = 96

    private let getImages: GetExternalServerImagesUseCase

    init() {
        self.getImages = KoinHelper.shared.getGetExternalServerImagesUseCase()
    }

    private func defaultFilters() -> ExternalServerImageFilters {
        ExternalServerImageFilters(
            character: "",
            scenario: "",
            nsfw: "",
            status: "",
            sort: "newest",
            search: ""
        )
    }

    func loadFirstPage() async {
        isLoading = true
        error = nil
        currentPage = 1
        do {
            let response = try await getImages.invoke(
                page: 1,
                perPage: perPage,
                filters: defaultFilters()
            )
            images = response.images.compactMap { $0 as? ServerImage }
            currentPage = Int(response.page)
            totalPages = Int(response.totalPages)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadMore() async {
        guard !isLoadingMore, currentPage < totalPages else { return }
        isLoadingMore = true
        do {
            let response = try await getImages.invoke(
                page: Int32(currentPage + 1),
                perPage: perPage,
                filters: defaultFilters()
            )
            images.append(contentsOf: response.images.compactMap { $0 as? ServerImage })
            currentPage = Int(response.page)
            totalPages = Int(response.totalPages)
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingMore = false
    }
}
