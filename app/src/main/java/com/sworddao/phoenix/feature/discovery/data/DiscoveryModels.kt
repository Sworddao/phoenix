package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import kotlinx.serialization.Serializable

@Serializable
enum class DiscoverySourceType(val displayName: String) {
    NPC("NPC Interaction"),
    DIALOGUE("Conversation"),
    QUEST("Quest Reward"),
    FRIENDSHIP("Friendship Milestone"),
    REGION("Region Discovery"),
    PASSPORT("Passport Stamp"),
    STORY("Story Progression"),
    LISTENING("Listening Practice"),
    SPEAKING("Speaking Practice"),
    MINI_GAME("Mini Game"),
    FESTIVAL("Festival Event"),
    HIDDEN("Hidden Discovery"),
    EXPLORATION("Exploration"),
}

@Serializable
data class VocabularyDiscovery(
    val id: String,
    val wordId: String,
    val word: VocabularyWord? = null,
    val source: DiscoverySourceType,
    val sourceId: String,
    val sourceName: String,
    val discoveredAt: Long,
    val isFirstDiscovery: Boolean,
    val bonusXp: Int = 0,
    val bonusFriendshipXp: Int = 0,
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedRegionId: String? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class DiscoveryReward(
    val xp: Int = 0,
    val friendshipXp: Int = 0,
    val vocabularyWords: List<String> = emptyList(),
    val streakBonus: Int = 0,
    val categoryBonus: Boolean = false,
    val regionBonus: Boolean = false,
)

@Serializable
data class NewlyUnlockedWord(
    val word: VocabularyWord,
    val source: DiscoverySourceType,
    val sourceName: String,
    val discoveredAt: Long,
    val isFirstDiscovery: Boolean,
    val reward: DiscoveryReward,
)

@Serializable
data class DiscoveryHistory(
    val discoveries: List<VocabularyDiscovery>,
    val totalCount: Int,
    val todayCount: Int,
    val weekCount: Int,
    val streakDays: Int,
    val lastDiscoveryDate: Long?,
    val wordsBySource: Map<DiscoverySourceType, Int>,
    val wordsByCategory: Map<VocabularyCategory, Int>,
    val wordsByRegion: Map<String, Int>,
)

@Serializable
data class DiscoveryStatistics(
    val totalDiscovered: Int,
    val totalAvailable: Int,
    val todayDiscovered: Int,
    val weekDiscovered: Int,
    val monthDiscovered: Int,
    val streakDays: Int,
    val longestStreak: Int,
    val lastDiscoveryDate: Long?,
    val wordsBySource: Map<DiscoverySourceType, Int>,
    val wordsByCategory: Map<VocabularyCategory, Int>,
    val wordsByMastery: Map<VocabularyMastery, Int>,
    val wordsByRegion: Map<String, Int>,
    val averageDiscoveriesPerDay: Float,
    val completionPercentage: Float,
)

@Serializable
data class DiscoverySession(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val discoveries: List<VocabularyDiscovery>,
    val source: DiscoverySourceType,
    val sourceId: String,
    val totalXpEarned: Int = 0,
    val totalFriendshipXpEarned: Int = 0,
    val isActive: Boolean = true,
)

@Serializable
data class DailyDiscovery(
    val date: Long,
    val discoveries: List<VocabularyDiscovery>,
    val totalCount: Int,
    val streakDay: Boolean,
)

sealed class DiscoveryResult {
    data class WordDiscovered(
        val word: VocabularyWord,
        val discovery: VocabularyDiscovery,
        val isFirstDiscovery: Boolean,
        val reward: DiscoveryReward,
    ) : DiscoveryResult()

    data class WordAlreadyDiscovered(
        val word: VocabularyWord,
        val discovery: VocabularyDiscovery,
    ) : DiscoveryResult()

    data class BatchDiscovered(
        val words: List<NewlyUnlockedWord>,
        val totalXp: Int,
        val totalFriendshipXp: Int,
    ) : DiscoveryResult()

    data class Success(val message: String) : DiscoveryResult()
    data class Error(val message: String) : DiscoveryResult()
}

@Serializable
data class DiscoveryAnimationState(
    val isShowing: Boolean = false,
    val currentWord: VocabularyWord? = null,
    val source: DiscoverySourceType? = null,
    val sourceName: String? = null,
    val isFirstDiscovery: Boolean = false,
    val reward: DiscoveryReward? = null,
    val animationPhase: AnimationPhase = AnimationPhase.IDLE,
)

@Serializable
enum class AnimationPhase {
    IDLE,
    WORD_APPEARING,
    WORD_DISPLAYING,
    REWARD_SHOWING,
    COMPLETING,
}
