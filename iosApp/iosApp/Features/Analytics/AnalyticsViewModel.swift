import Foundation
import Shared

@MainActor
final class AnalyticsViewModelOwner: ObservableObject {
    @Published var isLoading = true
    @Published var totalViews: Int = 0
    @Published var totalFavorites: Int = 0
    @Published var totalSearches: Int = 0
    @Published var averageViewDurationMs: Int64?
    @Published var dailyViewCounts: [(day: Date, count: Int)] = []
    @Published var topModelTypes: [(name: String, count: Int)] = []
    @Published var topCreators: [(name: String, count: Int)] = []
    @Published var topSearchQueries: [(name: String, count: Int)] = []
    @Published var error: String?

    private let vm: AnalyticsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createAnalyticsViewModel()
        store.put(key: "AnalyticsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            isLoading = state.isLoading
            totalViews = Int(state.totalViews)
            totalFavorites = Int(state.totalFavorites)
            totalSearches = Int(state.totalSearches)
            averageViewDurationMs = state.averageViewDurationMs?.int64Value
            let rawDays = state.dailyViewCounts as? [DailyViewCount] ?? []
            dailyViewCounts = rawDays.map { item in
                let date = Date(timeIntervalSince1970: Double(item.dayTimestamp) / 1000.0)
                return (day: date, count: Int(item.count))
            }
            let rawTypes = state.topModelTypes as? [CategoryStat] ?? []
            topModelTypes = rawTypes.map { (name: $0.name, count: Int($0.count)) }
            let rawCreators = state.topCreators as? [CategoryStat] ?? []
            topCreators = rawCreators.map { (name: $0.name, count: Int($0.count)) }
            let rawQueries = state.topSearchQueries as? [CategoryStat] ?? []
            topSearchQueries = rawQueries.map { (name: $0.name, count: Int($0.count)) }
            error = state.error
        }
    }

    func refresh() { vm.refresh() }
}
