package com.sworddao.phoenix.feature.writing.data

import kotlinx.serialization.Serializable

@Serializable
enum class WritingDifficulty(val displayName: String, val level: Int, val description: String) {
    BEGINNER("Beginner", 1, "Single characters with simple strokes"),
    ELEMENTARY("Elementary", 2, "Common characters with basic stroke order"),
    INTERMEDIATE("Intermediate", 3, "Characters with more complex stroke patterns"),
    ADVANCED("Advanced", 4, "Multi-stroke characters and repeated strokes"),
}

@Serializable
enum class StrokeType(val displayName: String, val displayNameCn: String) {
    HORIZONTAL("Horizontal", "横"),
    VERTICAL("Vertical", "竖"),
    LEFT_FALLING("Left-falling", "撇"),
    RIGHT_FALLING("Right-falling", "捺"),
    DOT("Dot", "点"),
    HOOK("Hook", "钩"),
    RAISING("Rising", "提"),
    TURNING("Turning", "折"),
}

@Serializable
enum class StrokeDirection(val displayName: String, val description: String) {
    LEFT_TO_RIGHT("Left to Right", "Draw from the left to the right"),
    RIGHT_TO_LEFT("Right to Left", "Draw from the right to the left"),
    TOP_TO_BOTTOM("Top to Bottom", "Draw from the top to the bottom"),
    BOTTOM_TO_TOP("Bottom to Top", "Draw from the bottom to the top"),
    DIAGONAL_DOWN_LEFT("Diagonal Down-Left", "Draw diagonally to the lower left"),
    DIAGONAL_DOWN_RIGHT("Diagonal Down-Right", "Draw diagonally to the lower right"),
}

@Serializable
data class HanziStroke(
    val id: String,
    val character: String,
    val order: Int,
    val type: StrokeType,
    val direction: StrokeDirection,
    val name: String,
    val nameCn: String,
)

@Serializable
data class HanziCharacter(
    val id: String,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val syllableTones: List<Int> = emptyList(),
    val wordId: String? = null,
    val strokes: List<HanziStroke> = emptyList(),
    val difficulty: WritingDifficulty = WritingDifficulty.BEGINNER,
    val xpReward: Int = 10,
    val order: Int = 0,
) {
    val strokeCount: Int
        get() = strokes.size

    val isSeeded: Boolean
        get() = strokes.isNotEmpty()
}

@Serializable
enum class WritingExerciseType(val displayName: String, val description: String) {
    TRACE_STROKES("Trace Strokes", "Trace each stroke of the character in order"),
    STROKE_ORDER("Stroke Order", "Put the strokes in the correct order"),
    DIRECTION_CHECK("Stroke Direction", "Identify the correct direction of a stroke"),
}

@Serializable
data class WritingExercise(
    val id: String,
    val type: WritingExerciseType,
    val difficulty: WritingDifficulty,
    val character: HanziCharacter,
    val prompt: String,
    val xpReward: Int = 10,
    val friendshipBonus: Int = 0,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
) {
    val hanzi: String
        get() = character.hanzi

    val strokeCount: Int
        get() = character.strokeCount
}

@Serializable
data class WritingStrokeAnswer(
    val strokeIndex: Int,
    val expectedType: StrokeType,
    val expectedDirection: StrokeDirection,
    val wasCorrect: Boolean = false,
    val attempts: Int = 0,
)

@Serializable
data class WritingAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: String,
    val wordId: String? = null,
    val hanzi: String = "",
    val strokeAnswers: List<WritingStrokeAnswer> = emptyList(),
    val timeTakenMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val correctStrokeCount: Int
        get() = strokeAnswers.count { it.wasCorrect }

    val totalStrokeCount: Int
        get() = strokeAnswers.size

    val wasCorrect: Boolean
        get() = strokeAnswers.isNotEmpty() && strokeAnswers.all { it.wasCorrect }

    val accuracy: Float
        get() = if (totalStrokeCount > 0) correctStrokeCount.toFloat() / totalStrokeCount else 0f
}

@Serializable
data class WritingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseIds: List<String>,
    val currentExerciseIndex: Int = 0,
    val attempts: List<WritingAttempt> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalXpEarned: Int = 0,
    val totalFriendshipBonus: Int = 0,
    val totalCorrectStrokes: Int = 0,
    val isCompleted: Boolean = false,
) {
    val currentExerciseId: String?
        get() = if (currentExerciseIndex < exerciseIds.size) exerciseIds[currentExerciseIndex] else null

    val progress: Float
        get() = if (exerciseIds.isEmpty()) 0f else currentExerciseIndex.toFloat() / exerciseIds.size

    val correctAttempts: Int
        get() = attempts.count { it.wasCorrect }
}

