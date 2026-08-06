# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added

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
- Documentation for reading system and related system updates
- Documentation for listening system and related system updates
- Documentation for dialogue, NPC, quest, world, passport, and vocabulary systems
- SPEC.md restructuring with fixed numbering, Non-Goals, Graduate Outcomes sections
- Feature template moved to docs/templates/feature-template.md

### Changed

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
