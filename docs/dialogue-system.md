# Dialogue System

## Overview

The Dialogue System is the foundation for all NPC interactions in Phoenix. It provides a reusable, data-driven engine for branching conversations with player choices.

## Architecture

```
feature/dialogue/
├── data/
│   ├── DialogueModels.kt          # Data classes and enums
│   └── MockDialogueRepository.kt  # Mock implementation
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
    val type: ActionType,  // ADD_FRIENDSHIP_XP, UNLOCK_VOCABULARY, etc.
    val targetId: String,
    val value: String
)
```

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
    val completedActions: List<DialogueAction>,
    val isLoading: Boolean,
    val error: String?
)
```

### Methods

- `selectChoice(choiceId)` — Process player choice
- `advanceDialogue()` — Move to next node
- `dismissError()` — Clear error state

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

Completion card shown at conversation end.

## Future Extensibility

The system is designed for easy expansion:

1. **New NPCs** — Add dialogue to MockDialogueRepository
2. **Conditions** — Implement condition checking in repository
3. **Actions** — Add new action types for game mechanics
4. **Friendship** — Integrate with NPC friendship system
5. **Quests** — Trigger quests from dialogue actions
6. **Vocabulary** — Unlock vocabulary from conversations
7. **Audio** — Add audio playback for NPC speech
8. **Hanzi** — Toggle Chinese character display

## Integration

The dialogue system integrates with:

- **NPC Framework** — Uses NPC data for speakers
- **Navigation** — Routes to DialogueScreen
- **Village** — NPC markers launch dialogues
- **Friendship** — Actions can modify friendship XP
- **Vocabulary** — Actions can unlock vocabulary categories
