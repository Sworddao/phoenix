package com.sworddao.phoenix.feature.reading.data

import kotlinx.serialization.Serializable

@Serializable
enum class ReadingDifficulty(val displayName: String, val level: Int, val description: String) {
    BEGINNER("Beginner", 1, "Simple characters, pinyin and short greetings"),
    ELEMENTARY("Elementary", 2, "Common phrases and short sentences"),
    INTERMEDIATE("Intermediate", 3, "Conversational sentences and story lines"),
    ADVANCED("Advanced", 4, "NPC dialogue and connected paragraphs"),
}

@Serializable
enum class ReadingExerciseType(val displayName: String, val description: String) {
    MATCH_SPOKEN_TO_WRITTEN("Match Spoken to Written", "Match the spoken word with its hanzi"),
    MATCH_PINYIN_TO_HANZI("Match Pinyin to Hanzi", "Pick the hanzi that matches the pinyin"),
    MATCH_HANZI_TO_MEANING("Match Hanzi to Meaning", "Pick the English meaning of the hanzi"),
    SENTENCE_READING("Sentence Reading", "Read a sentence and answer a question"),
    PHRASE_RECOGNITION("Phrase Recognition", "Recognize the correct phrase"),
    CHARACTER_RECOGNITION("Character Recognition", "Identify a single character"),
    CONTEXT_READING("Context Reading", "Read a short context and respond"),
    NPC_DIALOGUE_READING("NPC Dialogue Reading", "Read an NPC line and choose a reply"),
}

@Serializable
enum class CharacterRevealState(val displayName: String) {
    HIDDEN("Hidden"),
    PINYIN_ONLY("Pinyin Only"),
    HANZI_ONLY("Hanzi Only"),
    HANZI_AND_PINYIN("Hanzi + Pinyin"),
    TONE_COLORED_PINYIN("Tone-colored Pinyin"),
    TAP_TO_REVEAL("Tap to Reveal"),
    AUTO_REVEAL("Auto Reveal"),
}

@Serializable
enum class ToneColor(val color: Long) {
    NEUTRAL(0xFF9E9E9E),
    TONE1(0xFFE53935),
    TONE2(0xFFF4511E),
    TONE3(0xFF1E88E5),
    TONE4(0xFF43A047),
}

@Serializable
data class RenderedHanziSpan(
    val text: String,
    val toneColor: ToneColor? = null,
)

@Serializable
data class RenderedHanzi(
    val hanzi: String? = null,
    val maskedHanzi: String = "",
    val pinyin: String = "",
    val toneColoredPinyin: List<RenderedHanziSpan> = emptyList(),
    val isHanziVisible: Boolean = false,
)

@Serializable
data class HanziRendererInfo(
    val name: String,
    val version: String,
    val supportedModes: List<CharacterRevealState>,
    val supportsToneColoring: Boolean = true,
)

@Serializable
data class HanziCard(
    val id: String,
    val wordId: String? = null,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val syllableTones: List<Int> = emptyList(),
    val revealState: CharacterRevealState = CharacterRevealState.PINYIN_ONLY,
    val isDiscovered: Boolean = true,
)

@Serializable
data class ReadingChoice(
    val id: String,
    val text: String,
    val pinyin: String? = null,
    val hanzi: String? = null,
)

@Serializable
data class ReadingExercise(
    val id: String,
    val type: ReadingExerciseType,
    val difficulty: ReadingDifficulty,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val syllableTones: List<Int> = emptyList(),
    val prompt: String,
    val choices: List<ReadingChoice> = emptyList(),
    val correctChoiceIndex: Int = 0,
    val context: String = "",
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedWordId: String? = null,
    val relatedSpeakingExerciseId: String? = null,
    val relatedListeningExerciseId: String? = null,
    val xpReward: Int = 10,
    val friendshipBonus: Int = 0,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
) {
    val correctChoice: ReadingChoice?
        get() = choices.getOrNull(correctChoiceIndex)

    val card: HanziCard
        get() = HanziCard(
            id = "card_$id",
            wordId = relatedWordId,
            hanzi = hanzi,
            pinyin = pinyin,
            english = english,
            syllableTones = syllableTones,
        )
}

@Serializable
data class ReadingAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: String,
    val wordId: String? = null,
    val chosenChoiceId: String,
    val wasCorrect: Boolean = false,
    val revealedHanziBeforeAnswer: Boolean = false,
    val timeTakenMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class ReadingSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseIds: List<String>,
    val currentExerciseIndex: Int = 0,
    val attempts: List<ReadingAttempt> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalXpEarned: Int = 0,
    val totalFriendshipBonus: Int = 0,
    val totalReveals: Int = 0,
    val isCompleted: Boolean = false,
) {
    val currentExerciseId: String?
        get() = if (currentExerciseIndex < exerciseIds.size) exerciseIds[currentExerciseIndex] else null

    val progress: Float
        get() = if (exerciseIds.isEmpty()) 0f else currentExerciseIndex.toFloat() / exerciseIds.size

    val correctAttempts: Int
        get() = attempts.count { it.wasCorrect }

    val revealCount: Int
        get() = attempts.count { it.revealedHanziBeforeAnswer }
}

