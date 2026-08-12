# Project Structure

## Overview

Phoenix follows a feature-first modular architecture with clean separation of concerns.

## Root Directory

```
Phoenix/
├── app/                          # Android application module
├── assets/                       # Game assets (audio, images)
├── docs/                         # Documentation
├── SPEC.md                       # Project specification
├── CHANGELOG.md                  # Version history
├── CONTRIBUTING.md               # Contribution guidelines
├── README.md                     # Project overview
└── build.gradle.kts              # Root build configuration
```

## App Module

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/sworddao/phoenix/
│   │   │   ├── data/             # Data layer
│   │   │   ├── di/               # Dependency injection
│   │   │   ├── feature/          # Feature modules
│   │   │   └── ui/               # UI layer
│   │   ├── res/                  # Android resources
│   │   └── AndroidManifest.xml
│   ├── test/                     # Unit tests
│   └── androidTest/              # Instrumented tests
└── build.gradle.kts              # App build configuration
```

## Feature Modules

Each feature follows a consistent structure:

```
feature/
└── {feature_name}/
    ├── data/                     # Data layer
    │   ├── {Feature}Models.kt    # Data classes
    │   ├── {Feature}Entities.kt  # Room entities
    │   ├── {Feature}Dao.kt       # Room DAO
    │   ├── {Feature}Mappers.kt   # Model ↔ entity mappers
    │   ├── Room{Feature}Repository.kt   # Room-backed implementation
    │   └── Mock{Feature}Repository.kt   # In-memory implementation
    ├── domain/                   # Domain layer
    │   └── {Feature}Repository.kt
    ├── viewmodel/                # ViewModel layer
    │   └── {Feature}ViewModel.kt
    ├── di/                       # Dependency injection
    │   └── {Feature}Module.kt
    └── ui/                       # UI layer
        ├── {Feature}Screen.kt
        └── {Component}.kt
