package com.sworddao.phoenix.feature.progression.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// ---------------------------------------------------------------------
// XP Rules
// ---------------------------------------------------------------------

@Serializable
enum class XpSource(val displayName: String, val baseXp: Int, val icon: String) {
    DIALOGUE("对话", 20, "💬"),
    VOCABULARY_DISCOVERY("词汇发现", 10, "🆕"),
    QUEST_COMPLETION("任务完成", 50, "📜"),
    FRIENDSHIP_LEVEL_UP("友谊升级", 30, "🤝"),
    SPEAKING_PRACTICE("口语练习", 10, "🗣️"),
    LISTENING_PRACTICE("聆听练习", 10, "👂"),
    READING_PRACTICE("阅读练习", 10, "📖"),
    EXPLORATION("探索", 25, "🧭"),
    PASSPORT_STAMP("护照盖章", 15, "📮"),
    ACHIEVEMENT("成就", 40, "🏅"),
    REVIEW("复习", 15, "🔁"),
}

// ---------------------------------------------------------------------
// Level system
// ---------------------------------------------------------------------

object XpCalculator {

    const val MAX_LEVEL = 100

    private const val BASE_XP = 100
    private const val XP_GROWTH_PER_LEVEL = 25

    fun xpRequiredForLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, MAX_LEVEL)
        return BASE_XP + (safeLevel - 1) * XP_GROWTH_PER_LEVEL
    }

    fun totalXpForLevel(level: Int): Int {
        var total = 0
        var current = 1
        while (current < level) {
            total += xpRequiredForLevel(current)
            current++
        }
        return total
    }

    fun levelForTotalXp(totalXp: Int): Int {
        val safeXp = totalXp.coerceAtLeast(0)
        var level = 1
        var accumulated = 0
        while (level < MAX_LEVEL) {
            accumulated += xpRequiredForLevel(level)
            if (safeXp < accumulated) break
            level++
        }
        return level
    }

    fun xpIntoLevel(totalXp: Int): Int {
        val level = levelForTotalXp(totalXp)
        return totalXp.coerceAtLeast(0) - totalXpForLevel(level)
    }

    fun xpRemainingToNextLevel(totalXp: Int): Int {
        val level = levelForTotalXp(totalXp)
        return xpRequiredForLevel(level) - xpIntoLevel(totalXp)
    }

    fun progressInLevel(totalXp: Int): Float {
        val level = levelForTotalXp(totalXp)
        val needed = xpRequiredForLevel(level)
        return (xpIntoLevel(totalXp).toFloat() / needed).coerceIn(0f, 1f)
    }

    fun isMaxLevel(totalXp: Int): Boolean = levelForTotalXp(totalXp) >= MAX_LEVEL
}

// ---------------------------------------------------------------------
// Feature unlocks
// ---------------------------------------------------------------------

@Serializable
enum class FeatureUnlock(
    val displayName: String,
    val displayNameCn: String,
    val description: String,
    val requiredLevel: Int,
    val icon: String,
) {
    SPEAKING("Speaking Practice", "口语练习", "Unlock the speaking practice feature", 2, "🗣️"),
    LISTENING("Listening Practice", "聆听练习", "Unlock the listening practice feature", 3, "👂"),
    READING("Reading Practice", "阅读练习", "Unlock the reading practice feature", 4, "📖"),
    QUEST_TYPES("Quest Types", "新任务类型", "Unlock new quest types", 5, "📜"),
    NPC_ACCESS("New NPCs", "新 NPC", "Unlock access to new NPCs", 7, "🧑‍🌾"),
    CONVERSATIONS("New Conversations", "新对话", "Unlock new conversations", 8, "💬"),
    REGIONS("New Regions", "新区域", "Unlock new regions", 10, "🧭"),
}

@Serializable
data class FeatureUnlockEntry(
    val feature: FeatureUnlock,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
)

// ---------------------------------------------------------------------
// Chapters
// ---------------------------------------------------------------------

@Serializable
data class ChapterUnlockRequirement(
    val requiredRegionId: String? = null,
    val requiredLevel: Int = 1,
    val requiredQuestId: String? = null,
) {
    val hasRequirements: Boolean
        get() = requiredRegionId != null || requiredLevel > 1 || requiredQuestId != null
}

@Serializable
data class ChapterInfo(
    val id: String,
    val title: String,
    val titleCn: String,
    val order: Int,
    val regionId: String,
    val icon: String = "📍",
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val completionPercentage: Float = 0f,
    val unlockRequirement: ChapterUnlockRequirement = ChapterUnlockRequirement(),
)

// ---------------------------------------------------------------------
// Player progress
// ---------------------------------------------------------------------

