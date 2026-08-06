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
└── vocabulary/di/VocabularyModule.kt
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
    │   └── vocabulary/data/VocabularyRepositoryTest.kt
    └── ui/viewmodel/ProfileViewModelTest.kt
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
