package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QualityScoreCalculatorTest {

    @Test
    fun zeroStatsReturnsZero() {
        val stats = ModelStats(
            downloadCount = 0,
            favoriteCount = 0,
            commentCount = 0,
            ratingCount = 0,
            rating = 0.0,
        )
        assertEquals(0, QualityScoreCalculator.calculate(stats))
    }

    @Test
    fun highQualityModelScoresHigh() {
        val stats = ModelStats(
            downloadCount = 5000,
            favoriteCount = 800,
            commentCount = 50,
            ratingCount = 100,
            rating = 4.8,
        )
        val score = QualityScoreCalculator.calculate(stats)
        assertTrue(score >= 70, "High quality model should score >= 70, got $score")
    }

    @Test
    fun lowEngagementModelScoresLow() {
        val stats = ModelStats(
            downloadCount = 100,
            favoriteCount = 1,
            commentCount = 0,
            ratingCount = 0,
            rating = 0.0,
        )
        val score = QualityScoreCalculator.calculate(stats)
        assertTrue(score <= 30, "Low engagement model should score <= 30, got $score")
    }

    @Test
    fun buzzFarmingPenaltyApplied() {
        // Suspicious: tons of downloads but almost no favorites/ratings
        val suspicious = ModelStats(
            downloadCount = 50000,
            favoriteCount = 10,
            commentCount = 0,
            ratingCount = 0,
            rating = 0.0,
        )
        val legitimate = ModelStats(
            downloadCount = 50000,
            favoriteCount = 5000,
            commentCount = 100,
            ratingCount = 500,
            rating = 4.5,
        )
        val suspiciousScore = QualityScoreCalculator.calculate(suspicious)
        val legitimateScore = QualityScoreCalculator.calculate(legitimate)
        assertTrue(
            legitimateScore > suspiciousScore,
            "Legitimate model ($legitimateScore) should score higher than suspicious ($suspiciousScore)",
        )
    }

    @Test
    fun buzzFarmingPenaltyIsZeroBelowThreshold() {
        val stats = ModelStats(
            downloadCount = 500,
            favoriteCount = 0,
            commentCount = 0,
            ratingCount = 0,
            rating = 0.0,
        )
        val penalty = QualityScoreCalculator.calculateBuzzFarmingPenalty(stats)
        assertEquals(0.0, penalty)
    }

    @Test
    fun buzzFarmingPenaltyDetectsLowFavRatio() {
        val stats = ModelStats(
            downloadCount = 10000,
            favoriteCount = 2,
            commentCount = 0,
            ratingCount = 0,
            rating = 0.0,
        )
        val penalty = QualityScoreCalculator.calculateBuzzFarmingPenalty(stats)
        assertTrue(penalty > 0.0, "Should detect low fav ratio as suspicious")
    }

    @Test
    fun scoreIsBoundedBetween0And100() {
        val extremeHigh = ModelStats(
            downloadCount = 1000000,
            favoriteCount = 500000,
            commentCount = 10000,
            ratingCount = 50000,
            rating = 5.0,
        )
        val score = QualityScoreCalculator.calculate(extremeHigh)
        assertTrue(score in 0..100, "Score should be bounded 0-100, got $score")
    }

    @Test
    fun moderateModelScoresInMiddleRange() {
        val stats = ModelStats(
            downloadCount = 1000,
            favoriteCount = 50,
            commentCount = 5,
            ratingCount = 10,
            rating = 3.5,
        )
        val score = QualityScoreCalculator.calculate(stats)
        assertTrue(score in 20..70, "Moderate model should score 20-70, got $score")
    }

    @Test
    fun ratingInfluencesScore() {
        val lowRating = ModelStats(
            downloadCount = 1000,
            favoriteCount = 100,
            commentCount = 10,
            ratingCount = 50,
            rating = 1.5,
        )
        val highRating = lowRating.copy(rating = 4.8)
        assertTrue(
            QualityScoreCalculator.calculate(highRating) >
                QualityScoreCalculator.calculate(lowRating),
            "Higher rating should produce higher score",
        )
    }
}
