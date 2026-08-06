package com.sworddao.phoenix.feature.friendship.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendshipDao {

    @Query("SELECT * FROM friendship_state WHERE npcId = :npcId")
    fun getFriendshipState(npcId: String): Flow<FriendshipEntity?>

    @Query("SELECT * FROM friendship_state")
    fun getAllFriendshipStates(): Flow<List<FriendshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFriendshipState(entity: FriendshipEntity)

    @Query("DELETE FROM friendship_state WHERE npcId = :npcId")
    suspend fun deleteFriendshipState(npcId: String)

    @Query("UPDATE friendship_state SET friendshipXp = :xp, friendshipLevel = :level, lastInteractionTimestamp = :timestamp WHERE npcId = :npcId")
    suspend fun updateFriendshipXp(npcId: String, xp: Int, level: String, timestamp: Long)

    @Query("UPDATE friendship_state SET totalConversations = totalConversations + 1, lastInteractionTimestamp = :timestamp WHERE npcId = :npcId")
    suspend fun incrementConversationCount(npcId: String, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversationMemory(entity: ConversationMemoryEntity)

    @Query("SELECT * FROM conversation_memory WHERE npcId = :npcId ORDER BY timestamp DESC LIMIT :limit")
    fun getConversationHistory(npcId: String, limit: Int = 20): Flow<List<ConversationMemoryEntity>>

    @Query("SELECT COUNT(*) FROM conversation_memory WHERE npcId = :npcId")
    fun getConversationCount(npcId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendshipEvent(entity: FriendshipEventEntity)

    @Query("SELECT * FROM friendship_event WHERE npcId = :npcId ORDER BY timestamp DESC LIMIT :limit")
    fun getFriendshipEvents(npcId: String, limit: Int = 50): Flow<List<FriendshipEventEntity>>

    @Query("UPDATE friendship_state SET unlockedTopics = :topics WHERE npcId = :npcId")
    suspend fun updateUnlockedTopics(npcId: String, topics: String)
}
