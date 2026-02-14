import Foundation
import Shared

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var defaultSortOrder: CivitSortOrder = .mostDownloaded
    @Published var defaultTimePeriod: TimePeriod = .allTime
    @Published var gridColumns: Int32 = 2
    @Published var hiddenModels: [HiddenModelEntity] = []
    @Published var excludedTags: [String] = []

    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let setNsfwFilterUseCase: SetNsfwFilterUseCase
    private let observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase
    private let setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase
    private let observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase
    private let setDefaultTimePeriodUseCase: SetDefaultTimePeriodUseCase
    private let observeGridColumnsUseCase: ObserveGridColumnsUseCase
    private let setGridColumnsUseCase: SetGridColumnsUseCase
    private let getHiddenModelsUseCase: GetHiddenModelsUseCase
    private let unhideModelUseCase: UnhideModelUseCase
    private let getExcludedTagsUseCase: GetExcludedTagsUseCase
    private let addExcludedTagUseCase: AddExcludedTagUseCase
    private let removeExcludedTagUseCase: RemoveExcludedTagUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private let clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase
    private let clearCacheUseCase: ClearCacheUseCase

    init() {
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.setNsfwFilterUseCase = KoinHelper.shared.getSetNsfwFilterUseCase()
        self.observeDefaultSortOrderUseCase = KoinHelper.shared.getObserveDefaultSortOrderUseCase()
        self.setDefaultSortOrderUseCase = KoinHelper.shared.getSetDefaultSortOrderUseCase()
        self.observeDefaultTimePeriodUseCase = KoinHelper.shared.getObserveDefaultTimePeriodUseCase()
        self.setDefaultTimePeriodUseCase = KoinHelper.shared.getSetDefaultTimePeriodUseCase()
        self.observeGridColumnsUseCase = KoinHelper.shared.getObserveGridColumnsUseCase()
        self.setGridColumnsUseCase = KoinHelper.shared.getSetGridColumnsUseCase()
        self.getHiddenModelsUseCase = KoinHelper.shared.getHiddenModelsUseCase()
        self.unhideModelUseCase = KoinHelper.shared.getUnhideModelUseCase()
        self.getExcludedTagsUseCase = KoinHelper.shared.getExcludedTagsUseCase()
        self.addExcludedTagUseCase = KoinHelper.shared.getAddExcludedTagUseCase()
        self.removeExcludedTagUseCase = KoinHelper.shared.getRemoveExcludedTagUseCase()
        self.clearSearchHistoryUseCase = KoinHelper.shared.getClearSearchHistoryUseCase()
        self.clearBrowsingHistoryUseCase = KoinHelper.shared.getClearBrowsingHistoryUseCase()
        self.clearCacheUseCase = KoinHelper.shared.getClearCacheUseCase()
        loadMutableData()
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            nsfwFilterLevel = value
        }
    }

    func observeSortOrder() async {
        for await value in observeDefaultSortOrderUseCase.invoke() {
            defaultSortOrder = value
        }
    }

    func observeTimePeriod() async {
        for await value in observeDefaultTimePeriodUseCase.invoke() {
            defaultTimePeriod = value
        }
    }

    func observeGridColumns() async {
        for await value in observeGridColumnsUseCase.invoke() {
            gridColumns = value.int32Value
        }
    }

    func onNsfwFilterToggle() {
        let newLevel: NsfwFilterLevel = nsfwFilterLevel == .off ? .all : .off
        nsfwFilterLevel = newLevel
        Task { try? await setNsfwFilterUseCase.invoke(level: newLevel) }
    }

    func onSortOrderChanged(_ sort: CivitSortOrder) {
        Task { try? await setDefaultSortOrderUseCase.invoke(sort: sort) }
    }

    func onTimePeriodChanged(_ period: TimePeriod) {
        Task { try? await setDefaultTimePeriodUseCase.invoke(period: period) }
    }

    func onGridColumnsChanged(_ columns: Int32) {
        Task { try? await setGridColumnsUseCase.invoke(columns: columns) }
    }

    func onUnhideModel(_ modelId: Int64) {
        Task {
            try? await unhideModelUseCase.invoke(modelId: modelId)
            loadMutableData()
        }
    }

    func onAddExcludedTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else { return }
        Task {
            try? await addExcludedTagUseCase.invoke(tag: trimmed)
            loadMutableData()
        }
    }

    func onRemoveExcludedTag(_ tag: String) {
        Task {
            try? await removeExcludedTagUseCase.invoke(tag: tag)
            loadMutableData()
        }
    }

    func onClearSearchHistory() {
        Task { try? await clearSearchHistoryUseCase.invoke() }
    }

    func onClearBrowsingHistory() {
        Task { try? await clearBrowsingHistoryUseCase.invoke() }
    }

    func onClearCache() {
        Task { try? await clearCacheUseCase.invoke() }
    }

    private func loadMutableData() {
        Task {
            hiddenModels = (try? await getHiddenModelsUseCase.invoke()) ?? []
            excludedTags = (try? await getExcludedTagsUseCase.invoke()) ?? []
        }
    }
}
