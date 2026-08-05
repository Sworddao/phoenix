# Phoenix

**Don't memorize Chinese. Live it.**

Phoenix is an offline-first Android game that teaches conversational Mandarin Chinese through exploration, storytelling, and real-life interactions.

---

## Vision

Instead of studying lessons, players explore a living world inspired by China. Players learn naturally by talking to characters, completing quests, solving everyday problems, and experiencing authentic situations.

Phoenix should feel closer to playing an adventure game than using a language-learning application.

---

## Planned Features

> **Note:** These features are planned and documented in [SPEC.md](SPEC.md). Some are partially implemented (see CHANGELOG.md for details).

- **Story-driven exploration** — Travel from Qingyuan Village to Phoenix Summit
- **NPC conversations** — Learn through interaction with memorable characters
- **Offline learning** — No internet required, ever
- **Native pronunciation** — Authentic Mandarin audio throughout
- **Speaking & listening practice** — Communication first, reading second
- **Pinyin-first interface** — Hanzi hidden by default, optional toggle
- **Bao companion** — A friendly red panda who guides and encourages
- **Quest system** — Real-world missions that teach vocabulary naturally
- **Passport & collectibles** — Track your journey and discoveries
- **Dad Mode** — Reduced pressure, larger UI, gentle encouragement
- **Accessibility** — Large text, high contrast, slow audio, reduced motion

---

## Technology Stack (Planned)

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
├── app/                    # Android application module
├── assets/                 # Game assets
│   ├── audio/              # Voice recordings
│   ├── music/              # Background music
│   ├── sfx/                # Sound effects
│   ├── icons/              # Game icons
│   ├── maps/               # Map data
│   ├── npc/                # NPC assets
│   ├── ui/                 # UI assets
│   └── logo/               # App logo
├── curriculum/             # Language curriculum data
├── database/               # Database schemas & migrations
├── design/                 # Design documents & wireframes
├── scripts/                # Build & utility scripts
├── docs/                   # Documentation
├── .github/                # GitHub templates & workflows
├── SPEC.md                 # Full project specification
├── LICENSE                 # MIT License
├── CONTRIBUTING.md         # Contribution guidelines
├── CODE_OF_CONDUCT.md      # Community standards
└── CHANGELOG.md            # Version history
```

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
