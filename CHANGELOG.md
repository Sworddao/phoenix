# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

- Room persistence layer for all game systems
  - `PhoenixDatabase` v3 with 59 entities and 13 DAOs covering vocabulary, discovery, quests, game progress, passport, world, friendship, reading, listening, speaking, review, progression, and app metadata
  - `MIGRATION_2_3` for automatic database upgrades from v2 with schema integrity tests
  - Room-backed repository implementations for 12 features (vocabulary, discovery, friendship, gameplay, listening, passport, progression, pronunciation, quest, reading, review, world) — dialogue and NPC remain in-memory mocks pending persistence
  - Seed data extracted to `data/seed/` (vocabulary, discovery, quest, world, passport, pronunciation, listening, reading, dialogue, NPC)
  - `RoomJson` JSON document storage via kotlinx.serialization for snapshot/history documents (game progress, progression, review memory)
  - `AppMetadata` entity with app-metadata DAO for per-word practice fields
  - Hilt `DatabaseModule` providing the Room database, all 13 DAOs, and the v2→v3 migration; feature DI modules updated to bind Room-backed implementations
  - Robolectric-based Room repository tests via `RoomTestDb` in-memory database harness plus `PhoenixDatabaseMigrationTest`
  - Fixes surfaced by persistence tests: `GiftRecord` marked `@Serializable`, `VocabularyDao.searchWords` now matches `hanzi`, seeded `vocabulary_progress` rows, `PlayerProgress` lambda marked `@Transient`
  - Full unit suite passes (898 tests, 0 failures)

- Device smoke tests and CI emulator job
  - `DeviceSmokeTest` instrumented tests validating the app boots to RESUMED, the real `phoenix_database` opens at v3 with all tables, v2→v3 migration preserves data on-device, and Room DAO round-trips persist
  - GitHub Actions `device-smoke` job running `connectedDebugAndroidTest` on an API 35 emulator

- Game progression & learning path system
  - Central XP engine (`XpSource`, `XpCalculator`) aggregating dialogue, vocabulary, quests, friendship, speaking, listening, reading, exploration, passport, and achievements
  - Level system (max level 100) with feature unlocks gated by level (speaking at 2, listening at 3, reading at 4, quest types at 5, NPCs at 7, conversations at 8, regions at 10)
  - Chapter system mapping the 12 world regions with region-completion or level-based unlock requirements
  - Learning progress percentages across 9 dimensions plus overall completion
  - Daily goals, per-source activity tracking, and goal streaks
  - Snapshot-delta aggregation that awards XP automatically from all source systems
  - Progression screen with level card, daily card, learning bars, chapter progress, objectives, recent unlocks, and feature unlock timeline
  - ProgressionViewModel observing all source systems for live refresh
  - Unit tests for XP math, level-ups, feature unlocks, snapshot deltas, chapters, and daily goals (898 tests total)
  - Progression entry button in Qingyuan Village

- Adaptive review & spaced repetition system
  - Spaced repetition engine with calculated intervals (10 min, 1 day, 3 days, 7 days, 14 days, 30 days, 90 days) and adaptive stage transitions
  - Per-word memory model (strength, confidence, correct/incorrect counts, average score, speaking/listening/reading accuracy, conversation success, streak, failures)
  - Adaptive difficulty derived from memory strength (NEW → LEARNING → FAMILIAR → MASTERED)
  - Review scheduling from all 9 source systems via snapshot deltas (vocabulary, dialogue, speaking, listening, reading, NPC, quest, friendship, exploration)
  - Review sessions (conversation, listening, speaking, reading, mixed, NPC challenge, quest review, daily) with answer scoring and XP integration (15 XP per session via XpSource.REVIEW)
  - Review dashboard with today's reviews, daily goal, Bao recommendations, statistics, upcoming reviews, and memory strengths
  - ReviewViewModel handling dashboard, sessions, and completion flow
  - Unit tests for engine, models, repository, and ViewModel (898 tests total, 120 for review)
  - Review entry button in Qingyuan Village

