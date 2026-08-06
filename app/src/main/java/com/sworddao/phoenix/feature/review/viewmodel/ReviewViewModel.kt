package com.sworddao.phoenix.feature.review.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sworddao.phoenix.feature.review.data.DailyReview
import com.sworddao.phoenix.feature.review.data.MemoryStrength
import com.sworddao.phoenix.feature.review.data.ReviewHistoryEntry
import com.sworddao.phoenix.feature.review.data.ReviewItem
import com.sworddao.phoenix.feature.review.data.ReviewRecommendation
import com.sworddao.phoenix.feature.review.data.ReviewResult
import com.sworddao.phoenix.feature.review.data.ReviewSession
import com.sworddao.phoenix.feature.review.data.ReviewStatistics
import com.sworddao.phoenix.feature.review.data.ReviewType
import com.sworddao.phoenix.feature.review.domain.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val todayReviews: List<ReviewItem> = emptyList(),
    val upcomingReviews: List<ReviewItem> = emptyList(),
    val statistics: ReviewStatistics = ReviewStatistics(),
    val recommendations: List<ReviewRecommendation> = emptyList(),
    val dailyReview: DailyReview = DailyReview(),
    val memoryStrengths: List<MemoryStrength> = emptyList(),
    val recentHistory: List<ReviewHistoryEntry> = emptyList(),
    val activeSession: ReviewSession? = null,
    val sessionProgress: Float = 0f,
    val sessionAnswers: Int = 0,
    val lastAnswerCorrect: Boolean? = null,
    val lastAnswerStrengthAfter: Float = 0f,
    val completedAccuracy: Float = 0f,
    val completedXp: Int = 0,
    val showCompletion: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                reviewRepository.refresh()
                _uiState.value = _uiState.value.copy(
                    todayReviews = reviewRepository.getTodayReviews().firstOrNull() ?: emptyList(),
                    upcomingReviews = reviewRepository.getUpcomingReviews().firstOrNull() ?: emptyList(),
                    recentHistory = reviewRepository.getReviewHistory().firstOrNull() ?: emptyList(),
                    statistics = reviewRepository.getReviewStatistics().firstOrNull() ?: ReviewStatistics(),
                    recommendations = reviewRepository.getRecommendations().firstOrNull() ?: emptyList(),
                    dailyReview = reviewRepository.getDailyReview().firstOrNull() ?: DailyReview(),
                    memoryStrengths = reviewRepository.getMemoryStrengths().firstOrNull() ?: emptyList(),
                    isLoading = false,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = throwable.message ?: "无法刷新复习内容",
                )
            }
        }
    }

    fun startSession(type: ReviewType) {
        viewModelScope.launch {
            runCatching {
                reviewRepository.startSession(type)
            }.onSuccess { result ->
                when (result) {
                    is ReviewResult.SessionStarted -> {
                        _uiState.value = _uiState.value.copy(
                            activeSession = result.session,
                            sessionProgress = 0f,
                            sessionAnswers = 0,
                            lastAnswerCorrect = null,
                            showCompletion = false,
                        )
                        refresh()
                    }
                    is ReviewResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> Unit
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
        }
    }

    fun answerCurrent(correct: Boolean, score: Float = if (correct) 1f else 0f) {
        val session = _uiState.value.activeSession ?: return
        val item = session.items.getOrNull(session.answeredCount) ?: return
        if (session.isCompleted) return
        viewModelScope.launch {
            runCatching {
                reviewRepository.submitAnswer(item.id, correct, score)
            }.onSuccess { result ->
                if (result is ReviewResult.Answered) {
                    val answered = session.answeredCount + 1
                    val sessionState = _uiState.value.activeSession ?: return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        activeSession = sessionState.copy(
                            correctCount = sessionState.correctCount + if (correct) 1 else 0,
                            incorrectCount = sessionState.incorrectCount + if (correct) 0 else 1,
                        ),
                        sessionProgress = answered.toFloat() / session.totalCount.coerceAtLeast(1),
                        sessionAnswers = answered,
                        lastAnswerCorrect = correct,
                        lastAnswerStrengthAfter = result.strengthAfter,
                        showCompletion = answered >= session.totalCount,
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
        }
    }

    fun completeActiveSession() {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            runCatching {
                reviewRepository.completeSession(session.id)
            }.onSuccess { result ->
                if (result is ReviewResult.SessionCompleted) {
                    _uiState.value = _uiState.value.copy(
                        activeSession = null,
                        completedAccuracy = result.accuracy,
                        completedXp = result.xpEarned,
                        showCompletion = true,
                    )
                }
                refresh()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(error = throwable.message)
            }
        }
    }

    fun dismissCompletion() {
        _uiState.value = _uiState.value.copy(
            activeSession = null,
            sessionProgress = 0f,
            sessionAnswers = 0,
            lastAnswerCorrect = null,
            showCompletion = false,
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}