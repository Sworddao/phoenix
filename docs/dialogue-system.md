# Dialogue System

## Overview

The Dialogue System is the foundation for all NPC interactions in Phoenix. It provides a reusable, data-driven engine for branching conversations with player choices.

## Architecture

```
feature/dialogue/
├── data/
│   ├── DialogueModels.kt          # Data classes and enums
│   ├── DialogueEntities.kt        # Room entity and DAO (`dialogue` table)
│   ├── DialogueMappers.kt         # Entity ↔ domain mapping
│   ├── DialogueFlow.kt            # Pure conversation traversal
│   ├── RoomDialogueRepository.kt  # Room-backed implementation
│   └── MockDialogueRepository.kt  # Mock implementation (tests/dev)
├── domain/
│   └── DialogueRepository.kt      # Repository interface
├── ui/
│   ├── DialogueScreen.kt          # Main dialogue screen
│   ├── DialogueBubble.kt          # Chat bubble component
│   ├── PlayerChoiceCard.kt        # Player choice component
│   ├── DialogueHeader.kt          # NPC header component
│   └── ConversationCompleteCard.kt # Completion card
├── viewmodel/
│   └── DialogueViewModel.kt       # ViewModel with StateFlow
└── di/
    └── DialogueModule.kt          # Hilt dependency injection
```

## Data Flow

```
User taps NPC marker
    ↓
Navigation to NPCProfileScreen
    ↓
User taps "Start Conversation"
    ↓
Navigation to DialogueScreen
    ↓
DialogueViewModel loads dialogue
    ↓
Repository starts conversation
    ↓
First node displayed
    ↓
User selects choice
    ↓
Repository processes choice
    ↓
Next node displayed
    ↓
... (repeats until conversation ends)
    ↓
ConversationCompleteCard shown
    ↓
Friendship actions processed
    ↓
User taps "Continue Adventure"
    ↓
Return to village
```

## Data Models

### Dialogue

Represents a complete conversation with an NPC.

```kotlin
data class Dialogue(
    val id: String,
    val npcId: String,
    val title: String,
    val description: String,
    val startNodeId: String,
    val nodes: List<DialogueNode>,
    val requiredFriendshipLevel: Int = 1
)
```

### DialogueNode

A single step in the conversation.

```kotlin
data class DialogueNode(
    val id: String,
    val type: DialogueNodeType,  // NPC_SPEAKS, PLAYER_CHOOSES, CONVERSATION_END
    val speaker: Speaker,        // NPC, PLAYER, NARRATOR
    val speakerName: String,
    val text: String,
    val pinyin: String,
    val hanzi: String,
    val choices: List<DialogueChoice>,
    val nextNodeId: String?,
    val conditions: List<DialogueCondition>,
    val actions: List<DialogueAction>
)
```

### DialogueChoice

A player response option.

```kotlin
data class DialogueChoice(
    val id: String,
    val text: String,
    val pinyin: String,
    val nextNodeId: String,
    val conditions: List<DialogueCondition>,
    val actions: List<DialogueAction>
)
```

### DialogueAction

An action triggered during conversation.

```kotlin
data class DialogueAction(
    val type: ActionType,  // ADD_FRIENDSHIP_XP, UNLOCK_VOCABULARY, PRACTICE_SPEAKING, etc.
    val targetId: String,
    val value: String
)
```

Action types include:

- `ADD_FRIENDSHIP_XP` — Increases NPC friendship XP
- `UNLOCK_VOCABULARY` — Unlocks vocabulary categories
- `COMPLETE_QUEST` — Records quest completion
- `GIVE_ITEM` — Records gift giving
- `PRACTICE_SPEAKING` — Unlocks speaking exercises (comma-separated exercise ids in `value`) and surfaces the practice prompt on the completion card
- `PRACTICE_LISTENING` (`@SerialName("practice_listening")`) — Unlocks listening exercises (comma-separated exercise ids in `value`) and surfaces the listening practice button on the completion card

### DialogueCondition

A condition that must be met for a choice or node.

```kotlin
data class DialogueCondition(
    val type: ConditionType,  // FRIENDSHIP_LEVEL, HAS_ITEM, etc.
    val targetId: String,
    val value: String
)
```

## ViewModel

The `DialogueViewModel` manages conversation state:

