package com.sworddao.phoenix.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Small key/value metadata store used for seed-once flags and
 * cross-session counters that have no dedicated table.
 */
@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface AppMetadataDao {

    @Query("SELECT value FROM app_metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(entity: AppMetadataEntity)

    @Query("DELETE FROM app_metadata WHERE `key` = :key")
    suspend fun delete(key: String)
}
