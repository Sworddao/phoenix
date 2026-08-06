# Architecture

## Overview

Phoenix follows **MVVM + Clean Architecture** with a feature-first modular approach.

## Architecture Layers

### 1. Presentation Layer (UI)
- **Jetpack Compose** for declarative UI
- **Material 3** for design system
- **ViewModel** for state management
- **StateFlow** for reactive state

### 2. Domain Layer
- **Repository interfaces** for data contracts
- **Use cases** (future)
- **Domain models** (future)

### 3. Data Layer
- **Repository implementations** for data access
- **Room** for local database
- **DataStore** for preferences
- **Mock repositories** for development

## MVVM Pattern

```
View (Compose)
    ↓ observes
ViewModel (StateFlow)
    ↓ calls
Repository (Interface)
    ↓ implements
MockRepository / Room / DataStore
```

### View
- Declarative UI with Compose
- Observes ViewModel state
- Calls ViewModel methods
- No business logic

### ViewModel
- Manages UI state with StateFlow
- Calls repository methods
- Handles user actions
- No UI references

### Repository
- Defines data contracts
- Abstracts data sources
- Provides testability
- Handles data operations

## Feature Organization

Each feature is self-contained:

```
feature/
├── data/         # Models, repository impl
├── domain/       # Repository interface
├── viewmodel/    # ViewModel
├── di/           # Hilt module
└── ui/           # Compose components
```

### Benefits
- **Modularity** — Features are independent
- **Testability** — Easy to mock
- **Scalability** — Add features without affecting others
- **Maintainability** — Clear separation of concerns

## Dependency Injection

### Hilt Modules
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureModule {
    @Binds
    @Singleton
    abstract fun bindRepository(impl: MockRepository): Repository
}
```

### Benefits
- **Loose coupling** — Dependencies are injected
- **Testability** — Easy to swap implementations
- **Lifecycle management** — Automatic scoping

## State Management

### StateFlow
```kotlin
data class UiState(
    val data: List<Item> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class FeatureViewModel @Inject constructor(
    private val repository: Repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

### Benefits
- **Reactive** — UI updates automatically
- **Lifecycle-aware** — Survives configuration changes
- **Thread-safe** — Coroutine-based

## Navigation

### Navigation Compose
```kotlin
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

NavHost(navController, startDestination = Screen.Home.route) {
    composable(Screen.Home.route) { HomeScreen() }
    composable(
        route = Screen.Detail.route,
        arguments = listOf(navArgument("id") { type = NavType.StringType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id")
        DetailScreen(id = id)
    }
}
```

### Benefits
- **Type-safe** — Compile-time checks
- **Argument passing** — Safe data transfer
- **Back stack management** — Automatic handling

## Testing Strategy

### Unit Tests
- **Models** — Data class logic
- **Repository** — Data operations
- **ViewModel** — State management

### Integration Tests
- **Repository + Database** — Room operations
- **ViewModel + Repository** — Full flow

### UI Tests
- **Compose** — Component rendering
- **Navigation** — Route transitions

### Test Structure
```
test/
└── feature/
    └── {feature}/
        ├── data/
        │   ├── {Feature}ModelsTest.kt
        │   └── {Feature}RepositoryTest.kt
        └── viewmodel/
            └── {Feature}ViewModelTest.kt
```

## Data Flow

### User Action
```
User taps button
    ↓
Compose calls ViewModel method
    ↓
ViewModel calls Repository
    ↓
Repository performs operation
    ↓
Repository returns result
    ↓
ViewModel updates StateFlow
    ↓
Compose recomposes with new state
```

### Error Handling
```
Repository throws exception
    ↓
ViewModel catches exception
    ↓
ViewModel updates error state
    ↓
Compose displays error message
```

## Offline-First Philosophy

### Local Data Sources
- **Room** — Structured data
- **DataStore** — Preferences
- **Assets** — Static content

### Benefits
- **No internet required** — Full functionality offline
- **Fast performance** — Local data access
- **Reliable** — No network dependencies

## Future Architecture

### Planned Additions
- **Use cases** — Business logic layer
- **Domain models** — Separate from data models
- **WorkManager** — Background tasks
- **Room** — Advanced queries
- **DataStore** — Complex preferences
- **Vocabulary persistence** — Room database for vocabulary
- **Discovery persistence** — Room database for discovery history

### Scalability
- **Feature modules** — Independent development
- **Clean architecture** — Clear boundaries
- **Dependency injection** — Flexible implementations
