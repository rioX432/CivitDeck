import Foundation
import Shared

@MainActor
final class AnalyticsViewModel: ObservableObject {
    @Published var isLoading = true
    @Published var totalViews: Int = 0
    @Published var totalFavorites: Int = 0
    @Published var totalSearches: Int = 0
    @Published var dailyViewCounts: [(day: Date, count: Int)] = []
    @Published var topModelTypes: [(name: String, count: Int)] = []
    @Published var topCreators: [(name: String, count: Int)] = []
    @Published var topSearchQueries: [(name: String, count: Int)] = []
    @Published var averageViewDurationMs: Int64?
    @Published var errorMessage: String?

    private let getBrowsingStatsUseCase: GetBrowsingStatsUseCase

    init() {
        self.getBrowsingStatsUseCase = KoinHelper.shared.getBrowsingStatsUseCase()
    }

    func loadStats() async {
        isLoading = true
        errorMessage = nil
        do {
            let stats = try await getBrowsingStatsUseCase.invoke()
            totalViews = Int(stats.totalViews)
            totalFavorites = Int(stats.totalFavorites)
            totalSearches = Int(stats.totalSearches)
            dailyViewCounts = stats.dailyViewCounts.map { item in
                let date = Date(timeIntervalSince1970: Double(item.dayTimestamp) / 1000.0)
                return (day: date, count: Int(item.count))
            }
            topModelTypes = stats.topModelTypes.map { (name: $0.name, count: Int($0.count)) }
            topCreators = stats.topCreators.map { (name: $0.name, count: Int($0.count)) }
            topSearchQueries = stats.topSearchQueries.map { (name: $0.name, count: Int($0.count)) }
            averageViewDurationMs = stats.averageViewDurationMs?.int64Value
            isLoading = false
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
        }
    }
}
