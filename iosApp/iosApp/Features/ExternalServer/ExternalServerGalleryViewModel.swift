import Foundation
import Shared

@MainActor
final class ExternalServerGalleryViewModel: ObservableObject {
    private static let pollIntervalNanoseconds: UInt64 = 2_000_000_000
    @Published var images: [ServerImage] = []
    @Published var isLoading = false
    @Published var isLoadingMore = false
    @Published var isRefreshing = false
    @Published var error: String?

    // Capabilities
    @Published var supportsFilters = false
    @Published var supportsGeneration = false
    @Published var supportsGenerationOptions = false

    // Filters
    @Published var filters = ExternalServerImageFilters(
        character: "", scenario: "", nsfw: "", status: "", sort: "newest", search: ""
    )
    @Published var showFilterSheet = false

    // Generation
    @Published var generationOptions: [GenerationOption] = []
    @Published var generationParams: [String: String] = [:]
    @Published var dependentChoices: [String: [GenerationChoice]] = [:]
    @Published var isLoadingOptions = false
    @Published var isSubmittingGeneration = false
    @Published var activeJob: GenerationJob?
    @Published var generationError: String?
    @Published var showGenerationSheet = false

    // Selection mode
    @Published var isSelectionMode = false
    @Published var selectedCloudKeys: Set<String> = []
    @Published var isDeleting = false
    @Published var deleteError: String?

    private var currentPage = 1
    private var totalPages = 1
    private let perPage: Int32 = 96
    private var pollTask: Task<Void, Never>?

    private let getImages: GetExternalServerImagesUseCase
    private let getCapabilities: GetExternalServerCapabilitiesUseCase
    private let getGenerationOptionsUC: GetGenerationOptionsUseCase
    private let getDependentChoicesUC: GetDependentChoicesUseCase
    private let executeGenerationUC: ExecuteGenerationUseCase
    private let getGenerationStatusUC: GetGenerationStatusUseCase
    private let deleteServerImagesUC: DeleteServerImagesUseCase

    init() {
        self.getImages = KoinHelper.shared.getGetExternalServerImagesUseCase()
        self.getCapabilities = KoinHelper.shared.getGetExternalServerCapabilitiesUseCase()
        self.getGenerationOptionsUC = KoinHelper.shared.getGetGenerationOptionsUseCase()
        self.getDependentChoicesUC = KoinHelper.shared.getGetDependentChoicesUseCase()
        self.executeGenerationUC = KoinHelper.shared.getExecuteGenerationUseCase()
        self.getGenerationStatusUC = KoinHelper.shared.getGetGenerationStatusUseCase()
        self.deleteServerImagesUC = KoinHelper.shared.getDeleteServerImagesUseCase()
    }

    // MARK: - Capabilities

    func loadCapabilities() async {
        do {
            let caps = try await getCapabilities.invoke()
            supportsFilters = caps.supports(endpoint: "images.filters")
            supportsGeneration = caps.supports(endpoint: "generation")
            supportsGenerationOptions = caps.supports(endpoint: "generation.options")
        } catch {
            // Capabilities are optional; silently continue with defaults
        }
    }

    // MARK: - Gallery

    func loadFirstPage() async {
        isLoading = true
        error = nil
        currentPage = 1
        do {
            let response = try await getImages.invoke(page: 1, perPage: perPage, filters: filters)
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
                page: Int32(currentPage + 1), perPage: perPage, filters: filters
            )
            images.append(contentsOf: response.images.compactMap { $0 as? ServerImage })
            currentPage = Int(response.page)
            totalPages = Int(response.totalPages)
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingMore = false
    }

    func refresh() async {
        isRefreshing = true
        error = nil
        currentPage = 1
        do {
            let response = try await getImages.invoke(page: 1, perPage: perPage, filters: filters)
            images = response.images.compactMap { $0 as? ServerImage }
            currentPage = Int(response.page)
            totalPages = Int(response.totalPages)
        } catch {
            self.error = error.localizedDescription
        }
        isRefreshing = false
    }