@Serializable
data class PlayerProgress(
    val level: Int = 1,
    val totalXp: Int = 0,
    val xpIntoLevel: Int = 0,
    val xpToNextLevel: Int = XpCalculator.xpRequiredForLevel(1),
    val currentChapter: Int = 1,
    val currentStoryStage: String = "village_intro",
    val unlockedRegionIds: List<String> = emptyList(),
    val unlockedNpcIds: List<String> = emptyList(),
    val unlockedFeatures: List<FeatureUnlock> = emptyList(),
    val villageProgress: Float = 0f,
    val chapterProgress: Float = 0f,
    val overallCompletion: Float = 0f,
    val chapters: List<ChapterInfo> = emptyList(),
) {
    val xpProgressInLevel: Float
        get() = if (xpToNextLevel > 0) (xpIntoLevel.toFloat() / xpToNextLevel).coerceIn(0f, 1f) else 1f

    @Transient
    val isFeatureUnlocked: (FeatureUnlock) -> Boolean = { it in unlockedFeatures }

    fun hasFeature(feature: FeatureUnlock): Boolean = feature in unlockedFeatures

    val nextFeatureToUnlock: FeatureUnlock?
        get() = FeatureUnlock.entries.firstOrNull { it !in unlockedFeatures }
}

// ---------------------------------------------------------------------
// Learning progress
// ---------------------------------------------------------------------

@Serializable
data class LearningProgress(
    val speakingPercent: Float = 0f,
    val listeningPercent: Float = 0f,
    val readingPercent: Float = 0f,
    val vocabularyPercent: Float = 0f,
    val conversationPercent: Float = 0f,
    val questPercent: Float = 0f,
    val friendshipPercent: Float = 0f,
    val explorationPercent: Float = 0f,
    val passportPercent: Float = 0f,
) {
    val overallPercent: Float
        get() = (speakingPercent + listeningPercent + readingPercent + vocabularyPercent +
            conversationPercent + questPercent + friendshipPercent + explorationPercent +
            passportPercent) / 9f

    fun percentFor(index: Int): Float = when (index) {
        0 -> speakingPercent
        1 -> listeningPercent
        2 -> readingPercent
        3 -> vocabularyPercent
        4 -> conversationPercent
        5 -> questPercent
        6 -> friendshipPercent
        7 -> explorationPercent
        8 -> passportPercent
        else -> 0f
    }

    companion object {
        val LABELS = listOf(
            "口语" to "Speaking",
            "聆听" to "Listening",
            "阅读" to "Reading",
            "词汇" to "Vocabulary",
            "对话" to "Conversation",
            "任务" to "Quest",
            "友谊" to "Friendship",
            "探索" to "Exploration",
            "护照" to "Passport",
        )
    }
}

// ---------------------------------------------------------------------
// Daily progress
// ---------------------------------------------------------------------

@Serializable
data class DailyProgress(
    val date: String = "",
    val xpEarnedToday: Int = 0,
    val activitiesCompletedToday: Int = 0,
    val dailyGoal: Int = 3,
    val activitiesByType: Map<XpSource, Int> = emptyMap(),
    val goalStreak: Int = 0,
) {
    val completionPercent: Float
        get() = (activitiesCompletedToday.toFloat() / dailyGoal).coerceIn(0f, 1f)

    val isGoalReached: Boolean
        get() = activitiesCompletedToday >= dailyGoal

    val activitiesRemaining: Int
        get() = (dailyGoal - activitiesCompletedToday).coerceAtLeast(0)
}

// ---------------------------------------------------------------------
// Recent unlocks & objectives
// ---------------------------------------------------------------------

@Serializable
data class RecentUnlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
enum class ObjectiveCategory(val displayName: String) {
    LEARNING("Learning"),
    EXPLORATION("Exploration"),
    FRIENDSHIP("Friendship"),
    STORY("Story"),
}

@Serializable
data class CurrentObjective(
    val id: String,
    val title: String,
    val description: String,
    val category: ObjectiveCategory,
    val currentCount: Int = 0,
    val targetCount: Int = 1,
    val icon: String = "🎯",
) {
    val progress: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount).coerceIn(0f, 1f) else 1f

    val isCompleted: Boolean
        get() = currentCount >= targetCount
}

// ---------------------------------------------------------------------
// Feature unlock timeline
// ---------------------------------------------------------------------

@Serializable
data class FeatureUnlockTimeline(
    val entries: List<FeatureUnlockEntry> = emptyList(),
)

// ---------------------------------------------------------------------
// Result statuses
// ---------------------------------------------------------------------

sealed class ProgressionResult {
    data class Success(val message: String) : ProgressionResult()
    data class Error(val message: String) : ProgressionResult()
    data class XpAwarded(val source: XpSource, val amount: Int, val newLevel: Int) : ProgressionResult()
    data class LevelUp(val newLevel: Int, val unlockedFeatures: List<FeatureUnlock>) : ProgressionResult()
    data class ChapterUnlocked(val chapter: ChapterInfo) : ProgressionResult()
    data class FeatureUnlocked(val feature: FeatureUnlock) : ProgressionResult()
    data class Refreshed(
        val playerProgress: PlayerProgress,
        val learningProgress: LearningProgress,
    ) : ProgressionResult()
}
