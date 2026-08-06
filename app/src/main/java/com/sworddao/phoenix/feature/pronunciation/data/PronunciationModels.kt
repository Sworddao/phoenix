package com.sworddao.phoenix.feature.pronunciation.data

import kotlinx.serialization.Serializable

@Serializable
enum class SpeakingDifficulty(val displayName: String, val level: Int, val description: String) {
    BEGINNER("Beginner", 1, "Simple words and basic greetings"),
    ELEMENTARY("Elementary", 2, "Common phrases and short sentences"),
    INTERMEDIATE("Intermediate", 3, "Conversational sentences with tones"),
    UPPER_INTERMEDIATE("Upper Intermediate", 4, "Complex sentences and natural speech"),
    ADVANCED("Advanced", 5, "Native-speed conversations and idioms"),
}

@Serializable
enum class SpeakingExerciseType(val displayName: String, val description: String) {
    REPEAT_AFTER_NPC("Repeat After NPC", "Listen to NPC and repeat the phrase"),
    VOCABULARY_WORD("Vocabulary Word", "Practice a single vocabulary word"),
    DIALOGUE_PHRASE("Dialogue Phrase", "Practice a phrase from conversation"),
    TONE_PRACTICE("Tone Practice", "Focus on correct tone production"),
    SENTENCE_BUILDING("Sentence Building", "Build and speak complete sentences"),
    FREESTYLE("Freestyle Speaking", "Speak freely on a topic"),
}

@Serializable
enum class PronunciationFeedbackType(val displayName: String, val isPositive: Boolean) {
    EXCELLENT("Excellent!", true),
    GREAT_START("Great start!", true),
    NICE_IMPROVEMENT("Nice improvement!", true),
    ALMOST("Almost!", true),
    GETTING_CLOSER("You're getting closer!", true),
    SOUNDED_SMOOTHER("That sounded smoother!", true),
    TRY_AGAIN("Let's try that again.", false),
    KEEP_PRACTICING("Keep practicing!", false),
}

@Serializable
data class PronunciationAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: String,
    val wordId: String?,
    val phraseId: String?,
    val expectedText: String,
    val expectedPinyin: String,
    val spokenText: String? = null,
    val confidence: Float = 0f,
    val feedbackType: PronunciationFeedbackType = PronunciationFeedbackType.TRY_AGAIN,
    val toneAccuracy: Float = 0f,
    val fluencyScore: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val wasSuccessful: Boolean = false,
    val audioPath: String? = null,
) {
    val overallScore: Float
        get() = (confidence + toneAccuracy + fluencyScore) / 3f

    val isHighConfidence: Boolean
        get() = confidence >= 0.7f && toneAccuracy >= 0.6f
}

@Serializable
data class SpeakingExercise(
    val id: String,
    val type: SpeakingExerciseType,
    val difficulty: SpeakingDifficulty,
    val expectedText: String,
    val expectedPinyin: String,
    val expectedHanzi: String? = null,
    val audioPath: String? = null,
    val wordId: String? = null,
    val phraseId: String? = null,
    val context: String = "",
    val hints: List<String> = emptyList(),
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val xpReward: Int = 10,
    val friendshipBonus: Int = 0,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
) {
    val displayText: String
        get() = expectedHanzi ?: expectedText
}

@Serializable
data class PronunciationSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseIds: List<String>,
    val currentExerciseIndex: Int = 0,
    val attempts: List<PronunciationAttempt> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalXpEarned: Int = 0,
    val totalFriendshipBonus: Int = 0,
    val isCompleted: Boolean = false,
) {
    val currentExerciseId: String?
        get() = if (currentExerciseIndex < exerciseIds.size) exerciseIds[currentExerciseIndex] else null

    val progress: Float
        get() = if (exerciseIds.isEmpty()) 0f else currentExerciseIndex.toFloat() / exerciseIds.size

    val successfulAttempts: Int
        get() = attempts.count { it.wasSuccessful }

    val averageConfidence: Float
        get() = if (attempts.isEmpty()) 0f else attempts.map { it.confidence }.average().toFloat()
}

@Serializable
data class PronunciationResult(
    val attempt: PronunciationAttempt,
    val exercise: SpeakingExercise,
    val isNewPersonalBest: Boolean = false,
    val streakContinued: Boolean = false,
    val currentStreak: Int = 0,
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val badgeProgress: Map<String, Float> = emptyMap(),
) {
    val feedbackMessage: String
        get() = attempt.feedbackType.displayName

    val shouldCelebrate: Boolean
        get() = attempt.wasSuccessful && (isNewPersonalBest || streakContinued)
}