- Project specification (SPEC.md)
- Repository structure
- MIT License
- Contributing guidelines
- Code of Conduct
- Core Android application with onboarding flow
- Bao companion with animated expressions
- Qingyuan Village with interactive canvas scene
- NPC framework with friendship system
- Dialogue system with branching conversations
- Friendship system with NPC relationship progression
  - FriendshipState tracking XP, level, and history
  - ConversationMemory for dialogue history
  - FriendshipEvent for relationship milestones
  - Room persistence for offline data storage
  - NPCProfileScreen with full NPC details
  - FriendshipCard, FriendshipProgressBar, RelationshipBadge UI components
  - LevelUpDialog for celebration moments
  - Integration with dialogue engine for automatic XP gains
- Quest system foundation with scalable architecture
  - Quest data models (QuestType, QuestDifficulty, QuestStatus, QuestCategory)
  - QuestObjective with progress tracking
  - QuestReward with vocabulary, items, and unlockables
  - QuestPrerequisite for progression gates
  - QuestRepository interface with MockQuestRepository implementation
  - QuestViewModel with filtering and state management
  - QuestCard, QuestStatusBadge, QuestDifficultyChip UI components
  - QuestObjectiveList with progress indicators
  - QuestRewardCard displaying all reward types
  - QuestCompletionDialog for celebration moments
  - QuestListScreen with search and filtering
  - QuestDetailScreen with objectives, rewards, and actions
  - Navigation routes for quest screens
  - Integration with QingyuanVillageScreen
  - 12 sample quests across 3 chapters
  - Unit tests for models and repository
- World map and exploration foundation
  - World data models (WorldRegion, WorldLocation, UnlockRequirement, TravelMethod, etc.)
  - 12 regions across 5 chapters (Qingyuan Village to Phoenix Summit)
  - WorldRepository interface with MockWorldRepository implementation
  - WorldViewModel with state management and travel logic
  - RegionCard, RegionStatusBadge, WorldMapCanvas UI components
  - ExplorationProgressCard, CurrentLocationBanner components
  - WorldMapScreen with region list and detail dialogs
  - Travel confirmation dialog
  - Integration with QingyuanVillageScreen
  - Navigation routes for world map
  - Unit tests for models and repository
- Passport & collectibles system
  - Passport data models (Passport, PassportRegion, Collectible, etc.)
  - 12 regions, 52 collectibles across 20 categories
  - 9 achievements with progress tracking
  - MockPassportRepository with pre-seeded data
  - PassportViewModel with state management
  - PassportScreen with stamps, collectibles, and timeline
  - Region cards, collection grid, achievement list
  - Navigation routes for passport
  - Unit tests for models and repository
- Vocabulary learning foundation
  - Vocabulary data models (VocabularyWord, VocabularyCategory, etc.)
  - 100+ vocabulary entries across 12 categories
  - VocabularyRepository interface with MockVocabularyRepository
  - VocabularyViewModel with search, filter, and mastery tracking
  - VocabularyScreen, VocabularyDetailScreen with full UI
  - MasteryIndicator, CategoryChip, FilterChipsRow components
  - 5 mastery levels (Unknown → Mastered)
  - Integration with regions, NPCs, and quests
  - Navigation routes for vocabulary
  - Unit tests for models and repository
- Vocabulary discovery and conversation rewards
  - Discovery data models (VocabularyDiscovery, DiscoverySource, DiscoveryReward, etc.)
  - 13 discovery sources (NPC, Dialogue, Quest, Friendship, Region, etc.)
  - DiscoveryRepository interface with MockDiscoveryRepository
  - DiscoveryViewModel with filtering and animation state
  - VocabularyDiscoveryDialog, NewWordCard, DiscoveryTimeline components
  - First discovery detection and duplicate handling
  - Bonus XP and friendship rewards based on source
  - Discovery streak tracking
  - Integration with Dialogue, Quest, NPC, World, Passport, and Friendship systems
  - Navigation routes for discovery history
  - Unit tests for models and repository
