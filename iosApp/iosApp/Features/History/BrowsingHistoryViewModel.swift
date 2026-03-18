import Foundation
import Shared

@MainActor
final class BrowsingHistoryViewModel: ObservableObject {
    @Published var groups: [DateGroup] = []
    @Published var isEmpty: Bool = true

    private let observeRecentlyViewedUseCase: ObserveRecentlyViewedUseCase
    private let deleteBrowsingHistoryItemUseCase: DeleteBrowsingHistoryItemUseCase
    private let clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase

    struct DateGroup: Identifiable {
        let id: String
        let label: String
        let items: [RecentlyViewedModel]
    }

    init() {
        self.observeRecentlyViewedUseCase = KoinHelper.shared.getObserveRecentlyViewedUseCase()
        self.deleteBrowsingHistoryItemUseCase = KoinHelper.shared.getDeleteBrowsingHistoryItemUseCase()
        self.clearBrowsingHistoryUseCase = KoinHelper.shared.getClearBrowsingHistoryUseCase()
    }

    func observeHistory() async {
        for await items in observeRecentlyViewedUseCase.invoke(limit: 200) {
            groups = Self.groupByDate(items)
            isEmpty = items.isEmpty
        }
    }

    func deleteItem(historyId: Int64) {
        Task { try? await deleteBrowsingHistoryItemUseCase.invoke(historyId: historyId) }
    }

    func clearAll() {
        Task { try? await clearBrowsingHistoryUseCase.invoke() }
    }

    private static func groupByDate(_ items: [RecentlyViewedModel]) -> [DateGroup] {
        let now = Date().timeIntervalSince1970 * 1000
        let todayStart = now - now.truncatingRemainder(dividingBy: dayMillis)
        let yesterdayStart = todayStart - dayMillis
        let weekStart = todayStart - weekMillis

        var today: [RecentlyViewedModel] = []
        var yesterday: [RecentlyViewedModel] = []
        var thisWeek: [RecentlyViewedModel] = []
        var earlier: [RecentlyViewedModel] = []

        for item in items {
            let ts = Double(item.viewedAt)
            if ts >= todayStart {
                today.append(item)
            } else if ts >= yesterdayStart {
                yesterday.append(item)
            } else if ts >= weekStart {
                thisWeek.append(item)
            } else {
                earlier.append(item)
            }
        }

        var result: [DateGroup] = []
        if !today.isEmpty { result.append(DateGroup(id: "today", label: "Today", items: today)) }
        if !yesterday.isEmpty { result.append(DateGroup(id: "yesterday", label: "Yesterday", items: yesterday)) }
        if !thisWeek.isEmpty { result.append(DateGroup(id: "week", label: "This Week", items: thisWeek)) }
        if !earlier.isEmpty { result.append(DateGroup(id: "earlier", label: "Earlier", items: earlier)) }
        return result
    }

    private static let dayMillis: Double = 86_400_000
    private static let weekMillis: Double = 7 * dayMillis
}
