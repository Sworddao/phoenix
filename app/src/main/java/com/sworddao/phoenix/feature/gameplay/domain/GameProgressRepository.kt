package com.sworddao.phoenix.feature.gameplay.domain

import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.GameProgress
import com.sworddao.phoenix.feature.gameplay.data.SessionSummary
import kotlinx.coroutines.flow.Flow

interface GameProgressRepository {
    fun getGameProgress(): Flow<GameProgress>
    fun getSessionSummary(): Flow<SessionSummary>
    suspend fun recordDialogueCompleted(npcId: String)
    suspend fun recordWordDiscovered(wordId: String)
    suspend fun recordQuestCompleted(questId: String)
    suspend fun recordFriendshipLevelUp(npcId: String)
    suspend fun recordPassportStampEarned(regionId: String)
    suspend fun recordSpeakingPractice()
    suspend fun unlockMilestone(milestone: GameMilestone)
    suspend fun resetSession()
}
