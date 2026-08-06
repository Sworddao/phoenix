package com.sworddao.phoenix.feature.world.data

import kotlinx.serialization.Serializable

@Serializable
enum class RegionStatus {
    LOCKED,
    AVAILABLE,
    VISITED,
    CURRENT,
    COMPLETED,
}

@Serializable
enum class TravelMethod {
    WALKING,
    BUS,
    TRAIN,
    HIGH_SPEED_RAIL,
    TAXI,
    BICYCLE,
    BOAT,
}

@Serializable
enum class LandmarkType {
    TEMPLE,
    MARKET,
    RESTAURANT,
    PARK,
    MUSEUM,
    SHOP,
    STATION,
    HOTEL,
    SCHOOL,
    HOSPITAL,
    LIBRARY,
    GARDEN,
    BRIDGE,
    TOWER,
    WALL,
    PALACE,
    VILLAGE,
    CITY_GATE,
    RIVER,
    MOUNTAIN,
}

@Serializable
enum class CollectibleType {
    TEA,
    BAMBOO,
    POSTCARD,
    LANTERN,
    BOOK,
    INSTRUMENT,
    SOUVENIR,
    FESTIVAL_TICKET,
    PHOTOGRAPH,
    RECIPE_CARD,
    VOICE_RECORDING,
    STORY_SCROLL,
    VOCABULARY_CARD,
    STAMP,
    COIN,
    SCROLL,
}

@Serializable
data class WorldRegion(
    val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val status: RegionStatus = RegionStatus.LOCKED,
    val order: Int,
    val chapter: Int,
    val unlockRequirements: UnlockRequirement = UnlockRequirement(),
    val connections: List<String> = emptyList(),
    val travelMethods: List<TravelMethod> = emptyList(),
    val landmarks: List<Landmark> = emptyList(),
    val npcIds: List<String> = emptyList(),
    val questIds: List<String> = emptyList(),
    val collectibles: List<CollectibleLocation> = emptyList(),
    val completionPercentage: Float = 0f,
    val musicTrack: String? = null,
    val ambienceTrack: String? = null,
    val mapPositionX: Float = 0f,
    val mapPositionY: Float = 0f,
    val color: Long = 0xFF4CAF50,
    val icon: String = "📍",
) {
    val isUnlocked: Boolean
        get() = status != RegionStatus.LOCKED

    val isCompleted: Boolean
        get() = status == RegionStatus.COMPLETED

    val isCurrent: Boolean
        get() = status == RegionStatus.CURRENT
}

@Serializable
data class WorldLocation(
    val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val regionId: String,
    val type: LandmarkType,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val npcIds: List<String> = emptyList(),
    val questIds: List<String> = emptyList(),
    val isDiscovered: Boolean = false,
    val isAccessible: Boolean = true,
)

@Serializable
data class UnlockRequirement(
    val questIds: List<String> = emptyList(),
    val npcFriendshipLevel: Int = 0,
    val requiredVocabularyCount: Int = 0,
    val requiredLevel: Int = 0,
    val requiredRegions: List<String> = emptyList(),
    val requiredFriends: List<String> = emptyList(),
) {
    val hasRequirements: Boolean
        get() = questIds.isNotEmpty() ||
            npcFriendshipLevel > 0 ||
            requiredVocabularyCount > 0 ||
            requiredLevel > 0 ||
            requiredRegions.isNotEmpty() ||
            requiredFriends.isNotEmpty()
}

@Serializable
data class RegionProgress(
    val regionId: String,
    val status: RegionStatus,
    val completionPercentage: Float = 0f,
    val discoveredLocations: List<String> = emptyList(),
    val completedQuests: List<String> = emptyList(),
    val collectedItems: List<String> = emptyList(),
    val visitedNpcs: List<String> = emptyList(),
    val unlockedFastTravel: Boolean = false,
    val firstVisitedAt: Long? = null,
    val lastVisitedAt: Long? = null,
    val totalPlayTimeMinutes: Int = 0,
)

@Serializable
data class ExplorationProgress(
    val totalRegions: Int = 0,
    val completedRegions: Int = 0,
    val currentRegionId: String? = null,
    val totalLocations: Int = 0,
    val discoveredLocations: Int = 0,
    val totalCollectibles: Int = 0,
    val collectedItems: Int = 0,
    val completionPercentage: Float = 0f,
    val regions: Map<String, RegionProgress> = emptyMap(),
) {
    val regionsRemaining: Int
        get() = totalRegions - completedRegions

    val locationsRemaining: Int
        get() = totalLocations - discoveredLocations
}

@Serializable
data class Landmark(
    val id: String,
    val name: String,
    val nameCn: String,
    val type: LandmarkType,
    val description: String,
    val regionId: String,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isDiscovered: Boolean = false,
    val isInteractable: Boolean = true,
    val npcIds: List<String> = emptyList(),
    val questIds: List<String> = emptyList(),
)

@Serializable
data class CollectibleLocation(
    val id: String,
    val name: String,
    val type: CollectibleType,
    val regionId: String,
    val locationId: String? = null,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val isCollected: Boolean = false,
    val isHidden: Boolean = false,
    val description: String? = null,
    val culturalNote: String? = null,
)

@Serializable
data class RegionConnection(
    val fromRegionId: String,
    val toRegionId: String,
    val travelMethod: TravelMethod,
    val travelTimeMinutes: Int = 0,
    val isUnlocked: Boolean = false,
    val description: String? = null,
)

sealed class WorldResult {
    data class Success(val message: String) : WorldResult()
    data class Error(val message: String) : WorldResult()
    data class TravelStarted(val fromRegionId: String, val toRegionId: String) : WorldResult()
    data class TravelCompleted(val regionId: String) : WorldResult()
    data class RegionUnlocked(val regionId: String) : WorldResult()
    data class LocationDiscovered(val locationId: String) : WorldResult()
    data class CollectibleFound(val collectibleId: String) : WorldResult()
}