@Serializable
data class WritingResult(
    val attempt: WritingAttempt,
    val exercise: WritingExercise,
    val isNewPersonalBest: Boolean = false,
    val streakContinued: Boolean = false,
    val currentStreak: Int = 0,
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val reward: WritingReward = WritingReward(),
    val badgeProgress: Map<String, Float> = emptyMap(),
) {
    val shouldCelebrate: Boolean
        get() = attempt.wasCorrect && (isNewPersonalBest || streakContinued || reward.isFirstCharacterWritten)

    val feedbackMessage: String
        get() = if (attempt.wasCorrect) "写对了！" else "笔画顺序还要再练练，继续加油！"
}

@Serializable
data class WritingReward(
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val streakContinued: Boolean = false,
    val isFirstCharacterWritten: Boolean = false,
    val newMastery: WritingMastery? = null,
    val isNewPersonalBest: Boolean = false,
    val badgeProgress: Map<String, Float> = emptyMap(),
)

@Serializable
data class WritingProgress(
    val itemId: String,
    val wordId: String? = null,
    val hanzi: String = "",
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val timesWritten: Int = 0,
    val totalStrokes: Int = 0,
    val correctStrokes: Int = 0,
    val bestTimeMs: Long = 0,
    val lastWrittenAt: Long? = null,
    val masteryLevel: WritingMastery = WritingMastery.NEW,
) {
    val successRate: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f

    val strokeAccuracy: Float
        get() = if (totalStrokes > 0) correctStrokes.toFloat() / totalStrokes else 0f

    val isMastered: Boolean
        get() = masteryLevel == WritingMastery.MASTERED
}

@Serializable
enum class WritingMastery(val displayName: String, val level: Int, val requiredSuccessRate: Float, val minAttempts: Int) {
    NEW("New", 0, 0f, 0),
    SEEN("Seen", 1, 0.2f, 1),
    LEARNING("Learning", 2, 0.4f, 3),
    FAMILIAR("Familiar", 3, 0.6f, 6),
    CONFIDENT("Confident", 4, 0.75f, 10),
    MASTERED("Mastered", 5, 0.85f, 15),
}

@Serializable
data class WritingStatistics(
    val totalSessions: Int = 0,
    val totalExercises: Int = 0,
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val totalStrokes: Int = 0,
    val correctStrokes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWritingDate: Long? = null,
    val charactersWritten: Int = 0,
    val charactersMastered: Int = 0,
    val averageTimePerExerciseMs: Long = 0,
    val exercisesByType: Map<WritingExerciseType, Int> = emptyMap(),
    val exercisesByDifficulty: Map<WritingDifficulty, Int> = emptyMap(),
    val writingBadges: List<WritingBadge> = emptyList(),
) {
    val overallAccuracy: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f
}

@Serializable
data class WritingBadge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val earnedAt: Long? = null,
    val progress: Float = 0f,
    val isEarned: Boolean = false,
) {
    companion object {
        val ALL_BADGES = listOf(
            WritingBadge("write_first", "First Stroke", "Write your first character", "✏️"),
            WritingBadge("write_streak_3", "3-Day Writing Streak", "Practice writing 3 days in a row", "🔥"),
            WritingBadge("write_streak_7", "Week of Ink", "Practice writing 7 days in a row", "🗓️"),
            WritingBadge("write_streak_30", "Monthly Calligrapher", "Practice writing 30 days in a row", "🏆"),
            WritingBadge("write_steady_hand", "Steady Hand", "Reach a 10-day writing streak", "✋"),
            WritingBadge("write_stroke_perfect", "Stroke Perfect", "Get 20 correct characters in a row", "🎯"),
            WritingBadge("write_dialogue_ready", "Pen Ready", "Complete 15 writing exercises", "💬"),
            WritingBadge("write_char_collector", "Character Writer", "Master 10 characters by writing", "🔤"),
        )

        fun getBadge(id: String): WritingBadge? = ALL_BADGES.find { it.id == id }
    }
}

sealed class WritingResultStatus {
    data class Success(val message: String) : WritingResultStatus()
    data class Error(val message: String) : WritingResultStatus()
    data class StrokeChecked(val feedback: EngineStrokeFeedback) : WritingResultStatus()
    data class ExerciseCompleted(val result: WritingResult) : WritingResultStatus()
    data class SessionCompleted(val session: WritingSession, val statistics: WritingStatistics) : WritingResultStatus()
    data class StreakUpdated(val currentStreak: Int, val longestStreak: Int) : WritingResultStatus()
    data class BadgeEarned(val badge: WritingBadge) : WritingResultStatus()
    data class ProgressUpdated(val progress: WritingProgress) : WritingResultStatus()
}

@Serializable
data class WritingSessionConfig(
    val exerciseType: WritingExerciseType = WritingExerciseType.TRACE_STROKES,
    val difficulty: WritingDifficulty = WritingDifficulty.BEGINNER,
    val exerciseCount: Int = 5,
    val characterIds: List<String> = emptyList(),
    val npcId: String? = null,
    val questId: String? = null,
)
