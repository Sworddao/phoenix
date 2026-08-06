package com.sworddao.phoenix.feature.listening.data

import kotlinx.serialization.Serializable

@Serializable
enum class ListeningDifficulty(val displayName: String, val level: Int, val description: String) {
    BEGINNER("Beginner", 1, "Simple words and short greetings"),
    ELEMENTARY("Elementary", 2, "Common phrases and short sentences"),
    INTERMEDIATE("Intermediate", 3, "Conversational sentences and directions"),
    ADVANCED("Advanced", 4, "Fast speech, numbers, and orders"),
}

@Serializable
enum class ListeningExerciseType(val displayName: String, val description: String) {
    HEAR_AND_CHOOSE_MEANING("Hear and Choose Meaning", "Listen and pick the meaning"),
    HEAR_AND_IDENTIFY_VOCABULARY("Hear and Identify Vocabulary", "Listen and identify the word"),
    HEAR_AND_MATCH_IMAGE("Hear and Match Image", "Listen and match the picture"),
    HEAR_AND_CHOOSE_NPC_RESPONSE("Hear and Choose NPC Response", "Listen to an NPC and pick your response"),
    HEAR_NUMBERS("Hear Numbers", "Listen and identify the number"),
    HEAR_GREETINGS("Hear Greetings", "Listen and understand greetings"),
    HEAR_DIRECTIONS("Hear Directions", "Listen and follow directions"),
    HEAR_FOOD_ORDERS("Hear Food Orders", "Listen and understand food orders"),
}

@Serializable
data class AudioClip(
    val id: String,
    val text: String,
    val hanzi: String? = null,
    val english: String,
    val audioPath: String = "",
    val durationMs: Long = 2000,
    val wordId: String? = null,
    val phraseId: String? = null,
    val npcId: String? = null,
) {
    val displayText: String
        get() = hanzi ?: text
}

@Serializable
data class ListeningChoice(
    val id: String,
    val text: String,
    val pinyin: String? = null,
)

@Serializable
data class ListeningExercise(
    val id: String,
    val type: ListeningExerciseType,
    val difficulty: ListeningDifficulty,
    val clip: AudioClip,
    val prompt: String,
    val choices: List<ListeningChoice>,
    val correctChoiceIndex: Int = 0,
    val context: String = "",
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedWordId: String? = null,
    val relatedSpeakingExerciseId: String? = null,
    val xpReward: Int = 10,
    val friendshipBonus: Int = 0,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
) {
    val correctChoice: ListeningChoice?
        get() = choices.getOrNull(correctChoiceIndex)
}

@Serializable
data class ListeningAttempt(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: String,
    val wordId: String? = null,
    val chosenChoiceId: String,
    val wasCorrect: Boolean = false,
    val replayCount: Int = 0,
    val timeTakenMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
data class ListeningSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseIds: List<String>,
    val currentExerciseIndex: Int = 0,
    val attempts: List<ListeningAttempt> = emptyList(),
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

    val correctAttempts: Int
        get() = attempts.count { it.wasCorrect }

    val totalReplayCount: Int
        get() = attempts.sumOf { it.replayCount }
}

@Serializable
data class ListeningResult(
    val attempt: ListeningAttempt,
    val exercise: ListeningExercise,
    val isNewPersonalBest: Boolean = false,
    val streakContinued: Boolean = false,
    val currentStreak: Int = 0,
    val xpEarned: Int = 0,
    val friendshipBonusEarned: Int = 0,
    val badgeProgress: Map<String, Float> = emptyMap(),
) {
    val shouldCelebrate: Boolean
        get() = attempt.wasCorrect && (isNewPersonalBest || streakContinued)

    val feedbackMessage: String
        get() = if (attempt.wasCorrect) "听对了！" else "再听一遍，你很快就能听出来！"
}

@Serializable
data class ListeningProgress(
    val itemId: String,
    val wordId: String? = null,
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val replayCount: Int = 0,
    val bestTimeMs: Long = 0,
    val lastListenedAt: Long? = null,
    val masteryLevel: ListeningMastery = ListeningMastery.NEW,
) {
    val successRate: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f

    val isMastered: Boolean
        get() = masteryLevel == ListeningMastery.MASTERED
}

@Serializable
enum class ListeningMastery(val displayName: String, val level: Int, val requiredSuccessRate: Float, val minAttempts: Int) {
    NEW("New", 0, 0f, 0),
    LEARNING("Learning", 1, 0.3f, 3),
    IMPROVING("Improving", 2, 0.5f, 5),
    CONFIDENT("Confident", 3, 0.7f, 10),
    MASTERED("Mastered", 4, 0.85f, 20),
}

@Serializable
data class ListeningStatistics(
    val totalSessions: Int = 0,
    val totalExercises: Int = 0,
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val totalReplayCount: Int = 0,
    val totalTimeListenedMs: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastListeningDate: Long? = null,
    val wordsPracticed: Int = 0,
    val wordsMastered: Int = 0,
    val averageTimePerExerciseMs: Long = 0,
    val exercisesByType: Map<ListeningExerciseType, Int> = emptyMap(),
    val exercisesByDifficulty: Map<ListeningDifficulty, Int> = emptyMap(),
    val listeningBadges: List<ListeningBadge> = emptyList(),
) {
    val overallAccuracy: Float
        get() = if (totalAttempts > 0) correctAttempts.toFloat() / totalAttempts else 0f
}

@Serializable
data class ListeningBadge(
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
            ListeningBadge("listen_first", "First Listen", "Complete your first listening exercise", "👂"),
            ListeningBadge("listen_streak_3", "3-Day Listening Streak", "Practice listening 3 days in a row", "🔥"),
            ListeningBadge("listen_streak_7", "Week of Ears", "Practice listening 7 days in a row", "🗓️"),
            ListeningBadge("listen_streak_30", "Monthly Listener", "Practice listening 30 days in a row", "🏆"),
            ListeningBadge("listen_quick_ear", "Quick Ear", "Reach a 10-day listening streak", "⚡"),
            ListeningBadge("listen_accurate", "Accurate Ear", "Get 20 correct listening answers", "🎯"),
            ListeningBadge("listen_npc_ready", "Conversation Ear", "Complete 15 NPC listening exercises", "💬"),
            ListeningBadge("listen_word_collector", "Word Listener", "Master 20 words by listening", "📚"),
        )

        fun getBadge(id: String): ListeningBadge? = ALL_BADGES.find { it.id == id }
    }
}

sealed class ListeningResultStatus {
    data class Success(val message: String) : ListeningResultStatus()
    data class Error(val message: String) : ListeningResultStatus()
    data class ExerciseCompleted(val result: ListeningResult) : ListeningResultStatus()
    data class SessionCompleted(val session: ListeningSession, val statistics: ListeningStatistics) : ListeningResultStatus()
    data class StreakUpdated(val currentStreak: Int, val longestStreak: Int) : ListeningResultStatus()
    data class BadgeEarned(val badge: ListeningBadge) : ListeningResultStatus()
    data class ProgressUpdated(val progress: ListeningProgress) : ListeningResultStatus()
    data class ReplayRecorded(val exerciseId: String, val replayCount: Int) : ListeningResultStatus()
}

@Serializable
data class ListeningSessionConfig(
    val exerciseType: ListeningExerciseType = ListeningExerciseType.HEAR_AND_CHOOSE_MEANING,
    val difficulty: ListeningDifficulty = ListeningDifficulty.BEGINNER,
    val exerciseCount: Int = 5,
    val wordIds: List<String> = emptyList(),
    val npcId: String? = null,
    val questId: String? = null,
)