```kotlin
data class DialogueUiState(
    val dialogue: Dialogue?,
    val currentNode: DialogueNode?,
    val currentSpeaker: String,
    val history: List<DialogueHistoryEntry>,
    val availableChoices: List<DialogueChoice>,
    val isConversationComplete: Boolean,
    val isPracticeAvailable: Boolean,
    val isListeningPracticeAvailable: Boolean,
    val completedActions: List<DialogueAction>,
    val isLoading: Boolean,
    val error: String?
)
```

### Methods

- `selectChoice(choiceId)` — Process player choice
- `advanceDialogue()` — Move to next node
- `dismissError()` — Clear error state

The ViewModel injects a `PronunciationRepository` to unlock exercises for `PRACTICE_SPEAKING` actions and a `ListeningRepository` to unlock exercises for `PRACTICE_LISTENING` actions. When a completed conversation offered speaking practice, the completion card shows a "练习说" button that navigates to the pronunciation screen via `onPractice`; when it offered listening practice, `isListeningPracticeAvailable` is true and the completion card shows a "练习听" button navigating to the listening screen.

## Repository

The `DialogueRepository` interface provides:

```kotlin
interface DialogueRepository {
    fun getDialogueByNpcId(npcId: String): Flow<Dialogue?>
    fun getAllDialogues(): Flow<List<Dialogue>>
    suspend fun startConversation(dialogueId: String): DialogueResult
    suspend fun selectChoice(dialogueId: String, choiceId: String): DialogueResult
    suspend fun advanceDialogue(dialogueId: String): DialogueResult
}
```

The dialogue catalog is persisted in Room (`dialogue` table, `DialogueEntity` with nodes stored as `RoomJson`). `RoomDialogueRepository` seeds the catalog from `DialogueSeedData` on first access and runs conversation traversal through the shared pure `DialogueFlow`. Active conversation state remains transient in-memory by design — only the catalog is persisted. `MockDialogueRepository` retains the same behavior for tests and development.

### DialogueResult

```kotlin
sealed class DialogueResult {
    data class NodeLoaded(...) : DialogueResult()
    data class ConversationEnded(...) : DialogueResult()
    data class Error(val message: String) : DialogueResult()
}
```

## UI Components

### DialogueScreen

Main screen composable that orchestrates the conversation.

### DialogueBubble

Chat bubble for NPC and player messages with pinyin support.

### PlayerChoiceCard

Selectable card for player response options.

### DialogueHeader

Header showing NPC avatar, name, and occupation.

### ConversationCompleteCard

Completion card shown at conversation end with friendship XP summary.

## Friendship Integration

The dialogue system integrates with the Friendship System through actions:

- `ADD_FRIENDSHIP_XP` — Increases NPC friendship XP
- `UNLOCK_VOCABULARY` — Unlocks vocabulary categories
- `COMPLETE_QUEST` — Records quest completion
- `GIVE_ITEM` — Records gift giving

When a conversation completes, the `DialogueViewModel` processes completed actions and triggers friendship updates.

## Pronunciation Integration

Conversations can offer speaking practice after completion:

- Dialogue end nodes may declare a `PRACTICE_SPEAKING` action with a comma-separated list of exercise ids (e.g. `pron_ex_dlg_hao_chi,pron_ex_dlg_meet`)
- `DialogueViewModel` unlocks those exercises via `PronunciationRepository`
- `ConversationCompleteCard` renders a practice button when `isPracticeAvailable` is true
- `DialogueScreen` forwards the callback as `onPractice` to navigate to `Screen.Pronunciation.createRoute()`
- Grandma Mei's conversation includes practice exercises that reinforce the newly learned dialogue phrases

## Listening Integration

Conversations can also offer listening practice after completion:

- Dialogue end nodes may declare a `PRACTICE_LISTENING` action with a comma-separated list of exercise ids (e.g. `listen_ex_greet_hello,listen_ex_greet_thanks`)
- `DialogueViewModel` unlocks those exercises via the injected `ListeningRepository.unlockExercise`
- `ConversationCompleteCard` renders a "练习听" button when `isListeningPracticeAvailable` is true
- Grandma Mei's conversation end node unlocks `listen_ex_greet_hello` and `listen_ex_greet_thanks`

## Future Extensibility

The system is designed for easy expansion:

1. **New NPCs** — Add dialogue to DialogueSeedData
2. **Conditions** — Implement condition checking in repository
3. **Actions** — Add new action types for game mechanics
4. **Audio** — Add audio playback for NPC speech
5. **Hanzi** — Toggle Chinese character display
6. **Branching Stories** — Complex narrative paths based on friendship
7. **Quest Triggers** — Start quests from dialogue actions
