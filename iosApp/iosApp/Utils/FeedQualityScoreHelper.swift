import Foundation
import Shared

/// Mirrors QualityScoreCalculator from KMP (Kotlin object not exported to iOS).
/// Calculates a quality score (0-100) for a model based on its stats.
enum FeedQualityScoreHelper {
    private static let engagementWeight = 0.30
    private static let ratingWeight = 0.30
    private static let commentWeight = 0.20
    private static let volumeWeight = 0.20
    private static let excellentEngagementRatio = 0.10
    private static let goodEngagementRatio = 0.05
    private static let suspiciousDownloadThreshold: Int32 = 1000
    private static let suspiciousFavRatio = 0.005
    private static let suspiciousRatingRatio = 0.002
    private static let volumeLogBase = 10000.0

    static func calculate(stats: ModelStats) -> Int32 {
        let dl = stats.downloadCount
        let fav = stats.favoriteCount
        guard dl > 0 || fav > 0 else { return 0 }

        let engagement = engagementScore(stats)
        let rating = ratingScore(stats)
        let comment = commentScore(stats)
        let volume = volumeScore(stats)

        let raw = (engagement * engagementWeight
            + rating * ratingWeight
            + comment * commentWeight
            + volume * volumeWeight) * 100.0

        let penalty = buzzFarmingPenalty(stats)
        let final = Int32(raw * (1.0 - penalty))
        return min(100, max(0, final))
    }

    private static func engagementScore(_ s: ModelStats) -> Double {
        guard s.downloadCount > 0 else {
            return s.favoriteCount > 0 ? 0.5 : 0.0
        }
        let ratio = Double(s.favoriteCount) / Double(s.downloadCount)
        if ratio >= excellentEngagementRatio { return 1.0 }
        if ratio >= goodEngagementRatio {
            return 0.5 + (ratio - goodEngagementRatio) / (excellentEngagementRatio - goodEngagementRatio) * 0.5
        }
        return ratio / goodEngagementRatio * 0.5
    }

    private static func ratingScore(_ s: ModelStats) -> Double {
        guard s.ratingCount > 0 else { return 0.3 }
        let normalized = min(1.0, max(0.0, (s.rating - 1.0) / 4.0))
        let confidence = min(1.0, Double(s.ratingCount) / 20.0)
        return normalized * (0.5 + 0.5 * confidence)
    }

    private static func commentScore(_ s: ModelStats) -> Double {
        guard s.commentCount > 0 else { return 0.0 }
        return min(1.0, log(Double(s.commentCount) + 1.0) / log(100.0))
    }

    private static func volumeScore(_ s: ModelStats) -> Double {
        let total = Double(s.downloadCount + s.favoriteCount)
        guard total > 0 else { return 0.0 }
        return min(1.0, log(total + 1.0) / log(volumeLogBase))
    }

    private static func buzzFarmingPenalty(_ s: ModelStats) -> Double {
        guard s.downloadCount >= suspiciousDownloadThreshold else { return 0.0 }
        var penalty = 0.0
        let favRatio = Double(s.favoriteCount) / Double(s.downloadCount)
        let ratingRatio = Double(s.ratingCount) / Double(s.downloadCount)
        if favRatio < suspiciousFavRatio { penalty += 0.4 }
        if ratingRatio < suspiciousRatingRatio { penalty += 0.2 }
        if s.downloadCount > suspiciousDownloadThreshold * 10, s.ratingCount == 0 {
            penalty += 0.2
        }
        return min(0.8, penalty)
    }
}