    func applyFilters(_ newFilters: ExternalServerImageFilters) {
        filters = newFilters
        images = []
        Task { await loadFirstPage() }
    }

    func resetFilters() {
        applyFilters(ExternalServerImageFilters(
            character: "", scenario: "", nsfw: "", status: "", sort: "newest", search: ""
        ))
    }

    // MARK: - Generation

    func loadGenerationOptions() async {
        isLoadingOptions = true
        generationError = nil
        do {
            let options = try await getGenerationOptionsUC.invoke()
            generationOptions = options.compactMap { $0 as? GenerationOption }
            // Set defaults
            var defaults: [String: String] = [:]
            for option in generationOptions {
                if let defaultVal = option.defaultValue {
                    defaults[option.key] = defaultVal
                }
            }
            generationParams = defaults
        } catch {
            generationError = error.localizedDescription
        }
        isLoadingOptions = false
    }

    func updateGenerationParam(key: String, value: String) {
        generationParams[key] = value
        // Check for dependent fields
        let dependents = generationOptions.filter { $0.dependsOn == key }
        for option in dependents {
            if let endpoint = option.choicesEndpoint {
                let resolvedEndpoint = endpoint.replacingOccurrences(of: "{\(key)}", with: value)
                Task { await loadDependentChoices(key: option.key, endpoint: resolvedEndpoint) }
            }
        }
    }

    func submitGeneration() async {
        isSubmittingGeneration = true
        generationError = nil
        do {
            let job = try await executeGenerationUC.invoke(params: generationParams)
            activeJob = job
            isSubmittingGeneration = false
            showGenerationSheet = false
            startPolling(jobId: job.jobId)
        } catch {
            generationError = error.localizedDescription
            isSubmittingGeneration = false
        }
    }

    func dismissJobStatus() {
        pollTask?.cancel()
        activeJob = nil
    }

    private func loadDependentChoices(key: String, endpoint: String) async {
        do {
            let choices = try await getDependentChoicesUC.invoke(endpoint: endpoint)
            dependentChoices[key] = choices.compactMap { $0 as? GenerationChoice }
        } catch {
            // Silently fail for dependent choices
        }
    }

    // MARK: - Selection & Delete

    func enterSelectionMode(cloudKey: String) {
        isSelectionMode = true
        selectedCloudKeys = [cloudKey]
    }

    func toggleSelection(cloudKey: String) {
        if selectedCloudKeys.contains(cloudKey) {
            selectedCloudKeys.remove(cloudKey)
        } else {
            selectedCloudKeys.insert(cloudKey)
        }
        if selectedCloudKeys.isEmpty {
            isSelectionMode = false
        }
    }

    func selectAll() {
        selectedCloudKeys = Set(images.map { $0.cloudKey })
    }

    func exitSelectionMode() {
        isSelectionMode = false
        selectedCloudKeys = []
    }

    func deleteSelected() async {
        let keys = Array(selectedCloudKeys)
        guard !keys.isEmpty else { return }
        isDeleting = true
        deleteError = nil
        do {
            try await deleteServerImagesUC.invoke(cloudKeys: keys)
            images.removeAll { keys.contains($0.cloudKey) }
            isSelectionMode = false
            selectedCloudKeys = []
        } catch {
            deleteError = error.localizedDescription
        }
        isDeleting = false
    }

    private func startPolling(jobId: String) {
        pollTask?.cancel()
        pollTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: Self.pollIntervalNanoseconds)
                guard !Task.isCancelled else { return }
                do {
                    let job = try await getGenerationStatusUC.invoke(jobId: jobId)
                    activeJob = job
                    if job.status == .completed || job.status == .error {
                        if job.status == .completed {
                            await refresh()
                        }
                        return
                    }
                } catch {
                    return
                }
            }
        }
    }
}
