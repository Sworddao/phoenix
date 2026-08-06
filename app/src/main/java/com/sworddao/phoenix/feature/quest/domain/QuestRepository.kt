package com.sworddao.phoenix.feature.quest.domain

import com.sworddao.phoenix.feature.quest.data.Quest
import com.sworddao.phoenix.feature.quest.data.QuestCategory
import com.sworddao.phoenix.feature.quest.data.QuestDifficulty
import com.sworddao.phoenix.feature.quest.data.QuestFilter
import com.sworddao.phoenix.feature.quest.data.QuestProgress
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.quest.data.QuestStats
import com.sworddao.phoenix.feature.quest.data.QuestType
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    fun getAllQuests(): Flow<List<Quest>>
    fun getQuestById(questId: String): Flow<Quest?>
    fun getQuestsByType(type: QuestType): Flow<List<Quest>>
    fun getQuestsByDifficulty(difficulty: QuestDifficulty): Flow<List<Quest>>
    fun getQuestsByCategory(category: QuestCategory): Flow<List<Quest>>
    fun getQuestsByFilter(filter: QuestFilter): Flow<List<Quest>>
    fun getActiveQuests(): Flow<List<Quest>>
    fun getCompletedQuests(): Flow<List<Quest>>
    fun getAvailableQuests(): Flow<List<Quest>>
    fun getQuestProgress(questId: String): Flow<QuestProgress?>
    fun getQuestStats(): Flow<QuestStats>

    suspend fun startQuest(questId: String): QuestResult
    suspend fun completeQuest(questId: String): QuestResult
    suspend fun abandonQuest(questId: String): QuestResult
    suspend fun updateObjectiveProgress(questId: String, objectiveId: String, progress: Int): QuestResult
    suspend fun checkPrerequisites(questId: String): Boolean
    suspend fun getQuestsByNpc(npcId: String): Flow<List<Quest>>
    suspend fun getQuestsByLocation(locationId: String): Flow<List<Quest>>
    suspend fun refreshQuestAvailability(): Unit
}
