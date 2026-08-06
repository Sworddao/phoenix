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
    │   ├── Mock{Feature}Repository.kt
    │   └── {Feature}Entities.kt  # Room entities (if needed)
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

## Feature Modules Implemented

### NPC Framework (`feature/npc/`)
- NPC data models and repository
- NPC marker component
- NPC ViewModel

### Dialogue System (`feature/dialogue/`)
- Dialogue data models
- Dialogue tree engine
- Dialogue screen

### Friendship System (`feature/friendship/`)
- Friendship data models and entities
- Room persistence
- Friendship card and progress components
- NPC profile screen

### Quest System (`feature/quest/`)
- Quest data models
- Quest repository
- Quest card and list components
- Quest detail screen

### World Map (`feature/world/`)
- World data models
- World repository
- Region card and map components
- World map screen

### Passport & Collectibles (`feature/passport/`)
- Passport data models
- Passport repository
- Region stamp cards
- Collectible grid
- Achievement list

### Vocabulary Learning (`feature/vocabulary/`)
- Vocabulary data models (100+ entries, 12 categories)
- Vocabulary repository with search/filter
- Vocabulary list and detail screens
- Mastery tracking and statistics

### Vocabulary Discovery (`feature/discovery/`)
- Discovery data models (VocabularyDiscovery, DiscoverySource, etc.)
- Discovery repository with streak tracking
- Discovery dialog and timeline components
- Integration with all game systems

### Speaking & Pronunciation (`feature/pronunciation/`)
- Pronunciation data models (SpeakingExercise, PronunciationAttempt, SpeakingMastery, etc.)
- PronunciationEngine interface with offline MockPronunciationEngine
- Pronunciation repository with session lifecycle, streaks, badges, and rewards
- Pronunciation screen, speaking button, and feedback components
- Integration with dialogue, vocabulary, quest, game progress, and passport systems

### Listening & Audio Comprehension (`feature/listening/`)
- Listening data models (ListeningExercise, ListeningMastery, ListeningBadge, etc.)
- AudioEngine interface with offline MockAudioEngine (play/pause/resume/stop, 0.75x slow playback)
- Listening repository with session lifecycle, streaks, badges, and rewards
- Listening screen, audio player, exercise, and choice components
- Integration with dialogue, vocabulary, quest, game progress, and passport systems

### Game Progression & Learning Path (`feature/progression/`)
- XP rules and level system (max level 100) with per-level requirements
- 7 level-gated feature unlocks (speaking, listening, reading, quest types, NPCs, conversations, regions)
- Chapter system mapping the 12 world regions with unlock requirements
- Central repository aggregating all source systems via snapshot-delta detection
- Learning progress percentages across 9 dimensions plus overall completion
- Daily goals, per-source activity counts, and goal streaks
- Progression screen with level card, learning bars, chapters, objectives, recent unlocks, and feature unlock timeline

## Data Layer

```
data/
├── local/                        # Local data sources
│   ├── PhoenixDatabase.kt        # Room database
│   └── dao/                      # Data access objects
├── model/                        # Shared data models
│   ├── PlayerProfile.kt
│   └── AccessibilityPreferences.kt
└── preferences/                  # DataStore preferences
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
├── DatabaseModule.kt             # Room database providers
├── npc/di/NpcModule.kt           # NPC repository binding
├── dialogue/di/DialogueModule.kt # Dialogue repository binding
├── friendship/di/FriendshipModule.kt
├── quest/di/QuestModule.kt
├── world/di/WorldModule.kt
├── passport/di/PassportModule.kt
├── vocabulary/di/VocabularyModule.kt
├── discovery/di/DiscoveryModule.kt
├── pronunciation/di/PronunciationModule.kt
├── listening/di/ListeningModule.kt
└── progression/di/ProgressionModule.kt
```

## Testing Structure

```
test/
└── java/com/sworddao/phoenix/
    ├── feature/
    │   ├── npc/data/NpcModelsTest.kt
    │   ├── dialogue/data/DialogueModelsTest.kt
    │   ├── friendship/data/FriendshipModelsTest.kt
    │   ├── friendship/data/FriendshipRepositoryTest.kt
    │   ├── quest/data/QuestModelsTest.kt
    │   ├── quest/data/QuestRepositoryTest.kt
    │   ├── world/data/WorldModelsTest.kt
    │   ├── world/data/WorldRepositoryTest.kt
    │   ├── passport/data/PassportModelsTest.kt
    │   ├── passport/data/PassportRepositoryTest.kt
    │   ├── vocabulary/data/VocabularyModelsTest.kt
    │   ├── vocabulary/data/VocabularyRepositoryTest.kt
    │   ├── discovery/data/DiscoveryModelsTest.kt
    │   ├── discovery/data/DiscoveryRepositoryTest.kt
    │   ├── pronunciation/data/PronunciationModelsTest.kt
    │   ├── pronunciation/data/PronunciationRepositoryTest.kt
    │   ├── listening/data/ListeningModelsTest.kt
    │   ├── listening/data/ListeningRepositoryTest.kt
    │   ├── progression/data/ProgressionModelsTest.kt
    │   ├── progression/data/ProgressionRepositoryTest.kt
    │   ├── dialogue/data/DialogueModelsTest.kt
    │   ├── dialogue/data/DialogueViewModelActionTest.kt
    │   └── ui/viewmodel/ProfileViewModelTest.kt
```

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
├── project-structure.md          # This file
├── architecture.md               # Architecture details
└── templates/
    └── feature-template.md       # Feature proposal template
```

## Conventions

### File Naming
- Models: `{Feature}Models.kt`
- Repository Interface: `{Feature}Repository.kt`
- Repository Implementation: `Mock{Feature}Repository.kt`
- ViewModel: `{Feature}ViewModel.kt`
- Module: `{Feature}Module.kt`
- Screen: `{Feature}Screen.kt`
- Component: `{ComponentName}.kt`
- Test: `{Feature}ModelsTest.kt`, `{Feature}RepositoryTest.kt`

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
