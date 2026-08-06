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
- Accessibility settings (Dad Mode, reduced motion, large text, high contrast)
- Unit tests for data models and friendship system
- Documentation for dialogue, NPC, quest, and world systems
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

- Audio playback
- Vocabulary tracking
- Additional NPC dialogues
- Game progression system

---

## [0.1.0] - 2026-08-05

### Added

- Initial repository setup
- Project specification
- Documentation structure
