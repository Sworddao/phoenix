package com.sworddao.phoenix.feature.gameplay.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: String,
    val gameProgressJson: String,
)

@Entity(tableName = "session_summary")
data class SessionSummaryEntity(
    @PrimaryKey val id: String,
    val sessionSummaryJson: String,
)

@Dao
interface GameProgressDao {

    @Query("SELECT * FROM game_progress WHERE id = 'all'")
    fun getGameProgressDoc(): Flow<GameProgressEntity?>

    @Query("SELECT * FROM game_progress WHERE id = 'all'")
    suspend fun getGameProgressDocOnce(): GameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameProgressDoc(entity: GameProgressEntity)

    @Query("SELECT * FROM session_summary WHERE id = 'all'")
    fun getSessionSummaryDoc(): Flow<SessionSummaryEntity?>

    @Query("SELECT * FROM session_summary WHERE id = 'all'")
    suspend fun getSessionSummaryDocOnce(): SessionSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionSummaryDoc(entity: SessionSummaryEntity)
}