- Accessibility settings (Dad Mode, reduced motion, large text, high contrast)
- Speaking & pronunciation foundation (Feature 4.4)
  - Pronunciation data models (SpeakingExercise, PronunciationAttempt, SpeakingMastery, etc.)
  - 11 initial speaking exercises (vocabulary, tone, dialogue, and freestyle types)
  - PronunciationEngine interface with MockPronunciationEngine for offline-first speech analysis
  - PronunciationRepository interface with MockPronunciationRepository
  - Session lifecycle with exercise selection, progress tracking, and completion
  - Offline phonetic similarity evaluation (SUCCESS_THRESHOLD = 0.7) with encouraging feedback
  - Streak tracking with daily persistence semantics and 8 pronunciation badges
  - XP, friendship bonus, and personal-best rewards
  - Integration with Dialogue (PRACTICE_SPEAKING action), Vocabulary (timesSpoken), Quest (PRACTICE_SPEAKING objectives), Game Progress (FIRST_SPEAKING milestone), and Passport (SPEAKING_PRACTICE & ACHIEVEMENT_UNLOCKED entries)
  - PronunciationViewModel with recording state machine (startRecording, nextExercise, repeatExercise)
  - PronunciationScreen, SpeakingButton with pulse animation, RecordingIndicator, and Bao tip components
  - Reduced-motion support via animator duration scale
  - Navigation route `pronunciation/{wordId}` accessible from dialogue completion and vocabulary detail
  - Passport mock now persists recorded entries (recordEntry) for observable timeline history
  - Unit tests for models and repository
- Unit tests for data models and friendship system
- Listening & audio comprehension foundation (Feature 4.5)
  - Listening data models (ListeningExercise, AudioClip, ListeningAttempt, ListeningSession, ListeningProgress, ListeningMastery, ListeningStatistics, ListeningBadge)
  - 8 exercise types (hear & choose meaning, identify vocabulary, match image, NPC response, numbers, greetings, directions, food orders) across 4 difficulties
  - 14 initial listening exercises plus dynamic per-word exercise generation
  - AudioEngine interface with MockAudioEngine for offline-first playback simulation (play/pause/resume/stop, slow 0.75x, playback state flow)
  - ListeningRepository interface with MockListeningRepository
  - Session lifecycle with answer submission, replay counting, streak tracking, and completion
  - 8 listening badges (first listen, 3/7/30-day streaks, quick ear, accurate, NPC-ready, word collector)
  - XP, friendship bonus, and personal-best rewards
  - Integration with Dialogue (PRACTICE_LISTENING action), Vocabulary (timesHeard), Quest (LISTEN_TO_AUDIO objectives), Game Progress (FIRST_LISTENING milestone), Passport (LISTENING_PRACTICE & ACHIEVEMENT_UNLOCKED entries), and Pronunciation (unlocks linked speaking exercises)
  - ListeningViewModel with playback state machine (playCurrent, replay, toggleSlowPlayback, selectChoice)
  - ListeningScreen with AudioPlayerCard, ListeningChoiceCard, ReplayButton, Bao hint, and completion dialog
  - Reduced-motion support for playback and Bao animations
  - Navigation route `listening/{wordId}` accessible from dialogue completion and vocabulary detail
  - Quest `quest_order_tea` gains a LISTEN_TO_AUDIO objective
  - Unit tests for models and repository
- Reading & hanzi foundation (Feature 4.6)
  - Reading data models (ReadingExercise, HanziCard, ReadingAttempt, ReadingSession, ReadingProgress, ReadingMastery, ReadingStatistics, ReadingBadge, ReadingSessionConfig)
  - 8 exercise types (match spoken to written, match pinyin to hanzi, match hanzi to meaning, sentence reading, phrase recognition, character recognition, context reading, NPC dialogue reading) across 4 difficulties
  - 14 initial reading exercises plus dynamic per-word exercise generation
  - HanziRenderer interface with MockHanziRenderer for offline-first pinyin-first rendering (7 reveal states: hidden, pinyin-only, hanzi-only, hanzi+pinyin, tone-colored pinyin, tap-to-reveal, auto-reveal; masked `▢` hanzi and tone diacritic analysis)
  - ReadingRepository interface with MockReadingRepository
  - Session lifecycle with answer submission, reveal recording, streak tracking, and completion
  - 8 reading badges (first read, 3/7/30-day streaks, quick eye, accurate, dialogue-ready, character collector)
  - XP, friendship bonus, and personal-best rewards
  - Integration with Dialogue (PRACTICE_READING action + bubble "阅读这句话" action), Vocabulary (timesRead), Quest (READ_CHARACTERS objectives), Game Progress (FIRST_READING milestone), Passport (READING_PRACTICE & ACHIEVEMENT_UNLOCKED entries), Pronunciation (unlocks linked speaking exercises), and Listening (unlocks linked listening exercises)
  - ReadingViewModel with reveal state machine (startPractice, revealHanzi, setAutoRevealDelay, selectChoice, nextExercise)
  - ReadingScreen with HanziDisplayCard, ToneColoredPinyin, ReadingChoiceCard, BaoReadingHint, RevealButton, and completion dialog
  - Reduced-motion support for Bao animations
  - Navigation route `reading/{wordId}` accessible from dialogue completion, NPC dialogue bubbles, and vocabulary detail
  - Quest `quest_order_tea` gains a READ_CHARACTERS objective
  - Unit tests for models and repository
