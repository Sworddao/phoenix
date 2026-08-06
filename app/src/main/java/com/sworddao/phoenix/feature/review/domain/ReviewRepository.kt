package com.sworddao.phoenix.feature.review.domain

import com.sworddao.phoenix.feature.review.data.DailyReview
import com.sworddao.phoenix.feature.review.data.MemoryStrength
import com.sworddao.phoenix.feature.review.data.ReviewHistoryEntry
import com.sworddao.phoenix.feature.review.data.ReviewItem
import com.sworddao.phoenix.feature.review.data.ReviewRecommendation
import com.sworddao.phoenix.feature.review.data.ReviewResult
import com.sworddao.phoenix.feature.review.data.ReviewStatistics
import com.sworddao.phoenix.feature.review.data.ReviewType
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getTodayReviews(): Flow<List<ReviewItem>>
    fun getUpcomingReviews(): Flow<List<ReviewItem>>
    fun getReviewHistory(): Flow<List<ReviewHistoryEntry>>
    fun getReviewStatistics(): Flow<ReviewStatistics>
    fun getRecommendations(): Flow<List<ReviewRecommendation>>
    fun getDailyReview(): Flow<DailyReview>
    fun getMemoryStrengths(): Flow<List<MemoryStrength>>

    suspend fun refresh(): ReviewResult
    suspend fun startSession(type: ReviewType): ReviewResult
    suspend fun submitAnswer(itemId: String, correct: Boolean, score: Float): ReviewResult
    suspend fun completeSession(sessionId: String): ReviewResult
    suspend fun resetReviewSystem(): ReviewResult
}