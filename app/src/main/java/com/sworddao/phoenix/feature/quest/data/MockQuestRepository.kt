package com.sworddao.phoenix.feature.quest.data

import com.sworddao.phoenix.data.seed.QuestSeedData

import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockQuestRepository @Inject constructor() : QuestRepository {

    private val _quests = MutableStateFlow(createMockQuests())
    private val _progress = MutableStateFlow<Map<String, QuestProgress>>(emptyMap())

    override fun getAllQuests(): Flow<List<Quest>> = _quests

    override fun getQuestById(questId: String): Flow<Quest?> =
        _quests.map { quests -> quests.find { it.id == questId } }

    override fun getQuestsByType(type: QuestType): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.type == type } }

    override fun getQuestsByDifficulty(difficulty: QuestDifficulty): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.difficulty == difficulty } }

    override fun getQuestsByCategory(category: QuestCategory): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.category == category } }

    override fun getQuestsByFilter(filter: QuestFilter): Flow<List<Quest>> =
        _quests.map { quests ->
            quests.filter { quest ->
                (filter.types.isEmpty() || filter.types.contains(quest.type)) &&
                    (filter.difficulties.isEmpty() || filter.difficulties.contains(quest.difficulty)) &&
                    (filter.statuses.isEmpty() || filter.statuses.contains(quest.status)) &&
                    (filter.categories.isEmpty() || filter.categories.contains(quest.category)) &&
                    (filter.searchQuery.isEmpty() ||
                        quest.title.contains(filter.searchQuery, ignoreCase = true) ||
                        quest.description.contains(filter.searchQuery, ignoreCase = true))
            }
        }

    override fun getActiveQuests(): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.status == QuestStatus.ACTIVE } }

    override fun getCompletedQuests(): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.status == QuestStatus.COMPLETED } }

    override fun getAvailableQuests(): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.status == QuestStatus.AVAILABLE } }

    override fun getQuestProgress(questId: String): Flow<QuestProgress?> =
        _progress.map { it[questId] }

    override fun getQuestStats(): Flow<QuestStats> = _quests.map { quests ->
        val completed = quests.count { it.status == QuestStatus.COMPLETED }
        val active = quests.count { it.status == QuestStatus.ACTIVE }
        val locked = quests.count { it.status == QuestStatus.LOCKED }
        val available = quests.count { it.status == QuestStatus.AVAILABLE }
        val typeGroups = quests.filter { it.status == QuestStatus.COMPLETED }.groupBy { it.type }
        val favoriteType = typeGroups.maxByOrNull { it.value.size }?.key

        QuestStats(
            totalQuests = quests.size,
            completedQuests = completed,
            activeQuests = active,
            lockedQuests = locked,
            availableQuests = available,
            completionRate = if (quests.isNotEmpty()) completed.toFloat() / quests.size else 0f,
            totalExperienceEarned = quests.filter { it.status == QuestStatus.COMPLETED }
                .sumOf { it.difficulty.experienceReward },
            favoriteQuestType = favoriteType,
        )
    }

    override suspend fun startQuest(questId: String): QuestResult {
        val currentQuests = _quests.value
        val quest = currentQuests.find { it.id == questId }
            ?: return QuestResult.Error("Quest not found")

        if (quest.status != QuestStatus.AVAILABLE) {
            return QuestResult.Error("Quest is not available to start")
        }

        val progress = QuestProgress(
            questId = questId,
            status = QuestStatus.ACTIVE,
            objectives = quest.objectives.map { it.copy(currentCount = 0, completed = false) },
            startedAt = System.currentTimeMillis(),
            attempts = 1,
        )

        _progress.update { it + (questId to progress) }
        _quests.update { quests ->
            quests.map { q ->
                if (q.id == questId) q.copy(
                    status = QuestStatus.ACTIVE,
                    objectives = progress.objectives,
                ) else q
            }
        }

        return QuestResult.Success("Quest started: ${quest.title}")
    }

    override suspend fun completeQuest(questId: String): QuestResult {
        val currentQuests = _quests.value
        val quest = currentQuests.find { it.id == questId }
            ?: return QuestResult.Error("Quest not found")

        if (quest.status != QuestStatus.ACTIVE) {
            return QuestResult.Error("Quest is not active")
        }

        if (!quest.isComplete) {
            return QuestResult.Error("Quest objectives not completed")
        }

        _progress.update { progressMap ->
            val existing = progressMap[questId]
            progressMap + (questId to (existing?.copy(
                status = QuestStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
            ) ?: QuestProgress(
                questId = questId,
                status = QuestStatus.COMPLETED,
                objectives = quest.objectives,
                completedAt = System.currentTimeMillis(),
            )))
        }

        _quests.update { quests ->
            quests.map { q ->
                if (q.id == questId) q.copy(status = QuestStatus.COMPLETED) else q
            }
        }

        // Unlock prerequisite quests
        val completedQuestIds = _quests.value
            .filter { it.status == QuestStatus.COMPLETED }
            .map { it.id }

        _quests.update { quests ->
            quests.map { q ->
                if (q.status == QuestStatus.LOCKED &&
                    q.prerequisites.questIds.all { it in completedQuestIds }
                ) {
                    q.copy(status = QuestStatus.AVAILABLE)
                } else q
            }
        }

        return QuestResult.QuestCompleted(quest, quest.rewards)
    }

    override suspend fun abandonQuest(questId: String): QuestResult {
        val currentQuests = _quests.value
        val quest = currentQuests.find { it.id == questId }
            ?: return QuestResult.Error("Quest not found")

        if (quest.status != QuestStatus.ACTIVE) {
            return QuestResult.Error("Quest is not active")
        }

        _progress.update { it - questId }
        _quests.update { quests ->
            quests.map { q ->
                if (q.id == questId) q.copy(
                    status = QuestStatus.AVAILABLE,
                    objectives = q.objectives.map { it.copy(currentCount = 0, completed = false) },
                ) else q
            }
        }

        return QuestResult.Success("Quest abandoned: ${quest.title}")
    }

    override suspend fun updateObjectiveProgress(
        questId: String,
        objectiveId: String,
        progress: Int,
    ): QuestResult {
        val currentQuests = _quests.value
        val quest = currentQuests.find { it.id == questId }
            ?: return QuestResult.Error("Quest not found")

        val objective = quest.objectives.find { it.id == objectiveId }
            ?: return QuestResult.Error("Objective not found")

        val newCount = (objective.currentCount + progress).coerceAtMost(objective.targetCount)
        val updatedObjective = objective.copy(
            currentCount = newCount,
            completed = newCount >= objective.targetCount,
        )

        val updatedObjectives = quest.objectives.map { obj ->
            if (obj.id == objectiveId) updatedObjective else obj
        }

        _progress.update { progressMap ->
            val existing = progressMap[questId]
            progressMap + (questId to (existing?.copy(
                objectives = updatedObjectives,
                lastPlayedAt = System.currentTimeMillis(),
            ) ?: QuestProgress(
                questId = questId,
                status = QuestStatus.ACTIVE,
                objectives = updatedObjectives,
                startedAt = System.currentTimeMillis(),
                lastPlayedAt = System.currentTimeMillis(),
            )))
        }

        _quests.update { quests ->
            quests.map { q ->
                if (q.id == questId) q.copy(objectives = updatedObjectives) else q
            }
        }

        return if (updatedObjective.isComplete) {
            QuestResult.ObjectiveCompleted(updatedObjective)
        } else {
            QuestResult.Success("Objective progress updated")
        }
    }

    override suspend fun checkPrerequisites(questId: String): Boolean {
        val quest = _quests.value.find { it.id == questId } ?: return false
        val completedQuestIds = _quests.value
            .filter { it.status == QuestStatus.COMPLETED }
            .map { it.id }
        return quest.prerequisites.questIds.all { it in completedQuestIds }
    }

    override suspend fun getQuestsByNpc(npcId: String): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.npcId == npcId } }

    override suspend fun getQuestsByLocation(locationId: String): Flow<List<Quest>> =
        _quests.map { quests -> quests.filter { it.locationId == locationId } }

    override suspend fun refreshQuestAvailability() {
        val completedQuestIds = _quests.value
            .filter { it.status == QuestStatus.COMPLETED }
            .map { it.id }

        _quests.update { quests ->
            quests.map { quest ->
                if (quest.status == QuestStatus.LOCKED &&
                    quest.prerequisites.questIds.all { it in completedQuestIds }
                ) {
                    quest.copy(status = QuestStatus.AVAILABLE)
                } else quest
            }
        }
    }

    
    private fun createMockQuests(): List<Quest> =
        QuestSeedData.createMockQuests()
}