```

Repository implementations: production binds `Room{Feature}Repository` (backed by `PhoenixDatabase`); `Mock{Feature}Repository` is retained for development and unit tests. All feature modules bind Room-backed implementations.

## Feature Modules Implemented

### NPC Framework (`feature/npc/`)
- NPC data models, Room entity/DAO, and mappers
- Room persistence via `RoomNpcRepository` (lazily seeded from `NpcSeedData`)
- NPC marker component
- NPC ViewModel

### Dialogue System (`feature/dialogue/`)
- Dialogue data models
- Dialogue tree engine (`DialogueFlow`)
- Room persistence via `RoomDialogueRepository` (lazily seeded from `DialogueSeedData`)
- Dialogue screen

### Friendship System (`feature/friendship/`)
- Friendship data models and entities
- Room persistence via `RoomFriendshipRepository`
- Friendship card and progress components
- NPC profile screen

### Game Progress (`feature/gameplay/`)
- Game progress and milestone data models and entities
- Room persistence via `RoomGameProgressRepository`
- Integration with dialogue results for first-time milestones

### Quest System (`feature/quest/`)
- Quest data models and entities
- Room-backed quest repository
- Quest card and list components
- Quest detail screen

### World Map (`feature/world/`)
- World data models and entities
- Room-backed world repository
- Region card and map components
- World map screen

### Passport & Collectibles (`feature/passport/`)
- Passport data models and entities
- Room-backed passport repository
- Region stamp cards
- Collectible grid
- Achievement list

### Vocabulary Learning (`feature/vocabulary/`)
- Vocabulary data models and entities (100+ entries, 12 categories)
- Room-backed vocabulary repository with search/filter
- Vocabulary list and detail screens
- Mastery tracking and statistics

### Vocabulary Discovery (`feature/discovery/`)
- Discovery data models (VocabularyDiscovery, DiscoverySource, etc.) and entities
- Room-backed discovery repository with streak tracking
- Discovery dialog and timeline components
- Integration with all game systems

### Speaking & Pronunciation (`feature/pronunciation/`)
- Pronunciation data models (SpeakingExercise, PronunciationAttempt, SpeakingMastery, etc.)
- PronunciationEngine interface with offline MockPronunciationEngine
- Room-backed pronunciation repository with session lifecycle, streaks, badges, and rewards
- Pronunciation screen, speaking button, and feedback components
- Integration with dialogue, vocabulary, quest, game progress, and passport systems

### Listening & Audio Comprehension (`feature/listening/`)
- Listening data models (ListeningExercise, ListeningMastery, ListeningBadge, etc.)
- AudioEngine interface with offline MockAudioEngine (play/pause/resume/stop, 0.75x slow playback)
- Room-backed listening repository with session lifecycle, streaks, badges, and rewards
- Listening screen, audio player, exercise, and choice components
- Integration with dialogue, vocabulary, quest, game progress, and passport systems

### Reading & Hanzi (`feature/reading/`)
- Reading data models (ReadingExercise, HanziCard, ReadingMastery, ReadingBadge, etc.)
- HanziRenderer interface with offline MockHanziRenderer (pinyin-first rendering)
- Room-backed reading repository with session lifecycle, streaks, badges, and rewards
- Reading screen with hanzi reveal and choice components
- Integration with dialogue, vocabulary, quest, game progress, passport, pronunciation, and listening systems

### Game Progression & Learning Path (`feature/progression/`)
- XP rules and level system (max level 100) with per-level requirements
- 7 level-gated feature unlocks (speaking, listening, reading, quest types, NPCs, conversations, regions)
- Chapter system mapping the 12 world regions with unlock requirements
- Room-backed repository aggregating all source systems via snapshot-delta detection
- Learning progress percentages across 9 dimensions plus overall completion
- Daily goals, per-source activity counts, and goal streaks
- Progression screen with level card, learning bars, chapters, objectives, recent unlocks, and feature unlock timeline

### Adaptive Review & Spaced Repetition (`feature/review/`)
- Pure spaced repetition engine with calculated intervals (10 min / 1 day / 3 days / 7 days / 14 days / 30 days / 90 days) and adaptive stage transitions
- Per-word memory model (strength, confidence, correct/incorrect counts, average score, speaking/listening/reading accuracy, conversation success, streak, failures)
- Adaptive difficulty derived from memory strength (NEW → LEARNING → FAMILIAR → MASTERED)
- Room-backed repository scheduling reviews from all 9 source systems via snapshot deltas
- Sessions with per-type filtering and empty-state handling, rescheduling answered items to "upcoming"
- Review screen with today's reviews, daily goal, Bao recommendations, statistics, upcoming reviews, and memory strengths
- Integration with progression (15 XP per completed session via `XpSource.REVIEW`)

## Data Layer

```
data/
├── local/                        # Local data sources
│   ├── PhoenixDatabase.kt        # Room database (v3, 59 entities, 13 DAOs)
│   ├── PlaceholderEntity.kt      # Placeholder entity for schema compatibility
│   ├── AppMetadata.kt            # App metadata entity + DAO (per-word practice fields)
│   └── RoomJson.kt               # kotlinx-serialization JSON document persistence
├── model/                        # Shared data models
│   ├── PlayerProfile.kt
│   └── AccessibilityPreferences.kt
├── preferences/                  # DataStore preferences
└── seed/                         # Seed data for Room repositories
    ├── VocabularySeedData.kt
    ├── DiscoverySeedData.kt
    ├── QuestSeedData.kt
    ├── WorldSeedData.kt
    ├── PassportSeedData.kt
    ├── PronunciationSeedData.kt
    ├── ListeningSeedData.kt
    ├── ReadingSeedData.kt
    ├── DialogueSeedData.kt
    └── NpcSeedData.kt
```

## UI Layer

```
ui/
├── components/                   # Reusable composables
│   ├── BaoCharacter.kt
│   └── BaoExpression.kt
├── navigation/                   # Navigation configuration
│   └── Screen.kt                 # Route definitions
├── screens/                      # Screen composables
│   ├── Splash*.kt
│   ├── Welcome*.kt
│   ├── Onboarding*.kt
│   ├── PlayerProfile*.kt
│   ├── LearningPreferences*.kt
│   ├── BaoGreeting*.kt
│   ├── QingyuanVillage*.kt
│   ├── Home*.kt
│   └── Settings*.kt
├── theme/                        # Material3 theme
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
└── viewmodel/                    # ViewModels
    └── ProfileViewModel.kt
