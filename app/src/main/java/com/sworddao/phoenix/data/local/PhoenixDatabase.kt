package com.sworddao.phoenix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sworddao.phoenix.feature.friendship.data.ConversationMemoryEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipDao
import com.sworddao.phoenix.feature.friendship.data.FriendshipEventEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipEntity

@Database(
    entities = [
        PlaceholderEntity::class,
        FriendshipEntity::class,
        ConversationMemoryEntity::class,
        FriendshipEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PhoenixDatabase : RoomDatabase() {
    abstract fun friendshipDao(): FriendshipDao
}
