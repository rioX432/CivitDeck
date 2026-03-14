package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelStats
import kotlin.math.ln
import kotlin.math.min

/**
 * Calculates a quality score (0-100) for a model based on its stats.
 *
 * Scoring components:
 * - Engagement ratio (favorite/download) — 30%
 * - Rating — 30%
 * - Comment activity — 20%
 * - Volume bonus (log-scaled total engagement) — 20%
 *
 * Anti-buzz-farming heuristics penalize suspicious patterns:
 * - Very high downloads but suspiciously low favorites/ratings
 * - New models with unusually high download counts but negligible engagement
 */
object QualityScoreCalculator {

    private const val ENGAGEMENT_WEIGHT = 0.30
    private const val RATING_WEIGHT = 0.30
    private const val COMMENT_WEIGHT = 0.20
    private const val VOLUME_WEIGHT = 0.20

    // Engagement ratio thresholds
    private const val EXCELLENT_ENGAGEMENT_RATIO = 0.10
    private const val GOOD_ENGAGEMENT_RATIO = 0.05

    // Anti-buzz-farming thresholds
    private const val SUSPICIOUS_DOWNLOAD_THRESHOLD = 1000
    private const val SUSPICIOUS_FAV_RATIO = 0.005
    private const val SUSPICIOUS_RATING_RATIO = 0.002

    // Volume scaling
    private const val VOLUME_LOG_BASE = 10000.0

    fun calculate(stats: ModelStats): Int {
        if (stats.downloadCount <= 0 && stats.favoriteCount <= 0) return 0

        val engagementScore = calculateEngagementScore(stats)
        val ratingScore = calculateRatingScore(stats)
        val commentScore = calculateCommentScore(stats)
        val volumeScore = calculateVolumeScore(stats)

        val rawScore = (
            engagementScore * ENGAGEMENT_WEIGHT +
                ratingScore * RATING_WEIGHT +
                commentScore * COMMENT_WEIGHT +
                volumeScore * VOLUME_WEIGHT
            ) * 100.0

        val penalty = calculateBuzzFarmingPenalty(stats)
        val finalScore = (rawScore * (1.0 - penalty)).toInt()

        return finalScore.coerceIn(0, 100)
    }

    private fun calculateEngagementScore(stats: ModelStats): Double {
        if (stats.downloadCount <= 0) {
            return if (stats.favoriteCount > 0) 0.5 else 0.0
        }
        val ratio = stats.favoriteCount.toDouble() / stats.downloadCount
        return when {
            ratio >= EXCELLENT_ENGAGEMENT_RATIO -> 1.0
            ratio >= GOOD_ENGAGEMENT_RATIO -> 0.5 + (ratio - GOOD_ENGAGEMENT_RATIO) /
                (EXCELLENT_ENGAGEMENT_RATIO - GOOD_ENGAGEMENT_RATIO) * 0.5
            else -> ratio / GOOD_ENGAGEMENT_RATIO * 0.5
        }
    }

    private fun calculateRatingScore(stats: ModelStats): Double {
        if (stats.ratingCount <= 0) return 0.3 // neutral score for unrated
        // Rating is 1-5 scale, normalize to 0-1
        val normalized = ((stats.rating - 1.0) / 4.0).coerceIn(0.0, 1.0)
        // Boost confidence with more ratings (diminishing returns)
        val confidence = min(1.0, stats.ratingCount / 20.0)
        return normalized * (0.5 + 0.5 * confidence)
    }

    private fun calculateCommentScore(stats: ModelStats): Double {
        if (stats.commentCount <= 0) return 0.0
        // Log-scale comments, cap at a reasonable level
        return min(1.0, ln(stats.commentCount.toDouble() + 1.0) / ln(100.0))
    }

    private fun calculateVolumeScore(stats: ModelStats): Double {
        val total = (stats.downloadCount + stats.favoriteCount).toDouble()
        if (total <= 0) return 0.0
        return min(1.0, ln(total + 1.0) / ln(VOLUME_LOG_BASE))
    }

    /**
     * Returns a penalty factor (0.0 - 0.8) for suspected buzz-farming.
     * High downloads with very few favorites/ratings is suspicious.
     */
    internal fun calculateBuzzFarmingPenalty(stats: ModelStats): Double {
        if (stats.downloadCount < SUSPICIOUS_DOWNLOAD_THRESHOLD) return 0.0

        var penalty = 0.0
        val favRatio = stats.favoriteCount.toDouble() / stats.downloadCount
        val ratingRatio = stats.ratingCount.toDouble() / stats.downloadCount

        // Penalize if favorite-to-download ratio is suspiciously low
        if (favRatio < SUSPICIOUS_FAV_RATIO) {
            penalty += 0.4
        }

        // Penalize if rating-to-download ratio is suspiciously low
        if (ratingRatio < SUSPICIOUS_RATING_RATIO) {
            penalty += 0.2
        }

        // Additional penalty for extreme cases: very high downloads, zero ratings
        if (stats.downloadCount > SUSPICIOUS_DOWNLOAD_THRESHOLD * 10 &&
            stats.ratingCount == 0
        ) {
            penalty += 0.2
        }

        return min(0.8, penalty)
    }
}