- Writing & hanzi stroke foundation (Feature 4.9)
  - Writing data models (HanziCharacter, HanziStroke, WritingExercise, WritingAttempt, WritingStrokeAnswer, WritingSession, WritingProgress, WritingMastery, WritingStatistics, WritingBadge, WritingSessionConfig, WritingResult)
  - 3 exercise types (TRACE_STROKES, STROKE_ORDER, DIRECTION_CHECK) across 4 difficulties; progressive default sessions cycle all three types
  - 19 seeded characters with ordered stroke data (StrokeType + StrokeDirection) and ~30 initial exercises plus dynamic per-word generation
  - WritingEngine abstraction with MockWritingEngine validating stroke order and direction (only fully-correct strokes advance; wrong taps retry the pending stroke)
  - WritingRepository with Room persistence (writing_exercise, progress/statistics/badges/sessions/state docs)
  - Session lifecycle with real per-stroke attempts, streak tracking, XP (+5 streak bonus), friendship bonus, and personal-best detection
  - 8 writing badges (first stroke, 3/7/30-day streaks, steady hand, stroke perfect, pen ready, character collector)
  - Integration with Dialogue (PRACTICE_WRITING action), Vocabulary (timesWritten), Quest (WRITE_CHARACTERS objectives), Game Progress (FIRST_WRITING milestone + WRITING_PRACTICE XP), Passport (WRITING_PRACTICE & ACHIEVEMENT_UNLOCKED entries), and Friendship (greeting bonus)
  - WritingViewModel recording real stroke answers; WritingScreen with character card, direction buttons, feedback, result, and completion dialog
  - Navigation route `writing/{wordId}` accessible from dialogue completion, celebration, and vocabulary detail
  - Unit tests for engine, models, repository (Room + mock), and ViewModel
- Documentation for writing system
- Documentation for reading system and related system updates
- Documentation for listening system and related system updates
- Documentation for dialogue, NPC, quest, world, passport, and vocabulary systems
- SPEC.md restructuring with fixed numbering, Non-Goals, Graduate Outcomes sections
- Feature template moved to docs/templates/feature-template.md

### Changed

- Migrated repository implementations from in-memory mocks to Room-backed storage (dialogue and NPC remain in-memory mocks pending persistence)
- `app/build.gradle.kts`: added KSP Room schema configuration, Robolectric test options, and Room/AndroidX-test dependencies
- Updated NPC markers to navigate to NPCProfileScreen instead of info dialog
- Updated QingyuanVillageScreen with friendship context
- Updated PhoenixApp navigation for NPC profile route
- Updated README with current project status
- Updated CHANGELOG with feature history
- SPEC.md: Added placeholder headings for planned sections (18-29)

### Planned

- Real speech recognition (Android SpeechRecognizer / Vosk / Whisper.cpp) behind PronunciationEngine
- Real audio playback backend (ExoPlayer / Media3 or packaged TTS samples) behind AudioEngine
- Additional NPC dialogues
- Game progression system (further milestones and rewards)

---

## [0.1.0] - 2026-08-05

### Added

- Initial repository setup
- Project specification
- Documentation structure
