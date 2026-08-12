package com.sworddao.phoenix.feature.dialogue.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dialogue")
data class DialogueEntity(
    @PrimaryKey val id: String,
    val npcId: String,
    val title: String,
    val description: String,
    val startNodeId: String,
    val requiredFriendshipLevel: Int = 1,
    val nodesJson: String = "[]"
)

@Dao
interface DialogueDao {

    @Query("SELECT * FROM dialogue ORDER BY id")
    fun getAllDialogues(): Flow<List<DialogueEntity>>

    @Query("SELECT * FROM dialogue WHERE id = :dialogueId")
    fun getDialogueById(dialogueId: String): Flow<DialogueEntity?>

    @Query("SELECT * FROM dialogue WHERE npcId = :npcId ORDER BY id LIMIT 1")
    fun getDialogueByNpcId(npcId: String): Flow<DialogueEntity?>

    @Query("SELECT COUNT(*) FROM dialogue")
    suspend fun countDialogues(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(dialogues: List<DialogueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dialogue: DialogueEntity)

    @Query("DELETE FROM dialogue")
    suspend fun clear()
}
