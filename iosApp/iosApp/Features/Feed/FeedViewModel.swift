import Foundation
import Shared

@MainActor
final class FeedViewModel: ObservableObject {
    @Published var feedItems: [FeedItem] = []
    @Published var isLoading = true
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var unreadCount: Int = 0

    private let getCreatorFeedUseCase: GetCreatorFeedUseCase
    private let getUnreadFeedCountUseCase: GetUnreadFeedCountUseCase
    private let markFeedReadUseCase: MarkFeedReadUseCase
    private let observeQualityThresholdUseCase: ObserveQualityThresholdUseCase
    private var unreadObserveTask: Task<Void, Never>?
    private var thresholdObserveTask: Task<Void, Never>?
    private var allItems: [FeedItem] = []
    private var qualityThreshold: Int32 = 0

    init() {
        self.getCreatorFeedUseCase = KoinHelper.shared.getCreatorFeedUseCase()
        self.getUnreadFeedCountUseCase = KoinHelper.shared.getUnreadFeedCountUseCase()
        self.markFeedReadUseCase = KoinHelper.shared.getMarkFeedReadUseCase()
        self.observeQualityThresholdUseCase = KoinHelper.shared.getObserveQualityThresholdUseCase()
        observeUnreadCount()
        observeQualityThreshold()
    }

    func loadFeed(forceRefresh: Bool = false) async {
        if forceRefresh {
            isRefreshing = true
        } else if feedItems.isEmpty {
            isLoading = true
        }
        errorMessage = nil

        do {
            let items = try await getCreatorFeedUseCase.invoke(
                forceRefresh: forceRefresh
            )
            allItems = items.compactMap { $0 as? FeedItem }
            feedItems = filterByQuality(allItems)
            isLoading = false
            isRefreshing = false
            if !allItems.isEmpty {
                try? await markFeedReadUseCase.invoke()
            }
        } catch {
            errorMessage = error.localizedDescription
            isLoading = false
            isRefreshing = false
        }
    }

    private func observeQualityThreshold() {
        thresholdObserveTask = Task {
            for await threshold in observeQualityThresholdUseCase.invoke() {
                guard !Task.isCancelled else { return }
                if let intThreshold = threshold as? Int32 {
                    self.qualityThreshold = intThreshold
                    if !self.allItems.isEmpty {
                        self.feedItems = self.filterByQuality(self.allItems)
                    }
                }
            }
        }
    }

    private func filterByQuality(_ items: [FeedItem]) -> [FeedItem] {
        guard qualityThreshold > 0 else { return items }
        return items.filter { item in
            let score = FeedQualityScoreHelper.calculate(stats: item.stats)
            return score >= qualityThreshold
        }
    }

    private func observeUnreadCount() {
        unreadObserveTask = Task {
            for await count in getUnreadFeedCountUseCase.invoke() {
                guard !Task.isCancelled else { return }
                if let intCount = count as? Int {
                    self.unreadCount = intCount
                }
            }
        }
    }
}