@Serializable
data class PronunciationProgress(
    val wordId: String,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val bestConfidence: Float = 0f,
    val bestToneAccuracy: Float = 0f,
    val bestFluencyScore: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPracticedAt: Long? = null,
    val totalPracticeTimeMs: Long = 0,
    val masteryLevel: SpeakingMastery = SpeakingMastery.NEW,
) {
    val successRate: Float
        get() = if (totalAttempts > 0) successfulAttempts.toFloat() / totalAttempts else 0f

    val isMastered: Boolean
        get() = masteryLevel == SpeakingMastery.MASTERED
}

@Serializable
enum class SpeakingMastery(val displayName: String, val level: Int, val requiredSuccessRate: Float, val minAttempts: Int) {
    NEW("New", 0, 0f, 0),
    LEARNING("Learning", 1, 0.3f, 3),
    IMPROVING("Improving", 2, 0.5f, 5),
    CONFIDENT("Confident", 3, 0.7f, 10),
    MASTERED("Mastered", 4, 0.85f, 20),
}

@Serializable
data class SpeakingStatistics(
    val totalSessions: Int = 0,
    val totalExercises: Int = 0,
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val totalPracticeTimeMs: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPracticeDate: Long? = null,
    val wordsPracticed: Int = 0,
    val wordsMastered: Int = 0,
    val averageConfidence: Float = 0f,
    val averageToneAccuracy: Float = 0f,
    val averageFluencyScore: Float = 0f,
    val exercisesByType: Map<SpeakingExerciseType, Int> = emptyMap(),
    val exercisesByDifficulty: Map<SpeakingDifficulty, Int> = emptyMap(),
    val pronunciationBadges: List<PronunciationBadge> = emptyList(),
) {
    val overallSuccessRate: Float
        get() = if (totalAttempts > 0) successfulAttempts.toFloat() / totalAttempts else 0f
}

@Serializable
data class PronunciationBadge(
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
            PronunciationBadge("first_word", "First Word", "Practice your first word", "🎯"),
            PronunciationBadge("streak_3", "3-Day Streak", "Practice 3 days in a row", "🔥"),
            PronunciationBadge("streak_7", "Week Warrior", "Practice 7 days in a row", "🗓️"),
            PronunciationBadge("streak_30", "Monthly Master", "Practice 30 days in a row", "🏆"),
            PronunciationBadge("confident_speaker", "Confident Speaker", "Reach 80% confidence on 10 words", "🗣️"),
            PronunciationBadge("tone_master", "Tone Master", "Perfect tones on 20 exercises", "🎵"),
            PronunciationBadge("conversation_ready", "Conversation Ready", "Complete 50 dialogue phrases", "💬"),
            PronunciationBadge("pronunciation_pro", "Pronunciation Pro", "Master 100 words", "⭐"),
        )

        fun getBadge(id: String): PronunciationBadge? = ALL_BADGES.find { it.id == id }
    }
}

sealed class PronunciationResultStatus {
    data class Success(val message: String) : PronunciationResultStatus()
    data class Error(val message: String) : PronunciationResultStatus()
    data class ExerciseCompleted(val result: PronunciationResult) : PronunciationResultStatus()
    data class SessionCompleted(val session: PronunciationSession, val statistics: SpeakingStatistics) : PronunciationResultStatus()
    data class StreakUpdated(val currentStreak: Int, val longestStreak: Int) : PronunciationResultStatus()
    data class BadgeEarned(val badge: PronunciationBadge) : PronunciationResultStatus()
    data class ProgressUpdated(val progress: PronunciationProgress) : PronunciationResultStatus()
}

@Serializable
data class SpeakingQuestObjective(
    val exerciseType: SpeakingExerciseType,
    val difficulty: SpeakingDifficulty,
    val targetCount: Int,
    val currentCount: Int = 0,
    val wordIds: List<String> = emptyList(),
) {
    val isComplete: Boolean
        get() = currentCount >= targetCount

    val progress: Float
        get() = if (targetCount > 0) currentCount.toFloat() / targetCount else 0f
}

@Serializable
data class PronunciationSessionConfig(
    val exerciseType: SpeakingExerciseType = SpeakingExerciseType.REPEAT_AFTER_NPC,
    val difficulty: SpeakingDifficulty = SpeakingDifficulty.BEGINNER,
    val exerciseCount: Int = 5,
    val wordIds: List<String> = emptyList(),
    val phraseIds: List<String> = emptyList(),
    val npcId: String? = null,
    val questId: String? = null,
    val enableTonePractice: Boolean = true,
    val enableFreestyle: Boolean = false,
)