```

## Dependency Injection

```
di/
├── DatabaseModule.kt             # Room database, 16 DAOs, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5
├── feature/npc/di/NpcModule.kt             # NPC repository binding (Room)
├── feature/dialogue/di/DialogueModule.kt   # Dialogue repository binding (Room)
├── feature/gameplay/di/GameplayModule.kt
├── feature/friendship/di/FriendshipModule.kt
├── feature/quest/di/QuestModule.kt
├── feature/world/di/WorldModule.kt
├── feature/passport/di/PassportModule.kt
├── feature/vocabulary/di/VocabularyModule.kt
├── feature/discovery/di/DiscoveryModule.kt
├── feature/pronunciation/di/PronunciationModule.kt
├── feature/listening/di/ListeningModule.kt
├── feature/reading/di/ReadingModule.kt
├── feature/progression/di/ProgressionModule.kt
└── feature/review/di/ReviewModule.kt
```

Feature modules bind `Room{Feature}Repository` as the production implementation; mock repositories remain available for development and unit tests.

## Testing Structure

```
test/
└── java/com/sworddao/phoenix/
    ├── data/local/
    │   ├── PhoenixDatabaseMigrationTest.kt   # v2→v3 migration integrity
    │   └── RoomTestDb.kt                     # In-memory Room DB test harness
    ├── feature/
    │   ├── npc/data/NpcModelsTest.kt
    │   ├── dialogue/data/DialogueModelsTest.kt
    │   ├── dialogue/data/DialogueViewModelActionTest.kt
    │   ├── friendship/data/FriendshipModelsTest.kt
    │   ├── friendship/data/FriendshipRepositoryTest.kt
    │   ├── friendship/data/RoomFriendshipRepositoryTest.kt
    │   ├── gameplay/data/RoomGameProgressRepositoryTest.kt
    │   ├── quest/data/QuestModelsTest.kt
    │   ├── quest/data/QuestRepositoryTest.kt
    │   ├── quest/data/RoomQuestRepositoryTest.kt
    │   ├── world/data/WorldModelsTest.kt
    │   ├── world/data/WorldRepositoryTest.kt
    │   ├── world/data/RoomWorldRepositoryTest.kt
    │   ├── passport/data/PassportModelsTest.kt
    │   ├── passport/data/PassportRepositoryTest.kt
    │   ├── passport/data/RoomPassportRepositoryTest.kt
    │   ├── vocabulary/data/VocabularyModelsTest.kt
    │   ├── vocabulary/data/VocabularyRepositoryTest.kt
    │   ├── vocabulary/data/RoomVocabularyRepositoryTest.kt
    │   ├── discovery/data/DiscoveryModelsTest.kt
    │   ├── discovery/data/DiscoveryRepositoryTest.kt
    │   ├── discovery/data/RoomDiscoveryRepositoryTest.kt
    │   ├── pronunciation/data/PronunciationModelsTest.kt
    │   ├── pronunciation/data/PronunciationRepositoryTest.kt
    │   ├── pronunciation/data/RoomPronunciationRepositoryTest.kt
    │   ├── listening/data/ListeningModelsTest.kt
    │   ├── listening/data/ListeningRepositoryTest.kt
    │   ├── listening/data/RoomListeningRepositoryTest.kt
    │   ├── reading/data/ReadingModelsTest.kt
    │   ├── reading/data/ReadingRepositoryTest.kt
    │   ├── reading/data/RoomReadingRepositoryTest.kt
    │   ├── progression/data/ProgressionModelsTest.kt
    │   ├── progression/data/ProgressionRepositoryTest.kt
    │   ├── progression/data/RoomProgressionRepositoryTest.kt
    │   ├── review/data/SpacedRepetitionEngineTest.kt
    │   ├── review/data/ReviewModelsTest.kt
    │   ├── review/data/ReviewRepositoryTest.kt
    │   ├── review/data/RoomReviewRepositoryTest.kt
    │   └── review/viewmodel/ReviewViewModelTest.kt
    └── PlayerModelsTest.kt
```

Room repository tests use `RoomTestDb` (Robolectric in-memory `Room.inMemoryDatabaseBuilder`) and exercise the full Room-backed path for each feature.

## Documentation Structure

```
docs/
├── dialogue-system.md
├── npc-system.md
├── friendship-system.md
├── quest-system.md
├── world-system.md
├── passport-system.md
├── vocabulary-system.md
├── discovery-system.md
├── pronunciation-system.md
├── listening-system.md
├── reading-system.md
├── progression-system.md
├── review-system.md
├── project-structure.md          # This file
├── architecture.md               # Architecture details
└── templates/
    └── feature-template.md       # Feature proposal template
```

## Conventions

### File Naming
- Models: `{Feature}Models.kt`
- Entities: `{Feature}Entities.kt`
- DAO: `{Feature}Dao.kt`
- Mappers: `{Feature}Mappers.kt`
- Repository Interface: `{Feature}Repository.kt`
- Repository Implementation: `Room{Feature}Repository.kt` (production), `Mock{Feature}Repository.kt` (tests/dev)
- ViewModel: `{Feature}ViewModel.kt`
- Module: `{Feature}Module.kt`
- Screen: `{Feature}Screen.kt`
- Component: `{ComponentName}.kt`
- Test: `{Feature}ModelsTest.kt`, `{Feature}RepositoryTest.kt`, `Room{Feature}RepositoryTest.kt`

### Package Structure
- Feature packages: `com.sworddao.phoenix.feature.{feature_name}`
- UI packages: `com.sworddao.phoenix.ui`
- Data packages: `com.sworddao.phoenix.data`

### Dependencies
- Hilt for dependency injection
- StateFlow for reactive state
- Kotlin Coroutines for async
- Jetpack Compose for UI
- Material 3 for design
