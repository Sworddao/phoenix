# Phoenix

**Don't memorize Chinese. Live it.**

Phoenix is an offline-first Android game that teaches conversational Mandarin Chinese through exploration, storytelling, and real-life interactions.

---

## Vision

Instead of studying lessons, players explore a living world inspired by China. Players learn naturally by talking to characters, completing quests, solving everyday problems, and experiencing authentic situations.

Phoenix should feel closer to playing an adventure game than using a language-learning application.

---

## Implemented Features

- **Onboarding flow** — Welcome, profile creation, accessibility settings
- **Bao companion** — Red panda mascot with animated expressions
- **Qingyuan Village** — Interactive village with animated canvas scene
- **NPC framework** — Reusable NPC system with friendship levels
- **Dialogue system** — Branching conversations with player choices
- **Friendship system** — NPC relationship progression with persistence
- **Quest system** — Scalable architecture for story-driven quests
- **World map** — Interactive world map with 12 regions across 5 chapters
- **Accessibility** — Dad Mode, reduced motion, large text, high contrast

---

## Planned Features

> **Note:** These features are planned and documented in [SPEC.md](SPEC.md). Some are partially implemented (see CHANGELOG.md for details).

- **Story-driven exploration** — Travel from Qingyuan Village to Phoenix Summit
- **NPC conversations** — Learn through interaction with memorable characters
- **Offline learning** — No internet required, ever
- **Native pronunciation** — Authentic Mandarin audio throughout
- **Speaking & listening practice** — Communication first, reading second
- **Pinyin-first interface** — Hanzi hidden by default, optional toggle
- **Quest system** — Real-world missions that teach vocabulary naturally
- **Passport & collectibles** — Track your journey and discoveries
- **Dad Mode** — Reduced pressure, larger UI, gentle encouragement

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Database | Room (SQLite) |
| Audio | MediaPlayer / ExoPlayer |
| Maps | Custom Compose Canvas |
| Build | Gradle (Kotlin DSL) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

---

## Project Structure

```
Phoenix/
├── app/
│   └── src/main/java/com/sworddao/phoenix/
│       ├── data/                    # Data layer
│       │   ├── local/               # Room database
│       │   ├── model/               # Data models
│       │   └── preferences/         # DataStore preferences
│       ├── di/                      # Dependency injection
│       ├── feature/
│       │   ├── npc/                 # NPC framework
│       │   │   ├── data/            # Models & repository
│       │   │   ├── domain/          # Repository interface
│       │   │   ├── ui/              # Compose components
│       │   │   ├── viewmodel/       # ViewModel
│       │   │   └── di/              # Hilt module
│       │   ├── dialogue/            # Dialogue system
│       │   │   ├── data/            # Models & repository
│       │   │   ├── domain/          # Repository interface
│       │   │   ├── ui/              # Compose components
│       │   │   ├── viewmodel/       # ViewModel
│       │   │   └── di/              # Hilt module
│       │   ├── friendship/          # Friendship system
│       │   │   ├── data/            # Models, entities, repository
│       │   │   ├── domain/          # Repository interface
│       │   │   ├── ui/              # Compose components
│       │   │   ├── viewmodel/       # ViewModel
│       │   │   └── di/              # Hilt module
│       │   ├── quest/               # Quest system
│       │   │   ├── data/            # Models & repository
│       │   │   ├── domain/          # Repository interface
│       │   │   ├── ui/              # Compose components
│       │   │   ├── viewmodel/       # ViewModel
│       │   │   └── di/              # Hilt module
│       │   └── world/               # World map & exploration
│       │       ├── data/            # Models & repository
│       │       ├── domain/          # Repository interface
│       │       ├── ui/              # Compose components
│       │       ├── viewmodel/       # ViewModel
│       │       └── di/              # Hilt module
│       └── ui/                      # UI layer
│           ├── components/          # Reusable composables
│           ├── navigation/          # Navigation routes
│           ├── screens/             # Screen composables
│           ├── theme/               # Material3 theme
│           └── viewmodel/           # ViewModels
├── assets/                          # Game assets
├── docs/                            # Documentation
├── SPEC.md                          # Project specification
├── CHANGELOG.md                     # Version history
├── CONTRIBUTING.md                  # Contribution guidelines
└── README.md                        # This file
```

---

## Architecture

Phoenix follows **MVVM + Clean Architecture** with:

- **Hilt** for dependency injection
- **StateFlow** for reactive state management
- **Jetpack Compose** for declarative UI
- **Material 3** for design system
- **Kotlin Coroutines** for async operations
- **Offline-first** philosophy throughout

### Feature Organization

Each feature follows a consistent structure:
- `data/` — Models and repository implementations
- `domain/` — Repository interfaces
- `ui/` — Compose components and screens
- `viewmodel/` — ViewModels with StateFlow
- `di/` — Hilt dependency injection modules

---

## Screens

1. **Splash** — App loading screen
2. **Welcome** — Introduction to Phoenix
3. **Onboarding** — Feature walkthrough (4 pages)
4. **Player Profile** — Name, language, experience level
5. **Learning Preferences** — Accessibility settings
6. **Bao Greeting** — Meet your companion
7. **Qingyuan Village** — Interactive village exploration
8. **NPC Profile** — NPC details, friendship progress, conversation history
9. **Dialogue** — NPC conversations with branching choices
10. **Quest List** — Browse and filter available quests
11. **Quest Detail** — View quest objectives, rewards, and progress
12. **World Map** — Interactive world map with 12 regions
13. **Home** — Main dashboard
14. **Settings** — App configuration

---

## Development Workflow

1. Read [SPEC.md](SPEC.md) — it is the single source of truth
2. Create a feature branch from `main`
3. Implement features following the spec
4. Write tests where applicable
5. Submit a pull request

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## Author

**sworddao** — Project Creator & Lead Designer
