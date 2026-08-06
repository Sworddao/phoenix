# Quest System

## Overview

The Quest System provides a scalable architecture for story-driven quests that teach vocabulary naturally through context and interaction. Quests are the primary progression mechanism, guiding players through the game world while introducing new vocabulary and cultural concepts.

## Architecture

### Data Models

#### QuestType
Enum defining quest categories:
- `CONVERSATION` — Dialogues with NPCs
- `LISTENING` — Audio comprehension
- `SPEAKING` — Pronunciation practice
- `EXPLORATION` — Location discovery
- `MEMORY` — Vocabulary recall
- `PRONUNCIATION` — Pronunciation drills
- `STORY` — Narrative quests
- `MINI_GAME` — Skill-based games
- `PHOTOGRAPHY` — Photo collection
- `COLLECTING` — Item gathering
- `DAILY` — Repeatable daily tasks

#### QuestDifficulty
Enum with experience rewards:
- `EASY` (10 XP) — Basic vocabulary
- `MEDIUM` (20 XP) — Intermediate concepts
- `HARD` (30 XP) — Advanced usage
- `EXPERT` (50 XP) — Mastery challenges

#### QuestStatus
Enum tracking quest progression:
- `LOCKED` — Not yet available
- `AVAILABLE` — Ready to start
- `ACTIVE` — In progress
- `COMPLETED` — Finished

#### QuestCategory
Enum for quest classification:
- `DAILY` — Repeatable tasks
- `STORY` — Narrative-driven
- `EXPLORATION` — Discovery
- `SKILL` — Skill practice
- `CHALLENGE` — Difficult tasks
- `EVENT` — Special occasions

#### QuestObjective
Tracks individual quest goals:
- `type` — ObjectiveType enum
- `description` — Human-readable goal
- `targetId` — Optional target reference
- `targetCount` — Required progress
- `currentCount` — Current progress
- `completed` — Completion status
- `optional` — Whether required for completion

#### QuestReward
Defines completion rewards:
- `experience` — XP points
- `vocabulary` — New words learned
- `items` — Physical items
- `friendshipPoints` — NPC relationship gains
- `unlockQuests` — Prerequisite unlocks
- `unlockAreas` — Location unlocks

#### QuestPrerequisite
Defines requirements:
- `questIds` — Required completed quests
- `friendshipLevel` — Minimum NPC friendship
- `requiredVocabulary` — Known words
- `requiredLevel` — Player level

### Repository

#### QuestRepository Interface
Defines data access methods:
- `getAllQuests()` — All quests
- `getQuestById(id)` — Specific quest
- `getQuestsByType(type)` — Filter by type
- `getQuestsByDifficulty(difficulty)` — Filter by difficulty
- `getQuestsByCategory(category)` — Filter by category
- `getQuestsByFilter(filter)` — Complex filtering
- `getActiveQuests()` — In-progress quests
- `getCompletedQuests()` — Finished quests
- `getAvailableQuests()` — Ready to start
- `getQuestProgress(id)` — Progress tracking
- `getQuestStats()` — Statistics
- `startQuest(id)` — Begin quest
- `completeQuest(id)` — Finish quest
- `abandonQuest(id)` — Cancel quest
- `updateObjectiveProgress(id, objectiveId, progress)` — Update progress
- `checkPrerequisites(id)` — Verify requirements

#### MockQuestRepository
In-memory implementation with:
- 12 sample quests across 3 chapters
- Progress persistence
- Automatic prerequisite unlocking
- Filter support

### ViewModel

