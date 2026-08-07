package com.sworddao.phoenix.feature.quest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    @Query("SELECT * FROM quest ORDER BY `order`")
    fun getAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE id = :questId")
    fun getQuestById(questId: String): Flow<QuestEntity?>

    @Query("SELECT * FROM quest WHERE type = :type ORDER BY `order`")
    fun getQuestsByType(type: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE difficulty = :difficulty ORDER BY `order`")
    fun getQuestsByDifficulty(difficulty: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE category = :category ORDER BY `order`")
    fun getQuestsByCategory(category: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE status = :status ORDER BY `order`")
    fun getQuestsByStatus(status: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE npcId = :npcId ORDER BY `order`")
    fun getQuestsByNpc(npcId: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE locationId = :locationId ORDER BY `order`")
    fun getQuestsByLocation(locationId: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest_progress WHERE questId = :questId")
    fun getQuestProgress(questId: String): Flow<QuestProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuest(quest: QuestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuests(quests: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: QuestProgressEntity)

    @Query("UPDATE quest SET status = :status WHERE id = :questId")
    suspend fun updateQuestStatus(questId: String, status: String)

    @Query("UPDATE quest SET status = :status WHERE id = :questId AND status = 'LOCKED'")
    suspend fun unlockQuest(questId: String, status: String)

    @Query("SELECT COUNT(*) FROM quest")
    suspend fun countQuests(): Int

    @Query("DELETE FROM quest")
    suspend fun clearQuests()

    @Query("DELETE FROM quest_progress")
    suspend fun clearProgress()
}
