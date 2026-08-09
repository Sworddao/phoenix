package com.sworddao.phoenix.feature.quest.data

import com.sworddao.phoenix.data.seed.QuestSeedData
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomQuestRepository @Inject constructor(
    private val dao: QuestDao,
    private val gameProgressRepository: GameProgressRepository,
) : QuestRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countQuests() == 0) {
                dao.upsertQuests(QuestSeedData.createMockQuests().map { it.toEntity() })
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getAllQuests(): Flow<List<Quest>> =
        seededFlow { dao.getAllQuests().map { list -> list.map { it.toDomain() } } }

    override fun getQuestById(questId: String): Flow<Quest?> =
        seededFlow { dao.getQuestById(questId).map { it?.toDomain() } }

    override fun getQuestsByType(type: QuestType): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByType(type.name).map { list -> list.map { it.toDomain() } } }

    override fun getQuestsByDifficulty(difficulty: QuestDifficulty): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getQuestsByCategory(category: QuestCategory): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByCategory(category.name).map { list -> list.map { it.toDomain() } } }

    override fun getQuestsByFilter(filter: QuestFilter): Flow<List<Quest>> = seededFlow {
        dao.getAllQuests().map { quests ->
            quests.map { it.toDomain() }.filter { quest ->
                (filter.types.isEmpty() || filter.types.contains(quest.type)) &&
                    (filter.difficulties.isEmpty() || filter.difficulties.contains(quest.difficulty)) &&
                    (filter.statuses.isEmpty() || filter.statuses.contains(quest.status)) &&
                    (filter.categories.isEmpty() || filter.categories.contains(quest.category)) &&
                    (filter.searchQuery.isEmpty() ||
                        quest.title.contains(filter.searchQuery, ignoreCase = true) ||
                        quest.description.contains(filter.searchQuery, ignoreCase = true))
            }
        }
    }

    override fun getActiveQuests(): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByStatus(QuestStatus.ACTIVE.name).map { list -> list.map { it.toDomain() } } }

    override fun getCompletedQuests(): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByStatus(QuestStatus.COMPLETED.name).map { list -> list.map { it.toDomain() } } }

    override fun getAvailableQuests(): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByStatus(QuestStatus.AVAILABLE.name).map { list -> list.map { it.toDomain() } } }

    override fun getQuestProgress(questId: String): Flow<QuestProgress?> =
        seededFlow { dao.getQuestProgress(questId).map { it?.toDomain() } }

    override fun getQuestStats(): Flow<QuestStats> = seededFlow {
        dao.getAllQuests().map { quests ->
            val domain = quests.map { it.toDomain() }
            val completed = domain.count { it.status == QuestStatus.COMPLETED }
            val active = domain.count { it.status == QuestStatus.ACTIVE }
            val locked = domain.count { it.status == QuestStatus.LOCKED }
            val available = domain.count { it.status == QuestStatus.AVAILABLE }
            val typeGroups = domain.filter { it.status == QuestStatus.COMPLETED }.groupBy { it.type }
            val favoriteType = typeGroups.maxByOrNull { it.value.size }?.key
            QuestStats(
                totalQuests = domain.size,
                completedQuests = completed,
                activeQuests = active,
                lockedQuests = locked,
                availableQuests = available,
                completionRate = if (domain.isNotEmpty()) completed.toFloat() / domain.size else 0f,
                totalExperienceEarned = domain.filter { it.status == QuestStatus.COMPLETED }
                    .sumOf { it.difficulty.experienceReward },
                favoriteQuestType = favoriteType,
            )
        }
    }

    override suspend fun startQuest(questId: String): QuestResult {
        ensureSeeded()
        val quest = dao.getQuestById(questId).first()?.toDomain()
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
        dao.upsertProgress(progress.toEntity())
        dao.upsertQuest(
            quest.copy(status = QuestStatus.ACTIVE, objectives = progress.objectives).toEntity()
        )
        return QuestResult.Success("Quest started: ${quest.title}")
    }

    override suspend fun completeQuest(questId: String): QuestResult {
        ensureSeeded()
        val quest = dao.getQuestById(questId).first()?.toDomain()
            ?: return QuestResult.Error("Quest not found")
        if (quest.status != QuestStatus.ACTIVE) {
            return QuestResult.Error("Quest is not active")
        }
        if (!quest.isComplete) {
            return QuestResult.Error("Quest objectives not completed")
        }

        val now = System.currentTimeMillis()
        val existing = dao.getQuestProgress(questId).first()?.toDomain()
        dao.upsertProgress(
            (existing?.copy(status = QuestStatus.COMPLETED, completedAt = now)
                ?: QuestProgress(
                    questId = questId,
                    status = QuestStatus.COMPLETED,
                    objectives = quest.objectives,
                    completedAt = now,
                )).toEntity()
        )
        dao.upsertQuest(quest.copy(status = QuestStatus.COMPLETED).toEntity())
        gameProgressRepository.recordQuestCompleted(questId)
        refreshQuestAvailability()
        return QuestResult.QuestCompleted(quest, quest.rewards)
    }

    override suspend fun abandonQuest(questId: String): QuestResult {
        ensureSeeded()
        val quest = dao.getQuestById(questId).first()?.toDomain()
            ?: return QuestResult.Error("Quest not found")
        if (quest.status != QuestStatus.ACTIVE) {
            return QuestResult.Error("Quest is not active")
        }
        dao.upsertQuest(
            quest.copy(
                status = QuestStatus.AVAILABLE,
                objectives = quest.objectives.map { it.copy(currentCount = 0, completed = false) },
            ).toEntity()
        )
        return QuestResult.Success("Quest abandoned: ${quest.title}")
    }

    override suspend fun updateObjectiveProgress(
        questId: String,
        objectiveId: String,
        progress: Int,
    ): QuestResult {
        ensureSeeded()
        val quest = dao.getQuestById(questId).first()?.toDomain()
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

        val existing = dao.getQuestProgress(questId).first()?.toDomain()
        dao.upsertProgress(
            (existing?.copy(objectives = updatedObjectives, lastPlayedAt = System.currentTimeMillis())
                ?: QuestProgress(
                    questId = questId,
                    status = QuestStatus.ACTIVE,
                    objectives = updatedObjectives,
                    startedAt = System.currentTimeMillis(),
                    lastPlayedAt = System.currentTimeMillis(),
                )).toEntity()
        )
        dao.upsertQuest(quest.copy(objectives = updatedObjectives).toEntity())

        return if (updatedObjective.isComplete) {
            QuestResult.ObjectiveCompleted(updatedObjective)
        } else {
            QuestResult.Success("Objective progress updated")
        }
    }

    override suspend fun checkPrerequisites(questId: String): Boolean {
        ensureSeeded()
        val quest = dao.getQuestById(questId).first()?.toDomain() ?: return false
        val completedQuestIds = dao.getAllQuests().first()
            .filter { it.status == QuestStatus.COMPLETED.name }
            .map { it.id }
        return quest.prerequisites.questIds.all { it in completedQuestIds }
    }

    override suspend fun getQuestsByNpc(npcId: String): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override suspend fun getQuestsByLocation(locationId: String): Flow<List<Quest>> =
        seededFlow { dao.getQuestsByLocation(locationId).map { list -> list.map { it.toDomain() } } }

    override suspend fun refreshQuestAvailability() {
        ensureSeeded()
        val allQuests = dao.getAllQuests().first().map { it.toDomain() }
        val completedQuestIds = allQuests
            .filter { it.status == QuestStatus.COMPLETED }
            .map { it.id }
        allQuests
            .filter { it.status == QuestStatus.LOCKED && it.prerequisites.questIds.all { id -> id in completedQuestIds } }
            .forEach { dao.updateQuestStatus(it.id, QuestStatus.AVAILABLE.name) }
    }
}
