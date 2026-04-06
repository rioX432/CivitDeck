import Foundation
import Shared

@MainActor
final class ExternalServerGalleryViewModelOwner: ObservableObject {
    let vm: Feature_externalserverExternalServerGalleryViewModel
    private let store = ViewModelStore()

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

    init() {
        vm = KoinHelper.shared.createExternalServerGalleryViewModel()
        store.put(key: "ExternalServerGalleryViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            images = state.images as? [ServerImage] ?? []
            isLoading = state.isLoading
            isLoadingMore = state.isLoadingMore
            isRefreshing = state.isRefreshing
            error = state.error
            supportsFilters = state.supportsFilters
            supportsGeneration = state.supportsGeneration
            supportsGenerationOptions = state.supportsGenerationOptions
            filters = state.filters
            showFilterSheet = state.showFilterSheet
            generationOptions = state.generationOptions as? [GenerationOption] ?? []
            generationParams = state.generationParams as? [String: String] ?? [:]
            // Convert dependent choices
            var choices: [String: [GenerationChoice]] = [:]
            for (key, value) in state.dependentChoices {
                if let k = key as? String, let v = value as? [GenerationChoice] {
                    choices[k] = v
                }
            }
            dependentChoices = choices
            isLoadingOptions = state.isLoadingOptions
            isSubmittingGeneration = state.isSubmittingGeneration
            activeJob = state.activeJob
            generationError = state.generationError
            showGenerationSheet = state.showGenerationSheet
            isSelectionMode = state.isSelectionMode
            selectedCloudKeys = Set((state.selectedCloudKeys as? Set<String>) ?? [])
            isDeleting = state.isDeleting
            deleteError = state.deleteError
        }
    }

    // Gallery
    func onLoadMore() { vm.onLoadMore() }
    func onRetry() { vm.onRetry() }
    func onRefresh() { vm.onRefresh() }
    func onShowFilterSheet() { vm.onShowFilterSheet() }
    func onDismissFilterSheet() { vm.onDismissFilterSheet() }
    func onSearchChanged(_ search: String) { vm.onSearchChanged(search: search) }
    func onSortChanged(_ sort: String) { vm.onSortChanged(sort: sort) }
    func onCharacterFilterChanged(_ character: String) { vm.onCharacterFilterChanged(character: character) }
    func onScenarioFilterChanged(_ scenario: String) { vm.onScenarioFilterChanged(scenario: scenario) }
    func onNsfwFilterChanged(_ nsfw: String) { vm.onNsfwFilterChanged(nsfw: nsfw) }
    func onResetFilters() { vm.onResetFilters() }

    // Generation
    func onShowGenerationSheet() { vm.onShowGenerationSheet() }
    func onDismissGenerationSheet() { vm.onDismissGenerationSheet() }
    func onGenerationParamChanged(key: String, value: String) {
        vm.onGenerationParamChanged(key: key, value: value)
    }
    func onSubmitGeneration() { vm.onSubmitGeneration() }
    func onDismissJobStatus() { vm.onDismissJobStatus() }

    // Selection
    func enterSelectionMode(cloudKey: String) { vm.onEnterSelectionMode(cloudKey: cloudKey) }
    func toggleSelection(cloudKey: String) { vm.onToggleSelection(cloudKey: cloudKey) }
    func selectAll() { vm.onSelectAll() }
    func exitSelectionMode() { vm.onExitSelectionMode() }
    func deleteSelected() { vm.onDeleteSelected() }
}
