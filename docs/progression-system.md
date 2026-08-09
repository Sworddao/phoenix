# Game Progression & Learning Path System

## Overview

The Progression System (Feature 4.7) is the **central player progression engine** of Phoenix. It aggregates activity from every other system — dialogue, vocabulary, quests, friendship, speaking, listening, reading, exploration, passport, and achievements — into a single **XP + level** model, unlocks **features by level**, gates **chapters** behind region completion or level requirements, and tracks **daily goals**, **learning percentages**, and **current objectives**.

It is the glue that makes the whole game feel like one journey: every action you take in the world quietly feeds a single, visible growth path.

## Architecture

```
feature/progression/
├── data/
│   ├── ProgressionModels.kt          # XP rules, levels, unlocks, chapters, progress models
│   ├── ProgressionEntities.kt        # Room persistence
│   ├── RoomProgressionRepository.kt  # Production Room-backed implementation
│   └── MockProgressionRepository.kt  # In-memory implementation for tests/dev
├── domain/
│   └── ProgressionRepository.kt      # Repository interface
├── viewmodel/
│   └── ProgressionViewModel.kt       # ViewModel observing all source systems
├── di/
│   └── ProgressionModule.kt          # Hilt binding
└── ui/
    ├── ProgressionScreen.kt          # Progression screen
    └── ProgressionComponents.kt      # Level card, learning bars, chapters, objectives, timeline
```

## XP Rules

`XpSource` defines the 12 XP sources. Every action in the game maps to exactly one source:

| Source | Base XP | Icon | Triggered by |
|--------|---------|------|--------------|
| DIALOGUE | 20 | 💬 | Dialogue completions |
| VOCABULARY_DISCOVERY | 10 | 🆕 | Word discoveries |
| QUEST_COMPLETION | 50 | 📜 | Quest completions |
| FRIENDSHIP_LEVEL_UP | 30 | 🤝 | Friendship level-ups |
| SPEAKING_PRACTICE | 10 | 🗣️ | Speaking exercises |
| LISTENING_PRACTICE | 10 | 👂 | Listening exercises |
| READING_PRACTICE | 10 | 📖 | Reading exercises |
| WRITING_PRACTICE | 10 | ✍️ | Writing practices |
| EXPLORATION | 25 | 🧭 | Region unlocks & completions |
| PASSPORT_STAMP | 15 | 📮 | Passport stamps earned |
| ACHIEVEMENT | 40 | 🏅 | Milestones, badges, mastered words |
| REVIEW | 15 | 🔁 | Review session completions |

## Level System (`XpCalculator`)

- **Max level:** 100
- **Requirement per level:** `100 + (level - 1) × 25` XP (level 1 needs 100, level 2 needs 125, …)
- `totalXpForLevel` sums requirements; `levelForTotalXp` inverts it; `xpIntoLevel` / `xpRemainingToNextLevel` / `progressInLevel` describe where a player sits inside the current level.

## Feature Unlocks

Features unlock purely by level (`FeatureUnlock.requiredLevel`):

| Feature | Required Level |
|---------|----------------|
| SPEAKING (口语练习) | 2 |
| LISTENING (聆听练习) | 3 |
| READING (阅读练习) | 4 |
| WRITING (书写练习) | 5 |
| QUEST_TYPES (新任务类型) | 5 |
| NPC_ACCESS (新 NPC) | 7 |
| CONVERSATIONS (新对话) | 8 |
| REGIONS (新区域) | 10 |

`PlayerProgress.unlockedFeatures` reflects everything earned so far; `nextFeatureToUnlock` points at the next carrot.

## Chapters

The 12 world regions (chapter 1–5, Qingyuan Village → Phoenix Summit) map 1:1 to chapters. Chapter `i` is unlocked when the previous region is COMPLETED **or** the player reaches level `1 + i/2` — a soft gate that keeps the story moving even without perfect region completion. `ChapterInfo` carries `unlockRequirement` (`requiredRegionId`, `requiredLevel`, `requiredQuestId`).

## Aggregation & Snapshot Deltas

Production binds the Room-backed `RoomProgressionRepository`. `MockProgressionRepository` is `@Singleton` and injects the 10 source repositories (game progress, world, quest, passport, vocabulary, friendship, discovery, pronunciation, listening, reading). On `refresh()` it:

1. **Snapshots** the current counters (dialogues, words discovered, quests completed, friendship levels, passport stamps, practices, regions, achievements).
2. **Applies deltas** versus the previous snapshot, awarding XP per source and recording daily activities.
3. **Rebuilds** `LearningProgress` (10 percentages + overall), `PlayerProgress` (level, chapter, story stage, unlocks, completion), and the 9 current `CurrentObjective`s.

This means XP flows automatically from *any* other system — no manual wiring needed per feature.

## Daily Progress

`DailyProgress` tracks XP earned today, activities completed (with a daily goal of 3), per-source counts, and a goal streak. Reaching the daily goal is itself a celebratory recent unlock (🎯).

## UI

`ProgressionScreen` (route `progression`, entry button in the Qingyuan Village bottom bar) shows:
1. **Player Level Card** — level badge, current chapter, XP bar, overall completion
2. **Daily Progress Card** — today's XP, activities vs goal, streak
3. **Learning Radar** — 10 learning bars (speaking, listening, reading, writing, vocabulary, conversation, quest, friendship, exploration, passport)
4. **Chapter Progress** — all 12 chapters with unlock state
5. **Objectives** — 9 current objectives with progress bars
6. **Recent Unlocks** — activity feed (capped at 20)
7. **Feature Unlock Timeline** — the 8 features and their unlock state

A demo XP button lets players try the flow instantly.

## Tests

- `ProgressionModelsTest` — XP curve, max level, feature unlock ordering, learning/objective/daily model math
- `ProgressionRepositoryTest` — first-run zero state, `awardXp` level-ups and feature unlocks, snapshot deltas from real source activity (dialogues, words, quests, friendship, stamps, practices, exploration, achievements), daily goal, chapter catalog, reset

## Entry Points

- Village: `QingyuanVillageScreen(onNavigateToProgression = ...)` button 「我的进度」
- Navigation: `Screen.Progression` route registered in `PhoenixApp`
