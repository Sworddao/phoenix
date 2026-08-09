package com.sworddao.phoenix.feature.review.data

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.ln

// ---------------------------------------------------------------------
// Review sources & types
// ---------------------------------------------------------------------

@Serializable
enum class ReviewSource(val displayName: String, val icon: String) {
    VOCABULARY("词汇", "🆕"),
    DIALOGUE("对话", "💬"),
    SPEAKING("口语", "🗣️"),
    LISTENING("聆听", "👂"),
    READING("阅读", "📖"),
    WRITING("书写", "✍️"),
    NPC_CONVERSATION("NPC 对话", "🧑‍🌾"),
    QUEST("任务", "📜"),
    FRIENDSHIP("友谊", "🤝"),
    EXPLORATION("探索", "🧭"),
}

@Serializable
enum class ReviewType(val displayName: String, val icon: String) {
    CONVERSATION("对话复习", "💬"),
    LISTENING("聆听复习", "👂"),
    SPEAKING("口语复习", "🗣️"),
    READING("阅读复习", "📖"),
    WRITING("书写复习", "✍️"),
    MIXED("混合复习", "🔀"),
    NPC_CHALLENGE("NPC 挑战", "🧑‍🌾"),
    QUEST_REVIEW("任务复习", "📜"),
    DAILY_REVIEW("每日复习", "📅"),
}

// ---------------------------------------------------------------------
// Adaptive difficulty
// ---------------------------------------------------------------------

@Serializable
enum class ReviewDifficulty(val displayName: String, val symbol: String) {
    NEW("新词", "🆕"),
    LEARNING("学习中", "🌱"),
    FAMILIAR("熟悉", "👍"),
    MASTERED("已掌握", "✅");

    companion object {
        fun fromStrength(strength: Float): ReviewDifficulty = when {
            strength >= 0.8f -> MASTERED
            strength >= 0.55f -> FAMILIAR
            strength >= 0.3f -> LEARNING
            else -> NEW
        }
    }
}

// ---------------------------------------------------------------------
// Memory model
// ---------------------------------------------------------------------

@Serializable
data class MemoryStrength(
    val strength: Float = 0.4f,
    val confidence: Float = 0.5f,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val averageScore: Float = 0f,
    val speakingAccuracy: Float = 0f,
    val listeningAccuracy: Float = 0f,
    val readingAccuracy: Float = 0f,
    val writingAccuracy: Float = 0f,
    val conversationSuccess: Float = 0f,
    val lastReviewAt: Long? = null,
    val nextReviewAt: Long = 0L,
    val streak: Int = 0,
    val consecutiveFailures: Int = 0,
    val reviewCount: Int = 0,
) {
    val accuracy: Float
        get() = (speakingAccuracy + listeningAccuracy + readingAccuracy + writingAccuracy) / 4f

    val masteryLevel: ReviewDifficulty
        get() = ReviewDifficulty.fromStrength(strength)

    val overallAccuracy: Float
        get() = if (reviewCount == 0) 0f
        else (correctAnswers.toFloat() / reviewCount).coerceIn(0f, 1f)
}

// ---------------------------------------------------------------------
// Review schedule
// ---------------------------------------------------------------------

@Serializable
data class ReviewSchedule(
    val itemId: String,
    val wordId: String? = null,
    val stage: Int = 0,
    val intervalMillis: Long = SpacedRepetitionEngine.intervalForStage(0),
    val dueAt: Long = 0L,
    val lastReviewedAt: Long? = null,
) {
    val isDue: Boolean
        get() = dueAt <= System.currentTimeMillis()
}

// ---------------------------------------------------------------------
// Review item
// ---------------------------------------------------------------------

@Serializable
data class ReviewItem(
    val id: String,
    val source: ReviewSource,
    val type: ReviewType,
    val prompt: String,
    val detail: String,
    val wordId: String? = null,
    val hanzi: String? = null,
    val pinyin: String? = null,
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val memoryStrength: Float = 0f,
    val schedule: ReviewSchedule = ReviewSchedule(itemId = id),
) {
    val difficulty: ReviewDifficulty
        get() = SpacedRepetitionEngine.difficultyFor(memoryStrength)

    val isDue: Boolean
        get() = schedule.dueAt <= System.currentTimeMillis()

    val priority: Float
        get() = SpacedRepetitionEngine.priorityFor(memoryStrength, schedule.stage)
}