#### QuestViewModel
Manages UI state:
- `uiState` — Current state (QuestUiState)
- `filteredQuests` — Filtered quest list
- `selectQuest(quest)` — Select for detail view
- `startQuest(id)` — Begin quest
- `completeQuest(id)` — Finish quest
- `abandonQuest(id)` — Cancel quest
- `updateObjectiveProgress(id, objectiveId, progress)` — Update progress
- `dismissCompletionDialog()` — Close completion dialog
- `updateFilter(filter)` — Apply filters
- `clearFilter()` — Reset filters
- `setSearchQuery(query)` — Search
- `filterByType(type)` — Filter by type
- `filterByDifficulty(difficulty)` — Filter by difficulty
- `filterByStatus(status)` — Filter by status
- `filterByCategory(category)` — Filter by category

### UI Components

#### QuestCard
Displays quest summary with:
- Title and description
- Status badge
- Difficulty and category chips
- Progress indicator
- Objective count

#### QuestStatusBadge
Color-coded status indicator:
- Gray: Locked
- Blue: Available
- Green: Active
- Purple: Completed

#### QuestDifficultyChip
Difficulty level indicator with color coding

#### QuestCategoryChip
Category label with subtle styling

#### QuestTypeIcon
Emoji-based type indicator

#### QuestObjectiveList
Displays quest goals with:
- Objective icons
- Progress bars
- Completion checkmarks
- Optional indicators

#### QuestRewardCard
Shows completion rewards:
- Experience points
- Vocabulary list
- Items
- Friendship points
- Unlocks

#### QuestCompletionDialog
Celebration dialog with:
- Success message
- Reward summary
- Continue button

### Screens

#### QuestListScreen
Browse and filter quests:
- Search bar
- Filter chips (status, difficulty, category)
- Scrollable quest list
- Empty state handling

#### QuestDetailScreen
Quest details with:
- Type and difficulty icons
- Status badge
- Description
- Objectives list
- Rewards card
- Action buttons (start/abandon/complete)
- Error display

### Navigation

#### Routes
- `quest_list` — Quest list screen
- `quest_detail/{questId}` — Quest detail screen

#### Integration
- Added to Screen.kt sealed class
- Integrated into PhoenixApp.kt NavHost
- Accessible from QingyuanVillageScreen

## Sample Quests

### Chapter 1: First Steps in Qingyuan
1. **帮助梅奶奶** — Help Grandma Mei buy dumpling ingredients
2. **买饺子** — Buy dumplings from Uncle Li's shop
3. **买茶** — Purchase tea at the tea house
4. **参观寺庙** — Visit the ancient temple
5. **认识大学学生伟** — Meet university student Wei

### Chapter 2: Daily Life
6. **日常买菜** — Daily market shopping (repeatable)
7. **伟的学习小组** — Join Wei's study group
8. **村庄节日** — Village festival celebration

### Chapter 3: Advanced Challenges
9. **解开谜题** — Solve an ancient puzzle
10. **寻宝之旅** — Treasure hunt adventure

## Testing

### Unit Tests
- `QuestModelsTest` — Data model logic
- `QuestRepositoryTest` — Repository operations

### Test Coverage
- Quest progress calculations
- Objective completion tracking
- Status transitions
- Prerequisite unlocking
- Filter operations

## Integration Points

### NPC System
- Quests linked to NPCs via `npcId`
- NPC profile shows available quests
- Quest completion affects friendship

### Dialogue System
- Quests trigger dialogues via `dialogueId`
- Dialogue choices affect quest progress
- Quest completion dialogues

### Friendship System
- Quest rewards include friendship points
- NPC friendship unlocks quests
- Quests teach NPC vocabulary

### Speaking & Pronunciation System
- Objectives with type `PRACTICE_SPEAKING` (e.g. `obj_2_3` on 买饺子, `obj_3_3` on 买茶) are progressed automatically
- Every successful pronunciation attempt contributes +1 to all active quests' speaking objectives via `updateObjectiveProgress`
- The pronunciation feature also tracks game progress milestones and passport entries for first practice and earned badges

## Future Enhancements

- Room persistence for offline storage
- Daily quest rotation system
- Quest chains and branching paths
- Time-limited events
- Multiplayer quests
- Achievement integration
