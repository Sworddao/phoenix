package com.sworddao.phoenix.feature.gameplay.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomGameProgressRepository @Inject constructor(
    private val dao: GameProgressDao,
) : GameProgressRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.getGameProgressDocOnce() == null) {
                dao.upsertGameProgressDoc(GameProgressEntity("all", RoomJson.toJson(GameProgress())))
            }
            if (dao.getSessionSummaryDocOnce() == null) {
                dao.upsertSessionSummaryDoc(SessionSummaryEntity("all", RoomJson.toJson(SessionSummary())))
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getGameProgress(): Flow<GameProgress> =
        seededFlow { dao.getGameProgressDoc().map { doc -> RoomJson.fromJsonOrNull<GameProgress>(doc?.gameProgressJson) ?: GameProgress() } }

    override fun getSessionSummary(): Flow<SessionSummary> =
        seededFlow { dao.getSessionSummaryDoc().map { doc -> RoomJson.fromJsonOrNull<SessionSummary>(doc?.sessionSummaryJson) ?: SessionSummary() } }

    private suspend fun loadProgress(): GameProgress =
        RoomJson.fromJsonOrNull(dao.getGameProgressDocOnce()?.gameProgressJson) ?: GameProgress()

    private suspend fun saveProgress(progress: GameProgress) {
        dao.upsertGameProgressDoc(GameProgressEntity("all", RoomJson.toJson(progress)))
    }

    private suspend fun loadSummary(): SessionSummary =
        RoomJson.fromJsonOrNull(dao.getSessionSummaryDocOnce()?.sessionSummaryJson) ?: SessionSummary()

    private suspend fun saveSummary(summary: SessionSummary) {
        dao.upsertSessionSummaryDoc(SessionSummaryEntity("all", RoomJson.toJson(summary)))
    }

    override suspend fun recordDialogueCompleted(npcId: String) {
        ensureSeeded()
        val current = loadProgress()
        val newNpcList = if (npcId !in current.npcsInteractedWith) {
            current.npcsInteractedWith + npcId
        } else {
            current.npcsInteractedWith
        }

        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstDialogue) {
            milestones.add(GameMilestone.FIRST_DIALOGUE)
        }
        if (newNpcList.size >= 4 && !current.isVillageExplorer) {
            milestones.add(GameMilestone.VILLAGE_EXPLORER)
        }

        saveProgress(current.copy(
            totalDialoguesCompleted = current.totalDialoguesCompleted + 1,
            npcsInteractedWith = newNpcList,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            dialoguesCompleted = summary.dialoguesCompleted + 1,
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordWordDiscovered(wordId: String) {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstVocabulary) {
            milestones.add(GameMilestone.FIRST_VOCABULARY)
        }
        val newCount = current.totalWordsDiscovered + 1
        if (newCount >= 10 && !current.isWordCollector) {
            milestones.add(GameMilestone.WORD_COLLECTOR)
        }

        saveProgress(current.copy(
            totalWordsDiscovered = newCount,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            wordsDiscovered = summary.wordsDiscovered + 1,
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordQuestCompleted(questId: String) {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstQuest) {
            milestones.add(GameMilestone.FIRST_QUEST)
        }
        val newCount = current.totalQuestsCompleted + 1
        if (newCount >= 5 && !current.isQuestMaster) {
            milestones.add(GameMilestone.QUEST_MASTER)
        }

        saveProgress(current.copy(
            totalQuestsCompleted = newCount,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            questsCompleted = summary.questsCompleted + 1,
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordFriendshipLevelUp(npcId: String) {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstFriendship) {
            milestones.add(GameMilestone.FIRST_FRIENDSHIP)
        }

        saveProgress(current.copy(
            totalFriendshipLevels = current.totalFriendshipLevels + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            friendshipLevelsGained = summary.friendshipLevelsGained + 1,
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordPassportStampEarned(regionId: String) {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstPassportStamp) {
            milestones.add(GameMilestone.FIRST_PASSPORT_STAMP)
        }

        saveProgress(current.copy(
            totalPassportStamps = current.totalPassportStamps + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            passportStampsEarned = summary.passportStampsEarned + 1,
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordSpeakingPractice() {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstSpeaking) {
            milestones.add(GameMilestone.FIRST_SPEAKING)
        }

        saveProgress(current.copy(
            totalSpeakingPractices = current.totalSpeakingPractices + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordListeningPractice() {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstListening) {
            milestones.add(GameMilestone.FIRST_LISTENING)
        }

        saveProgress(current.copy(
            totalListeningPractices = current.totalListeningPractices + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun recordReadingPractice() {
        ensureSeeded()
        val current = loadProgress()
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstReading) {
            milestones.add(GameMilestone.FIRST_READING)
        }

        saveProgress(current.copy(
            totalReadingPractices = current.totalReadingPractices + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis(),
        ))

        val summary = loadSummary()
        saveSummary(summary.copy(
            milestonesUnlocked = summary.milestonesUnlocked +
                milestones.filter { it !in summary.milestonesUnlocked },
        ))
    }

    override suspend fun unlockMilestone(milestone: GameMilestone) {
        ensureSeeded()
        val current = loadProgress()
        if (milestone !in current.milestonesCompleted) {
            saveProgress(current.copy(
                milestonesCompleted = current.milestonesCompleted + milestone,
                lastActivityTime = System.currentTimeMillis(),
            ))

            val summary = loadSummary()
            saveSummary(summary.copy(
                milestonesUnlocked = summary.milestonesUnlocked + milestone,
            ))
        }
    }

    override suspend fun resetSession() {
        ensureSeeded()
        saveProgress(GameProgress())
        saveSummary(SessionSummary())
    }
}
