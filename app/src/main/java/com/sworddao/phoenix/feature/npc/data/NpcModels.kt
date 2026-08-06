package com.sworddao.phoenix.feature.npc.data

enum class FriendshipLevel(
    val displayTitle: String,
    val xpThreshold: Int,
    val level: Int
) {
    STRANGER(displayTitle = "Stranger", xpThreshold = 0, level = 1),
    VISITOR(displayTitle = "Visitor", xpThreshold = 100, level = 2),
    FRIEND(displayTitle = "Friend", xpThreshold = 300, level = 3),
    CLOSE_FRIEND(displayTitle = "Close Friend", xpThreshold = 600, level = 4),
    TRUSTED_FRIEND(displayTitle = "Trusted Friend", xpThreshold = 1000, level = 5),
    FAMILY(displayTitle = "Family", xpThreshold = 1500, level = 6);

    companion object {
        fun fromXp(xp: Int): FriendshipLevel {
            return entries.reversed().firstOrNull { xp >= it.xpThreshold } ?: STRANGER
        }
    }
}

data class FriendshipProgress(
    val level: FriendshipLevel,
    val currentXp: Int,
    val nextLevel: FriendshipLevel?,
    val progressPercentage: Float
)

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

data class NpcScheduleEntry(
    val timeOfDay: TimeOfDay,
    val locationName: String,
    val description: String
)

data class NpcSchedule(
    val entries: List<NpcScheduleEntry>
) {
    fun getLocationAtTime(timeOfDay: TimeOfDay): NpcScheduleEntry? {
        return entries.firstOrNull { it.timeOfDay == timeOfDay }
    }
}

enum class IdleAnimationState {
    IDLE,
    TALKING,
    WORKING,
    EATING,
    WALKING,
    SITTING,
    STANDING
}

enum class InteractionAvailability {
    AVAILABLE,
    BUSY,
    SLEEPING,
    NOT_IN_LOCATION
}

data class Npc(
    val id: String,
    val displayName: String,
    val occupation: String,
    val personality: String,
    val currentLocation: String,
    val friendshipXp: Int = 0,
    val schedule: NpcSchedule,
    val avatarEmoji: String,
    val idleAnimationState: IdleAnimationState = IdleAnimationState.IDLE,
    val interactionAvailability: InteractionAvailability = InteractionAvailability.AVAILABLE,
    val unlockRequirements: String? = null,
    val vocabularyCategories: List<String> = emptyList(),
    val dialogueReferences: List<String> = emptyList(),
    val shortDescription: String = ""
) {
    val friendshipLevel: FriendshipLevel
        get() = FriendshipLevel.fromXp(friendshipXp)

    val friendshipProgress: FriendshipProgress
        get() {
            val currentLevel = friendshipLevel
            val nextLevel = when (currentLevel) {
                FriendshipLevel.FAMILY -> null
                else -> FriendshipLevel.entries[currentLevel.ordinal + 1]
            }
            val xpInCurrentLevel = friendshipXp - currentLevel.xpThreshold
            val xpForNextLevel = nextLevel?.let { it.xpThreshold - currentLevel.xpThreshold } ?: 1
            val progress = if (xpForNextLevel > 0) {
                (xpInCurrentLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
            } else {
                1f
            }
            return FriendshipProgress(
                level = currentLevel,
                currentXp = friendshipXp,
                nextLevel = nextLevel,
                progressPercentage = progress
            )
        }
}
