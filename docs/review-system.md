# Adaptive Review & Spaced Repetition System

## Overview

The Review System (Feature 4.8) is an **adaptive spaced repetition engine** that turns everything the player does in Phoenix — vocabulary, dialogues, speaking, listening, reading, NPC conversations, quests, friendships, and exploration — into smart, scheduled review sessions. Instead of a static flashcard list, every word carries a **memory strength model** that is updated by every answer, every practice mode, and every conversation success. The engine then decides *when* to re-test each word and *how often*, so frequently missed words return sooner while mastered words fade into longer intervals.

Every completed review session feeds back into the Progression System (15 XP per session via `XpSource.REVIEW`), so review is part of the same growth loop as every other activity.

## Architecture

```
feature/review/
├── data/
│   ├── ReviewModels.kt              # Memory model, schedules, sessions, results, engine
│   └── MockReviewRepository.kt      # Mock implementation scheduling from all source systems
├── domain/
│   └── ReviewRepository.kt          # Repository interface (Room-replaceable)
├── viewmodel/
│   └── ReviewViewModel.kt           # Dashboard + session ViewModel
├── di/
│   └── ReviewModule.kt              # Hilt binding
└── ui/
    ├── ReviewScreen.kt              # Review dashboard + session screens
    └── ReviewComponents.kt          # Today card, recommendations, stats, session cards
```

## Spaced Repetition Engine (`SpacedRepetitionEngine`)

Pure, deterministic scheduling logic (no randomness, fully unit-tested):

**Stages & intervals** (calculated, never hardcoded in callers):

| Stage | Interval |
|-------|----------|
| 0 | 10 minutes |
| 1 | 1 day |
| 2 | 3 days |
| 3 | 7 days |
| 4 | 14 days |
| 5 | 30 days |
| 6 | 90 days |

**Stage transitions** (`nextStage`):
- Wrong answer → drop 2 stages (minimum 0) — repeated failures collapse the interval back to 10 minutes
- Correct answer → advance 1 stage; a perfect answer (score ≥ 0.95) advances 2 stages — *unless* the word is recovering (≥ 2 consecutive failures), which caps it at 1
- Stage cap is 6; intervals are clamped via `intervalForStage`

**Mastery**: a word is mastered when strength ≥ 0.8 (`MASTERY_THRESHOLD`).

## Memory Model (`MemoryStrength`)

Per-word state:

- `strength` — composite score: `0.4 × confidence + 0.4 × recall + 0.2 × averageScore`
- `confidence` — +0.15 × score on a correct answer (capped at 1), −0.25 on a wrong answer (floor 0.05)
- `correctAnswers` / `incorrectAnswers` / `reviewCount` / `averageScore` (weighted)
- `speakingAccuracy`, `listeningAccuracy`, `readingAccuracy`, `conversationSuccess` — fed by the matching review type via `withPractice`, and aggregated into `accuracy`
- `streak`, `consecutiveFailures` — drive recovering behavior and the rec_failures recommendation
- `lastReviewAt` / `nextReviewAt` — schedule anchors
- `overallAccuracy` — correct ÷ review count

Existing vocabulary state is honored on first seed: mastery maps to starting strength (MASTERED 0.85, FAMILIAR 0.65, LEARNING 0.5, else 0.4) and `reviewCount`/`correctAnswers` inherit from `timesReviewed`. Only words still learning (UNKNOWN/SEEN/LEARNING) get initial review items; FAMILIAR/MASTERED words are tracked but not re-pushed into the queue.

## Review Sources

`ReviewSource` (9): VOCABULARY, DIALOGUE, SPEAKING, LISTENING, READING, NPC_CONVERSATION, QUEST, FRIENDSHIP, EXPLORATION.
`ReviewType` (8): CONVERSATION, LISTENING, SPEAKING, READING, MIXED, NPC_CHALLENGE, QUEST_REVIEW, DAILY_REVIEW.

`MockReviewRepository` is `@Singleton` and injects the 10 source repositories (vocabulary, game progress, quest, friendship, world, passport, pronunciation, listening, reading, progression). On `refresh()` it:

1. **Seeds** memory entries + review items for discovered words needing review (once).
2. **Snapshots** source counters (words discovered, dialogues, quests completed, friendship levels, passport stamps, speaking/listening/reading practices, regions unlocked).
3. **Applies deltas** vs the previous snapshot, scheduling one review item per activity (quest completions → QUEST_REVIEW tied to the quest, friendship level-ups → NPC_CHALLENGE tied to the NPC, stamps/regions → EXPLORATION, etc.).
4. **Publishes** today's reviews (≤ 10, sorted by priority), upcoming reviews, memory strengths, statistics, daily review, and recommendations.

## Sessions & Adaptive Flow

- `startSession(type)` — DAILY/MIXED take up to 5 due items; typed sessions filter by type and fall back to any due items. Empty → error "今日暂无待复习内容".
- `submitAnswer(itemId, correct, score)` — updates the memory model, computes the next stage/interval, **reschedules the item** (it leaves "today" and appears in "upcoming" at its new due time), records history, increments the vocabulary review count, and updates statistics + daily activity.
- `completeSession(sessionId)` — marks the session complete, adds 15 XP (`XpSource.REVIEW`, 复习) through `progressionRepository.awardXp`, and publishes a `SessionCompleted` result with accuracy and XP.

Adaptive difficulty is derived from strength (`ReviewDifficulty`: NEW → LEARNING → FAMILIAR → MASTERED) and drives both item presentation and the weakest-first ordering.

## UI

`ReviewScreen` (route `review`, entry button 「复习」 in the Qingyuan Village bottom bar) shows:
1. **Today's Review Card** — due count vs daily goal of 5 with progress
2. **Bao Recommendation Card** — personalized suggestions (daily prompt, failure alerts, weak words, mastered count)
3. **Review Type Chips** — start a session of any of the 8 types
4. **Review Statistics** — total reviews, accuracy, mastered words, streak, sessions
5. **Upcoming Reviews** — future items with due times
6. **Memory Strengths** — weakest-first list with strength bars

`ReviewSessionScreen` (route `review_session/{type}`) presents one item at a time with 记得/不记得 answer buttons, live progress, and a completion dialog showing accuracy and XP earned.

## Tests

- `SpacedRepetitionEngineTest` (41) — interval tables/clamping, stage transitions incl. recovery, memory adjustments, practice modes, difficulty mapping, priority, recall decay
- `ReviewModelsTest` (34) — enums, memory model math, schedules, items, sessions, statistics, daily goal, results
- `ReviewRepositoryTest` (36) — first-refresh seeding, snapshot deltas from real source activity, today/upcoming semantics, answering flow, sessions + XP to progression, statistics/daily goal/recommendations, reset
- `ReviewViewModelTest` (9) — dashboard load, session start/error, answering, completion, dismissal

## Entry Points

- Village: `QingyuanVillageScreen(onNavigateToReview = ...)` button 「复习」
- Navigation: `Screen.Review` / `Screen.ReviewSession(type)` routes registered in `PhoenixApp`
