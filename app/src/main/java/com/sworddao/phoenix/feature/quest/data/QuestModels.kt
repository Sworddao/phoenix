package com.sworddao.phoenix.feature.quest.data

import kotlinx.serialization.Serializable

@Serializable
enum class QuestType(val displayName: String) {
    CONVERSATION("对话"),
    LISTENING("听力"),
    SPEAKING("口语"),
    EXPLORATION("探索"),
    MEMORY("记忆"),
    PRONUNCIATION("发音"),
    STORY("故事"),
    MINI_GAME("小游戏"),
    PHOTOGRAPHY("拍照"),
    COLLECTING("收集"),
    DAILY("日常"),
}

@Serializable
enum class QuestDifficulty(val displayName: String, val experienceReward: Int) {
    EASY("简单", 10),
    MEDIUM("中等", 20),
    HARD("困难", 30),
    EXPERT("专家", 50),
}

@Serializable
enum class QuestStatus {
    LOCKED,
    AVAILABLE,
    ACTIVE,
    COMPLETED,
}

@Serializable
enum class QuestCategory(val displayName: String) {
    DAILY("日常"),
    STORY("故事"),
    EXPLORATION("探索"),
    SKILL("技能"),
    CHALLENGE("挑战"),
    EVENT("活动"),
}

@Serializable
enum class ObjectiveType {
    TALK_TO_NPC,
    COMPLETE_DIALOGUE,
    VISIT_LOCATION,
    COLLECT_ITEM,
    LEARN_VOCABULARY,
    PRACTICE_SPEAKING,
    LISTEN_TO_AUDIO,
    READ_CHARACTERS,
    PHOTOGRAPH,
    DEFEAT_BOSS,
    FIND_SECRET,
    ESCORT_NPC,
    DELIVER_ITEM,
    EARN_FRIENDSHIP_POINTS,
    COMPLETE_MINI_GAME,
}

@Serializable
data class QuestObjective(
    val id: String,
    val type: ObjectiveType,
    val description: String,
    val targetId: String? = null,
    val targetCount: Int = 1,
    val currentCount: Int = 0,
    val completed: Boolean = false,
    val optional: Boolean = false,
) {
    val progress: Float
        get() = if (targetCount > 0) currentCount.toFloat() / targetCount else 0f

    val isComplete: Boolean
        get() = currentCount >= targetCount
}

@Serializable
data class QuestReward(
    val experience: Int = 0,
    val vocabulary: List<String> = emptyList(),
    val items: List<String> = emptyList(),
    val friendshipPoints: Int = 0,
    val unlockQuests: List<String> = emptyList(),
    val unlockAreas: List<String> = emptyList(),
)

@Serializable
data class QuestPrerequisite(
    val questIds: List<String> = emptyList(),
    val friendshipLevel: Int = 0,
    val requiredVocabulary: List<String> = emptyList(),
    val requiredLevel: Int = 0,
)

@Serializable
data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val difficulty: QuestDifficulty,
    val status: QuestStatus = QuestStatus.LOCKED,
    val category: QuestCategory,
    val objectives: List<QuestObjective> = emptyList(),
    val rewards: QuestReward = QuestReward(),
    val prerequisites: QuestPrerequisite = QuestPrerequisite(),
    val npcId: String? = null,
    val locationId: String? = null,
    val dialogueId: String? = null,
    val repeatable: Boolean = false,
    val daily: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val completionDialogue: String? = null,
    val failureDialogue: String? = null,
    val order: Int = 0,
    val chapter: Int = 1,
) {
    val progress: Float
        get() = if (objectives.isEmpty()) 0f
        else objectives.map { it.progress }.average().toFloat()

    val completedObjectives: Int
        get() = objectives.count { it.isComplete }

    val totalObjectives: Int
        get() = objectives.size

    val isComplete: Boolean
        get() = objectives.all { it.isComplete || it.optional }

    val activeObjectives: List<QuestObjective>
        get() = objectives.filter { !it.isComplete }
}

@Serializable
data class QuestProgress(
    val questId: String,
    val status: QuestStatus,
    val objectives: List<QuestObjective>,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val attempts: Int = 0,
) {
    val progress: Float
        get() = if (objectives.isEmpty()) 0f
        else objectives.map { it.progress }.average().toFloat()
}

@Serializable
data class QuestFilter(
    val types: List<QuestType> = emptyList(),
    val difficulties: List<QuestDifficulty> = emptyList(),
    val statuses: List<QuestStatus> = emptyList(),
    val categories: List<QuestCategory> = emptyList(),
    val searchQuery: String = "",
)

data class QuestStats(
    val totalQuests: Int,
    val completedQuests: Int,
    val activeQuests: Int,
    val lockedQuests: Int,
    val availableQuests: Int,
    val completionRate: Float,
    val totalExperienceEarned: Int,
    val favoriteQuestType: QuestType?,
)

sealed class QuestResult {
    data class Success(val message: String) : QuestResult()
    data class Error(val message: String) : QuestResult()
    data class ObjectiveCompleted(val objective: QuestObjective) : QuestResult()
    data class QuestCompleted(val quest: Quest, val rewards: QuestReward) : QuestResult()
    data class LevelUp(val newLevel: Int) : QuestResult()
    data class UnlockQuest(val questId: String) : QuestResult()
}
