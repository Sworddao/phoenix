package com.sworddao.phoenix.feature.world.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_region")
data class WorldRegionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val status: String,
    val order: Int,
    val chapter: Int,
    val unlockRequirementsJson: String,
    val connectionsJson: String,
    val travelMethodsJson: String,
    val npcIdsJson: String,
    val questIdsJson: String,
    val completionPercentage: Float = 0f,
    val musicTrack: String? = null,
    val ambienceTrack: String? = null,
    val mapPositionX: Float = 0f,
    val mapPositionY: Float = 0f,
    val color: Long = 0xFF4CAF50,
    val icon: String = "📍",
)

@Entity(tableName = "world_region_progress")
data class WorldRegionProgressEntity(
    @PrimaryKey val regionId: String,
    val status: String,
    val completionPercentage: Float = 0f,
    val discoveredLocationsJson: String = "[]",
    val completedQuestsJson: String = "[]",
    val collectedItemsJson: String = "[]",
    val visitedNpcsJson: String = "[]",
    val unlockedFastTravel: Boolean = false,
    val firstVisitedAt: Long? = null,
    val lastVisitedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
)

@Entity(tableName = "world_connection")
data class WorldConnectionEntity(
    @PrimaryKey val id: String,
    val fromRegionId: String,
    val toRegionId: String,
    val travelMethod: String,
    val travelTimeMinutes: Int = 0,
    val isUnlocked: Boolean = false,
    val description: String? = null,
)

@Entity(tableName = "world_location")
data class WorldLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val regionId: String,
    val type: String,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val npcIdsJson: String = "[]",
    val questIdsJson: String = "[]",
    val isDiscovered: Boolean = false,
    val isAccessible: Boolean = true,
)

@Entity(tableName = "world_landmark")
data class WorldLandmarkEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameCn: String,
    val type: String,
    val description: String,
    val regionId: String,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isDiscovered: Boolean = false,
    val isInteractable: Boolean = true,
    val npcIdsJson: String = "[]",
    val questIdsJson: String = "[]",
)

@Entity(tableName = "world_collectible")
data class WorldCollectibleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val regionId: String,
    val locationId: String? = null,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isCollected: Boolean = false,
    val isHidden: Boolean = false,
    val description: String? = null,
    val culturalNote: String? = null,
)
