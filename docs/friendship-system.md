# Friendship System

## Overview

The Friendship System tracks and manages the evolving relationships between the player and NPCs in Phoenix. It provides persistent relationship progression, conversation memory, and event tracking that forms the foundation for quests, rewards, dialogue branching, and story progression.

## Architecture

```
feature/friendship/
├── data/
│   ├── FriendshipModels.kt          # Domain data classes
│   ├── FriendshipEntities.kt        # Room persistence entities
│   ├── FriendshipDao.kt             # Room DAO for database operations
│   ├── FriendshipMappers.kt         # Entity <-> Domain mappers
│   ├── RoomFriendshipRepository.kt  # Production Room-backed implementation
│   └── MockFriendshipRepository.kt  # In-memory implementation for tests/dev
├── domain/
│   └── FriendshipRepository.kt      # Repository interface
├── ui/
│   ├── FriendshipCard.kt            # NPC friendship summary card
│   ├── FriendshipProgressBar.kt     # XP progress visualization
│   ├── RelationshipBadge.kt         # Level badge component
│   ├── LevelUpDialog.kt             # Level-up celebration dialog
│   ├── ConversationHistoryCard.kt   # Conversation memory card
│   └── NPCProfileScreen.kt          # Full NPC profile screen
├── viewmodel/
│   └── FriendshipViewModel.kt       # ViewModel with StateFlow
└── di/
    └── FriendshipModule.kt          # Hilt dependency injection
```

## Data Flow

```
Player interacts with NPC
    ↓
Dialogue system triggers FriendshipAction
    ↓
FriendshipRepository processes action
    ↓
FriendshipState updated (XP, level, history)
    ↓
Room database persisted
    ↓
FriendshipViewModel emits new state
    ↓
UI components recompose
    ↓
Level-up dialog shown if level changed
```

## Friendship Levels

| Level | XP Required | Display Title |
|-------|-------------|---------------|
| 1 | 0 | Stranger |
| 2 | 100 | Visitor |
| 3 | 300 | Friend |
| 4 | 600 | Close Friend |
| 5 | 1000 | Trusted Friend |
| 6 | 1500 | Family |

## Data Models

### FriendshipState

Tracks the player's relationship with a specific NPC.

```kotlin
data class FriendshipState(
    val npcId: String,
    val friendshipXp: Int = 0,
    val friendshipLevel: FriendshipLevel = FriendshipLevel.STRANGER,
    val totalConversations: Int = 0,
    val firstMeetingTimestamp: Long = System.currentTimeMillis(),
    val lastInteractionTimestamp: Long = 0L,
    val unlockedTopics: List<String> = emptyList(),
    val recentGifts: List<GiftRecord> = emptyList(),
    val completedQuests: List<String> = emptyList()
)
```

### ConversationMemory

Records a single conversation with an NPC.

```kotlin
data class ConversationMemory(
    val id: String,
    val npcId: String,
    val dialogueId: String,
    val dialogueTitle: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val topicsDiscussed: List<String>,
    val xpGained: Int,
    val choicesSummary: List<String>
)
```

### FriendshipEvent

Tracks significant relationship events.

```kotlin
data class FriendshipEvent(
    val id: String,
    val type: FriendshipEventType,
    val npcId: String,
    val description: String,
    val xpChange: Int,
    val timestamp: Long,
    val metadata: Map<String, String>
)
```

### FriendshipEventType

```kotlin
enum class FriendshipEventType {
    CONVERSATION,
    GIFT,
    QUEST_COMPLETE,
    LEVEL_UP,
    TOPIC_UNLOCK,
    FIRST_MEETING,
    DAILY_VISIT
}
```

## Repository Interface

```kotlin
interface FriendshipRepository {
    fun getFriendshipState(npcId: String): Flow<FriendshipState?>
    fun getAllFriendshipStates(): Flow<List<FriendshipState>>
    suspend fun addFriendshipXp(npcId: String, xpAmount: Int): FriendshipState?
    suspend fun recordConversation(
        npcId: String,
        dialogueId: String,
        dialogueTitle: String,
        xpGained: Int,
        topicsDiscussed: List<String>,
        choicesSummary: List<String>
    )
    fun getConversationHistory(npcId: String): Flow<List<ConversationMemory>>
    fun getFriendshipEvents(npcId: String): Flow<List<FriendshipEvent>>
    suspend fun unlockTopic(npcId: String, topic: String)
    suspend fun initializeFriendship(npcId: String)
}
```

## ViewModel

The `FriendshipViewModel` manages friendship UI state:

```kotlin
data class FriendshipUiState(
    val friendshipState: FriendshipState?,
    val conversationHistory: List<ConversationMemory>,
    val friendshipEvents: List<FriendshipEvent>,
    val allFriendshipStates: List<FriendshipState>,
    val isLoading: Boolean,
    val error: String?,
    val showLevelUpDialog: Boolean,
    val levelUpInfo: LevelUpInfo?
)
```

### Methods

- `selectNpc(npcId)` — Load friendship data for an NPC
- `addXp(npcId, xpAmount)` — Add friendship XP and check for level up
- `recordConversation(...)` — Record a completed conversation
- `dismissLevelUpDialog()` — Close the level-up celebration

## UI Components

### FriendshipCard

Summary card showing NPC avatar, name, occupation, friendship level, and XP. Tappable to open NPC profile.

### FriendshipProgressBar

Visual progress bar showing current XP, level, and progress to next level.

### RelationshipBadge

Small colored badge displaying the current friendship level title.

### LevelUpDialog

Celebration dialog shown when friendship level increases. Displays old and new level badges with congratulatory message.

### ConversationHistoryCard

Card showing a past conversation with title, date, XP gained, and topics discussed.

### NPCProfileScreen

Full NPC profile screen with:
- NPC portrait and personality
- Friendship progress bar
- Stats (conversations, XP, topics)
- Start Conversation button
- Conversation history list

## Persistence

Friendship data is persisted locally using Room database:

- `friendship_state` table — NPC friendship states
- `conversation_memory` table — Conversation history
- `friendship_event` table — Relationship events

All data is stored locally and available offline.

## Dialogue Integration

The friendship system integrates with the dialogue engine through `FriendshipAction`:

```kotlin
data class FriendshipAction(
    val type: FriendshipActionType,
    val targetNpcId: String,
    val value: String
)
```

When a dialogue completes, the `DialogueViewModel` can trigger friendship actions:
- `ADD_XP` — Increase friendship XP
- `UNLOCK_TOPIC` — Unlock new conversation topics
- `COMPLETE_QUEST` — Record quest completion
- `RECORD_GIFT` — Track gift giving
- `RECORD_CONVERSATION` — Save conversation memory

## Future Extensibility

The system is designed for easy expansion:

1. **Gift System** — Add gift-giving mechanics with `GiftRecord`
2. **Quest Integration** — Use `completedQuests` for quest tracking
3. **Shop System** — Unlock shops based on friendship level
4. **Story Branching** — Use `unlockedTopics` for dialogue conditions
5. **Daily Rewards** — Track `DAILY_VISIT` events
6. **Achievements** — Trigger achievements from friendship milestones
7. **Multi-NPC Events** — Combine friendship states for group events

## Integration Points

The friendship system integrates with:

- **NPC Framework** — Uses NPC data for display and context
- **Dialogue System** — Processes friendship actions from conversations
- **Navigation** — Routes to NPCProfileScreen
- **Village** — NPC markers show friendship context
- **Future Quest System** — Quest requirements based on friendship levels
- **Future Shop System** — Shop access based on friendship levels
