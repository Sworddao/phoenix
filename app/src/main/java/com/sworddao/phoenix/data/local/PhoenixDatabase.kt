package com.sworddao.phoenix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhoenixDatabase : RoomDatabase()