// ---------------------------------------------------------------------
// History & sessions
// ---------------------------------------------------------------------

@Serializable
data class ReviewHistoryEntry(
    val id: String,
    val itemId: String,
    val wordId: String? = null,
    val reviewedAt: Long,
    val correct: Boolean,
    val score: Float,
    val intervalMillis: Long,
    val type: ReviewType,
    val strengthAfter: Float,
)

@Serializable
data class ReviewSession(
    val id: String,
    val type: ReviewType,
    val items: List<ReviewItem> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val totalCount: Int = 0,
    val isCompleted: Boolean = false,
) {
    val progress: Float
        get() = if (totalCount == 0) 0f
        else ((correctCount + incorrectCount).toFloat() / totalCount).coerceIn(0f, 1f)

    val accuracy: Float
        get() = if (totalCount == 0) 0f
        else (correctCount.toFloat() / totalCount).coerceIn(0f, 1f)

    val answeredCount: Int
        get() = correctCount + incorrectCount
}

// ---------------------------------------------------------------------
// Statistics & daily review
// ---------------------------------------------------------------------

@Serializable
data class ReviewStatistics(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val totalReviews: Int = 0,
    val correctReviews: Int = 0,
    val incorrectReviews: Int = 0,
    val totalScore: Float = 0f,
    val wordsReviewed: Int = 0,
    val wordsMastered: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val xpEarned: Int = 0,
    val byType: Map<ReviewType, Int> = emptyMap(),
) {
    val averageScore: Float
        get() = if (totalReviews == 0) 0f else (totalScore / totalReviews).coerceIn(0f, 1f)

    val accuracy: Float
        get() = if (totalReviews == 0) 0f
        else (correctReviews.toFloat() / totalReviews).coerceIn(0f, 1f)
}

@Serializable
data class DailyReview(
    val date: String = "",
    val dueCount: Int = 0,
    val completedCount: Int = 0,
    val dailyGoal: Int = DAILY_GOAL,
    val weakestWords: List<MemoryStrength> = emptyList(),
    val bestWords: List<MemoryStrength> = emptyList(),
) {
    val completionPercent: Float
        get() = (completedCount.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val isGoalReached: Boolean
        get() = completedCount >= dailyGoal

    val activitiesRemaining: Int
        get() = (dailyGoal - completedCount).coerceAtLeast(0)

    companion object {
        const val DAILY_GOAL = 5
    }
}

// ---------------------------------------------------------------------
// Recommendations
// ---------------------------------------------------------------------

@Serializable
data class ReviewRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val type: ReviewType,
    val priority: Float,
    val icon: String,
)

// ---------------------------------------------------------------------
// Results
// ---------------------------------------------------------------------

sealed class ReviewResult {
    data class Success(val message: String) : ReviewResult()
    data class Refreshed(val dueCount: Int) : ReviewResult()
    data class Error(val message: String) : ReviewResult()
    data class SessionStarted(val session: ReviewSession) : ReviewResult()
    data class SessionCompleted(
        val session: ReviewSession,
        val xpEarned: Int,
        val accuracy: Float,
    ) : ReviewResult()
    data class Answered(
        val itemId: String,
        val wordId: String?,
        val correct: Boolean,
        val score: Float,
        val strengthAfter: Float,
        val intervalMillis: Long,
        val nextReviewAt: Long,
        val difficulty: ReviewDifficulty,
    ) : ReviewResult()
}

// ---------------------------------------------------------------------
// Spaced repetition engine (pure, deterministic)
// ---------------------------------------------------------------------

object SpacedRepetitionEngine {

    const val MAX_STAGE = 6
    const val MASTERY_THRESHOLD = 0.8f

    private const val MILLIS_PER_MINUTE = 60 * 1000L
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    fun intervalForStage(stage: Int): Long {
        val clamped = stage.coerceIn(0, MAX_STAGE)
        return when (clamped) {
            0 -> 10 * MILLIS_PER_MINUTE
            1 -> 1 * MILLIS_PER_DAY
            2 -> 3 * MILLIS_PER_DAY
            3 -> 7 * MILLIS_PER_DAY
            4 -> 14 * MILLIS_PER_DAY
            5 -> 30 * MILLIS_PER_DAY
            else -> 90 * MILLIS_PER_DAY
        }
    }

