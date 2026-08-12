package com.sworddao.phoenix.feature.npc.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "npc")
data class NpcEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val occupation: String,
    val personality: String,
    val currentLocation: String,
    val friendshipXp: Int = 0,
    val scheduleJson: String = "[]",
    val avatarEmoji: String,
    val idleAnimationState: String,
    val interactionAvailability: String,
    val unlockRequirements: String? = null,
    val vocabularyCategoriesJson: String = "[]",
    val dialogueReferencesJson: String = "[]",
    val shortDescription: String = ""
)

@Dao
interface NpcDao {

    @Query("SELECT * FROM npc ORDER BY id")
    fun getAllNpcs(): Flow<List<NpcEntity>>

    @Query("SELECT * FROM npc WHERE id = :id")
    fun getNpcById(id: String): Flow<NpcEntity?>

    @Query("SELECT * FROM npc WHERE currentLocation = :locationName ORDER BY id")
    fun getNpcsByLocation(locationName: String): Flow<List<NpcEntity>>

    @Query("SELECT COUNT(*) FROM npc")
    suspend fun countNpcs(): Int

    @Query("UPDATE npc SET friendshipXp = friendshipXp + :xpGain WHERE id = :npcId")
    suspend fun updateFriendshipXp(npcId: String, xpGain: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(npcs: List<NpcEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(npc: NpcEntity)

    @Query("DELETE FROM npc")
    suspend fun clear()
}
