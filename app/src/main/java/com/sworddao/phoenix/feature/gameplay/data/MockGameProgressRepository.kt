package com.sworddao.phoenix.feature.gameplay.data

import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockGameProgressRepository @Inject constructor() : GameProgressRepository {

    private val _gameProgress = MutableStateFlow(GameProgress())
    private val _sessionSummary = MutableStateFlow(SessionSummary())

    override fun getGameProgress(): Flow<GameProgress> = _gameProgress.asStateFlow()

    override fun getSessionSummary(): Flow<SessionSummary> = _sessionSummary.asStateFlow()

    override suspend fun recordDialogueCompleted(npcId: String) {
        val current = _gameProgress.value
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

        _gameProgress.value = current.copy(
            totalDialoguesCompleted = current.totalDialoguesCompleted + 1,
            npcsInteractedWith = newNpcList,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            dialoguesCompleted = _sessionSummary.value.dialoguesCompleted + 1,
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordWordDiscovered(wordId: String) {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstVocabulary) {
            milestones.add(GameMilestone.FIRST_VOCABULARY)
        }
        val newCount = current.totalWordsDiscovered + 1
        if (newCount >= 10 && !current.isWordCollector) {
            milestones.add(GameMilestone.WORD_COLLECTOR)
        }

        _gameProgress.value = current.copy(
            totalWordsDiscovered = newCount,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            wordsDiscovered = _sessionSummary.value.wordsDiscovered + 1,
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordQuestCompleted(questId: String) {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstQuest) {
            milestones.add(GameMilestone.FIRST_QUEST)
        }
        val newCount = current.totalQuestsCompleted + 1
        if (newCount >= 5 && !current.isQuestMaster) {
            milestones.add(GameMilestone.QUEST_MASTER)
        }

        _gameProgress.value = current.copy(
            totalQuestsCompleted = newCount,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            questsCompleted = _sessionSummary.value.questsCompleted + 1,
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordFriendshipLevelUp(npcId: String) {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstFriendship) {
            milestones.add(GameMilestone.FIRST_FRIENDSHIP)
        }

        _gameProgress.value = current.copy(
            totalFriendshipLevels = current.totalFriendshipLevels + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            friendshipLevelsGained = _sessionSummary.value.friendshipLevelsGained + 1,
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordPassportStampEarned(regionId: String) {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstPassportStamp) {
            milestones.add(GameMilestone.FIRST_PASSPORT_STAMP)
        }

        _gameProgress.value = current.copy(
            totalPassportStamps = current.totalPassportStamps + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            passportStampsEarned = _sessionSummary.value.passportStampsEarned + 1,
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordSpeakingPractice() {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstSpeaking) {
            milestones.add(GameMilestone.FIRST_SPEAKING)
        }

        _gameProgress.value = current.copy(
            totalSpeakingPractices = current.totalSpeakingPractices + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun recordListeningPractice() {
        val current = _gameProgress.value
        val milestones = current.milestonesCompleted.toMutableList()
        if (!current.hasCompletedFirstListening) {
            milestones.add(GameMilestone.FIRST_LISTENING)
        }

        _gameProgress.value = current.copy(
            totalListeningPractices = current.totalListeningPractices + 1,
            milestonesCompleted = milestones,
            lastActivityTime = System.currentTimeMillis()
        )

        _sessionSummary.value = _sessionSummary.value.copy(
            milestonesUnlocked = _sessionSummary.value.milestonesUnlocked +
                    milestones.filter { it !in _sessionSummary.value.milestonesUnlocked }
        )
    }

    override suspend fun unlockMilestone(milestone: GameMilestone) {
        val current = _gameProgress.value
        if (milestone !in current.milestonesCompleted) {
            _gameProgress.value = current.copy(
                milestonesCompleted = current.milestonesCompleted + milestone,
                lastActivityTime = System.currentTimeMillis()
            )

            _sessionSummary.value = _sessionSummary.value.copy(
                milestonesUnlocked = _sessionSummary.value.milestonesUnlocked + milestone
            )
        }
    }

    override suspend fun resetSession() {
        _gameProgress.value = GameProgress()
        _sessionSummary.value = SessionSummary()
    }
}