    fun nextStage(
        correct: Boolean,
        score: Float,
        currentStage: Int,
        consecutiveFailures: Int,
    ): Int {
        if (!correct) return (currentStage - 2).coerceAtLeast(0)
        val recovering = consecutiveFailures >= 2
        val advance = if (score >= 0.95f && !recovering) 2 else 1
        return (currentStage + advance).coerceAtMost(MAX_STAGE)
    }

    fun isMastered(strength: Float): Boolean = strength >= MASTERY_THRESHOLD

    fun adjustMemory(
        current: MemoryStrength,
        correct: Boolean,
        score: Float,
    ): MemoryStrength {
        val clampedScore = score.coerceIn(0f, 1f)
        val reviewCount = current.reviewCount + 1
        if (correct) {
            val newConfidence = (current.confidence + 0.15f * clampedScore).coerceIn(0.05f, 1f)
            val newAverageScore = weightedAverage(
                current = current.averageScore,
                newScore = clampedScore,
                previousCount = current.reviewCount,
            )
            return current.copy(
                confidence = newConfidence,
                correctAnswers = current.correctAnswers + 1,
                averageScore = newAverageScore,
                streak = current.streak + 1,
                consecutiveFailures = 0,
                reviewCount = reviewCount,
                strength = computeStrength(
                    confidence = newConfidence,
                    correctAnswers = current.correctAnswers + 1,
                    reviewCount = reviewCount,
                    averageScore = newAverageScore,
                ),
            )
        }
        val newConfidence = (current.confidence - 0.25f).coerceIn(0.05f, 1f)
        return current.copy(
            confidence = newConfidence,
            incorrectAnswers = current.incorrectAnswers + 1,
            streak = 0,
            consecutiveFailures = current.consecutiveFailures + 1,
            reviewCount = reviewCount,
            strength = computeStrength(
                confidence = newConfidence,
                correctAnswers = current.correctAnswers,
                reviewCount = reviewCount,
                averageScore = current.averageScore,
            ),
        )
    }

    fun withPractice(
        current: MemoryStrength,
        mode: ReviewType,
        accuracy: Float,
    ): MemoryStrength {
        val clamped = accuracy.coerceIn(0f, 1f)
        return when (mode) {
            ReviewType.SPEAKING -> current.copy(speakingAccuracy = clamped)
            ReviewType.LISTENING -> current.copy(listeningAccuracy = clamped)
            ReviewType.READING -> current.copy(readingAccuracy = clamped)
            ReviewType.WRITING -> current.copy(writingAccuracy = clamped)
            ReviewType.CONVERSATION, ReviewType.NPC_CHALLENGE ->
                current.copy(conversationSuccess = clamped)
            else -> current
        }
    }

    fun recallProbability(strength: Float, elapsedMillis: Long): Float {
        val elapsedDays = elapsedMillis.toDouble() / MILLIS_PER_DAY.toDouble()
        val decay = exp(-elapsedDays * ln(2.0) / 7.0)
        return (strength.toDouble() * decay).toFloat().coerceIn(0f, 1f)
    }

    fun difficultyFor(strength: Float): ReviewDifficulty =
        ReviewDifficulty.fromStrength(strength)

    fun priorityFor(strength: Float, stage: Int): Float {
        val masteryGap = 1f - strength.coerceIn(0f, 1f)
        val stageBoost = (stage.toFloat() / MAX_STAGE) * 0.2f
        return (masteryGap * 0.6f + stageBoost).coerceIn(0f, 1f)
    }

    fun computeStrength(
        confidence: Float,
        correctAnswers: Int,
        reviewCount: Int,
        averageScore: Float,
    ): Float {
        val recall = (correctAnswers + 1f) / (reviewCount + 2f)
        val scoreWeight = if (reviewCount == 0) 0f else averageScore
        return (0.4f * confidence + 0.4f * recall + 0.2f * scoreWeight)
            .coerceIn(0f, 1f)
    }

    fun weightedAverage(current: Float, newScore: Float, previousCount: Int): Float {
        if (previousCount == 0) return newScore
        return ((current * previousCount) + newScore) / (previousCount + 1)
    }
}