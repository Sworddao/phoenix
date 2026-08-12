# NPC System

## Overview

The NPC System is the foundation for all character interactions in Phoenix. NPCs are the primary teachers, providing vocabulary, culture, and conversation practice through natural interactions.

## Architecture

```
feature/npc/
├── data/
│   ├── NpcModels.kt          # Data classes and enums
│   ├── NpcEntities.kt        # Room entity and DAO (`npc` table)
│   ├── NpcMappers.kt         # Entity ↔ domain mapping
│   ├── RoomNpcRepository.kt  # Room-backed implementation
│   └── MockNpcRepository.kt  # Mock implementation (tests/dev)
├── domain/
│   └── NpcRepository.kt      # Repository interface
├── ui/
│   ├── NpcCard.kt            # NPC summary card
│   ├── NpcMarker.kt          # Village map marker
│   └── NpcInfoDialog.kt      # NPC info dialog
├── viewmodel/
│   └── NpcViewModel.kt       # ViewModel with StateFlow
└── di/
    └── NpcModule.kt          # Hilt dependency injection
```

## Data Models

### Npc

Core NPC data class with all character information.

```kotlin
data class Npc(
    val id: String,
    val displayName: String,
    val occupation: String,
    val personality: String,
    val currentLocation: String,
    val friendshipXp: Int = 0,
    val schedule: NpcSchedule,
    val avatarEmoji: String,
    val idleAnimationState: IdleAnimationState,
    val interactionAvailability: InteractionAvailability,
    val unlockRequirements: String?,
    val vocabularyCategories: List<String>,
    val dialogueReferences: List<String>,
    val shortDescription: String
)
```

### FriendshipLevel

Enum defining relationship progression.

```kotlin
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
    FAMILY(displayTitle = "Family", xpThreshold = 1500, level = 6)
}
```

### FriendshipProgress

Computed progress toward next friendship level.

```kotlin
data class FriendshipProgress(
    val level: FriendshipLevel,
    val currentXp: Int,
    val nextLevel: FriendshipLevel?,
    val progressPercentage: Float
)
```

### NpcSchedule

NPC daily schedule with location tracking.

```kotlin
data class NpcSchedule(
    val entries: List<NpcScheduleEntry>
) {
    fun getLocationAtTime(timeOfDay: TimeOfDay): NpcScheduleEntry?
}
```

## NPC Characters

### Grandma Mei

- **Occupation:** Retired Baker
- **Personality:** Warm, patient, and funny
- **Location:** Grandma Mei's Bakery
- **Teaches:** Greetings, Family, Food, Daily Conversation
- **Friendship Rewards:** Homemade Dumplings, Family Vocabulary Pack, Special Story

### Restaurant Owner Lin

- **Occupation:** Chef
- **Personality:** Passionate about food
- **Location:** Restaurant
- **Teaches:** Food, Ordering, Ingredients, Cooking, Payment, Restaurant Etiquette

### Taxi Driver Chen

- **Occupation:** Taxi Driver
- **Personality:** Friendly and talkative
- **Location:** Village Square
- **Teaches:** Directions, Numbers, Time, Travel, Weather

### University Student Wei

- **Occupation:** University Student
- **Personality:** Curious and energetic
- **Location:** Tea House
- **Teaches:** Friends, Technology, Music, Gaming, Campus Life

## Persistence

The NPC catalog is persisted in Room (`npc` table, `NpcEntity` with schedule/category/reference lists stored as `RoomJson`). `RoomNpcRepository` seeds the catalog from `NpcSeedData` on first access and tracks persisted friendship XP; the in-memory `MockNpcRepository` remains available for tests and development.

## Friendship Integration

Each NPC tracks friendship through:

- **XP** — Experience points from conversations
- **Level** — 6-tier progression system
- **History** — Conversation and event tracking
- **Memory** — Remembered interactions

See [Friendship System](friendship-system.md) for details.

## Dialogue Integration

NPCs connect to dialogues through:

- `dialogueReferences` — List of available dialogue IDs
- `requiredFriendshipLevel` — Minimum level for dialogue access
- `vocabularyCategories` — Topics the NPC teaches

See [Dialogue System](dialogue-system.md) for details.

## UI Components

### NpcMarker

Circular avatar marker placed on the village map. Shows emoji and last name.

### NpcCard

Summary card showing NPC name, occupation, and friendship level.

### NpcInfoDialog

Detailed dialog showing personality, description, friendship progress, and vocabulary.

## Future Extensibility

The system supports:

1. **New NPCs** — Add to NpcSeedData
2. **Schedule System** — NPCs move based on time of day
3. **Animation States** — Idle animations for different activities
4. **Availability System** — NPCs can be busy or sleeping
5. **Unlock Requirements** — NPCs unlock as player progresses
6. **Quest Givers** — NPCs assign quests at certain friendship levels
7. **Shop Keepers** — NPCs run shops at higher friendship levels