@Serializable
data class ReadingResult(
    val attempt: ReadingAttempt,
    val exercise: ReadingExercise,
    val isNewPersonalBest: Boolean = false,
    val streakContinued: Boolean = false,
    val currentStreak: Int = 0,
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val reward: ReadingReward = ReadingReward(),
    val badgeProgress: Map<String, Float> = emptyMap(),
) {
    val shouldCelebrate: Boolean
        get() = attempt.wasCorrect && (isNewPersonalBest || streakContinued || reward.isFirstWordRead)

    val feedbackMessage: String
        get() = if (attempt.wasCorrect) "读对了！" else "再看一遍，你很快就能读懂！"
}

@Serializable
data class ReadingReward(
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val streakContinued: Boolean = false,
    val isFirstWordRead: Boolean = false,
    val newMastery: ReadingMastery? = null,
    val isNewPersonalBest: Boolean = false,
    val badgeProgress: Map<String, Float> = emptyMap(),
)

@Serializable
data class ReadingProgress(
    val itemId: String,
    val wordId: String? = null,
    val hanzi: String = "",
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val timesRead: Int = 0,
    val timesRevealed: Int = 0,
    val hasRevealedHanzi: Boolean = false,
    val bestTimeMs: Long = 0,
    val lastReadAt: Long? = null,
    val masteryLevel: ReadingMastery = ReadingMastery.NEW,
) {
    val successRate: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f

    val isMastered: Boolean
        get() = masteryLevel == ReadingMastery.MASTERED
}

@Serializable
enum class ReadingMastery(val displayName: String, val level: Int, val requiredSuccessRate: Float, val minAttempts: Int) {
    NEW("New", 0, 0f, 0),
    SEEN("Seen", 1, 0.2f, 1),
    LEARNING("Learning", 2, 0.4f, 3),
    FAMILIAR("Familiar", 3, 0.6f, 6),
    CONFIDENT("Confident", 4, 0.75f, 10),
    MASTERED("Mastered", 5, 0.85f, 15),
}

@Serializable
data class ReadingStatistics(
    val totalSessions: Int = 0,
    val totalExercises: Int = 0,
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val totalReveals: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastReadingDate: Long? = null,
    val wordsRead: Int = 0,
    val charactersRead: Int = 0,
    val wordsMastered: Int = 0,
    val averageTimePerExerciseMs: Long = 0,
    val exercisesByType: Map<ReadingExerciseType, Int> = emptyMap(),
    val exercisesByDifficulty: Map<ReadingDifficulty, Int> = emptyMap(),
    val readingBadges: List<ReadingBadge> = emptyList(),
) {
    val overallAccuracy: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f
}

@Serializable
data class ReadingBadge(
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
            ReadingBadge("read_first", "First Read", "Complete your first reading exercise", "📖"),
            ReadingBadge("read_streak_3", "3-Day Reading Streak", "Practice reading 3 days in a row", "🔥"),
            ReadingBadge("read_streak_7", "Week of Words", "Practice reading 7 days in a row", "🗓️"),
            ReadingBadge("read_streak_30", "Monthly Reader", "Practice reading 30 days in a row", "🏆"),
            ReadingBadge("read_quick_eye", "Quick Eye", "Reach a 10-day reading streak", "⚡"),
            ReadingBadge("read_accurate", "Accurate Reader", "Get 20 correct reading answers", "🎯"),
            ReadingBadge("read_dialogue_ready", "Dialogue Ready", "Complete 15 NPC reading exercises", "💬"),
            ReadingBadge("read_char_collector", "Character Collector", "Master 10 words by reading", "🔤"),
        )

        fun getBadge(id: String): ReadingBadge? = ALL_BADGES.find { it.id == id }
    }
}

sealed class ReadingResultStatus {
    data class Success(val message: String) : ReadingResultStatus()
    data class Error(val message: String) : ReadingResultStatus()
    data class ExerciseCompleted(val result: ReadingResult) : ReadingResultStatus()
    data class SessionCompleted(val session: ReadingSession, val statistics: ReadingStatistics) : ReadingResultStatus()
    data class StreakUpdated(val currentStreak: Int, val longestStreak: Int) : ReadingResultStatus()
    data class BadgeEarned(val badge: ReadingBadge) : ReadingResultStatus()
    data class ProgressUpdated(val progress: ReadingProgress) : ReadingResultStatus()
    data class RevealRecorded(val wordId: String, val revealCount: Int) : ReadingResultStatus()
}

@Serializable
data class ReadingSessionConfig(
    val exerciseType: ReadingExerciseType = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
    val difficulty: ReadingDifficulty = ReadingDifficulty.BEGINNER,
    val exerciseCount: Int = 5,
    val wordIds: List<String> = emptyList(),
    val npcId: String? = null,
    val questId: String? = null,
)
