package com.sworddao.phoenix.feature.gameplay.data

import kotlinx.serialization.Serializable

@Serializable
enum class GameMilestone(val displayName: String, val description: String) {
    FIRST_DIALOGUE("初次对话", "Complete your first dialogue with an NPC"),
    FIRST_VOCABULARY("初次学习", "Discover your first vocabulary word"),
    FIRST_QUEST("初次任务", "Complete your first quest"),
    FIRST_FRIENDSHIP("初次友谊", "Reach friendship level 2 with an NPC"),
    FIRST_PASSPORT_STAMP("初次盖章", "Earn your first passport stamp"),
    VILLAGE_EXPLORER("村庄探索者", "Talk to all NPCs in Qingyuan Village"),
    WORD_COLLECTOR("词汇收藏家", "Discover 10 vocabulary words"),
    QUEST_MASTER("任务大师", "Complete 5 quests")
}

@Serializable
data class GameProgress(
    val milestonesCompleted: List<GameMilestone> = emptyList(),
    val totalDialoguesCompleted: Int = 0,
    val totalWordsDiscovered: Int = 0,
    val totalQuestsCompleted: Int = 0,
    val totalFriendshipLevels: Int = 0,
    val totalPassportStamps: Int = 0,
    val npcsInteractedWith: List<String> = emptyList(),
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis()
) {
    val hasCompletedFirstDialogue: Boolean
        get() = GameMilestone.FIRST_DIALOGUE in milestonesCompleted

    val hasCompletedFirstVocabulary: Boolean
        get() = GameMilestone.FIRST_VOCABULARY in milestonesCompleted

    val hasCompletedFirstQuest: Boolean
        get() = GameMilestone.FIRST_QUEST in milestonesCompleted

    val hasCompletedFirstFriendship: Boolean
        get() = GameMilestone.FIRST_FRIENDSHIP in milestonesCompleted

    val hasCompletedFirstPassportStamp: Boolean
        get() = GameMilestone.FIRST_PASSPORT_STAMP in milestonesCompleted

    val isVillageExplorer: Boolean
        get() = GameMilestone.VILLAGE_EXPLORER in milestonesCompleted

    val isWordCollector: Boolean
        get() = GameMilestone.WORD_COLLECTOR in milestonesCompleted

    val isQuestMaster: Boolean
        get() = GameMilestone.QUEST_MASTER in milestonesCompleted
}

@Serializable
data class SessionSummary(
    val dialoguesCompleted: Int = 0,
    val wordsDiscovered: Int = 0,
    val questsCompleted: Int = 0,
    val friendshipLevelsGained: Int = 0,
    val passportStampsEarned: Int = 0,
    val milestonesUnlocked: List<GameMilestone> = emptyList(),
    val totalXpEarned: Int = 0,
    val baoReaction: String = ""
)
