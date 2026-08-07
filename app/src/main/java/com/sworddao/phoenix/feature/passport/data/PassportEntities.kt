package com.sworddao.phoenix.feature.passport.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passport")
data class PassportEntity(
    @PrimaryKey val id: String,
    val playerName: String,
    val createdAt: Long,
    val lastUpdated: Long,
    val totalStamps: Int = 0,
    val totalCollectibles: Int = 0,
    val totalDiscoveries: Int = 0,
    val currentChapter: Int = 1,
)

@Entity(tableName = "passport_region")
data class PassportRegionEntity(
    @PrimaryKey val regionId: String,
    val regionName: String,
    val regionNameCn: String,
    val isDiscovered: Boolean = false,
    val isCompleted: Boolean = false,
    val stampEarned: Boolean = false,
    val stampRarity: String = "BRONZE",
    val discoveredAt: Long? = null,
    val completedAt: Long? = null,
    val completionPercentage: Float = 0f,
    val vocabularyLearned: Int = 0,
    val friendshipsMade: Int = 0,
    val questsCompleted: Int = 0,
    val collectiblesFound: Int = 0,
    val collectiblesTotal: Int = 0,
    val totalPlayTimeMinutes: Int = 0,
    val notes: String = "",
)

@Entity(tableName = "passport_collectible")
data class CollectibleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameCn: String,
    val category: String,
    val rarity: String = "COMMON",
    val source: String = "EXPLORATION",
    val description: String,
    val culturalNote: String? = null,
    val regionId: String,
    val isCollected: Boolean = false,
    val collectedAt: Long? = null,
    val isHidden: Boolean = false,
    val isDisplayed: Boolean = false,
    val displayLocation: String? = null,
    val tradeable: Boolean = true,
    val xpValue: Int = 10,
)

@Entity(tableName = "passport_event")
data class PassportEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String,
    val regionId: String? = null,
    val timestamp: Long,
    val metadata: String = "{}",
)

@Entity(tableName = "passport_achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Float = 0f,
    val requiredCount: Int = 1,
    val currentCount: Int = 0,
    val category: String = "",
    val xpReward: Int = 100,
)

@Entity(tableName = "passport_entry")
data class PassportEntryEntity(
    @PrimaryKey val id: String,
    val regionId: String,
    val type: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val metadata: String = "{}",
)
