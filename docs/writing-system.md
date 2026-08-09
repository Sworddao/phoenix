# Writing & Hanzi Stroke Foundation System

## Overview

The Writing System gives players a lightweight **handwriting foundation** through hanzi stroke practice. It is deliberately scoped: handwriting is a documented product non-goal, so this system provides a stroke-order/direction practice loop rather than full freehand recognition. Every character is backed by a stroke model (`HanziStroke`), validated by a `WritingEngine` abstraction, and persisted to Room.

## Architecture

```
feature/writing/
├── data/
│   ├── WritingModels.kt             # Data classes, enums, badges, result statuses
│   ├── WritingEngine.kt             # Engine abstraction + session state + feedback
│   ├── MockWritingEngine.kt         # In-memory engine (order + direction validation)
│   ├── WritingEntities.kt, WritingDao.kt, WritingMappers.kt   # Room persistence
│   ├── RoomWritingRepository.kt     # Production Room-backed implementation
│   └── MockWritingRepository.kt     # In-memory implementation for tests/dev
├── domain/
│   └── WritingRepository.kt         # Repository interface
├── viewmodel/
│   └── WritingViewModel.kt          # ViewModel with stroke-by-stroke session flow
├── di/
│   └── WritingModule.kt             # Hilt bindings (repository + engine)
└── ui/
    ├── WritingScreen.kt             # Practice flow screen
    └── WritingComponents.kt         # Character card, direction buttons, result UI
```

Seed data lives in `data/seed/WritingSeedData.kt` (19 seeded characters with stroke data, ~30 exercises across three exercise types).

## Writing Engine Abstraction

`WritingEngine` decouples stroke validation from the rest of the feature:

```kotlin
interface WritingEngine {
    val name: String
    val isAvailable: Boolean
    fun startSession(character: HanziCharacter): EngineSessionState
    fun expectedStroke(sessionId: String): HanziStroke?
    fun recordStroke(sessionId: String, strokeIndex: Int, direction: StrokeDirection): EngineStrokeFeedback
    fun isComplete(sessionId: String): Boolean
    fun progress(sessionId: String): Float
    fun correctOrderCount(sessionId: String): Int
    fun correctDirectionCount(sessionId: String): Int
    fun reset(sessionId: String)
    fun endSession(sessionId: String)
    fun getEngineInfo(): WritingEngineInfo
}
```

- `EngineSessionState` tracks `nextStrokeIndex`, `strokesCompleted`, order/direction counters, `errorCount`, and completion.
- `EngineStrokeFeedback` reports per-stroke results: `wasOrderCorrect`, `wasDirectionCorrect`, `wasCorrect`, the expected stroke type/direction, and a localized message.
- **Advance semantics**: a stroke only advances the session when BOTH its index (order) and direction are correct. Wrong-order or wrong-direction taps increment `errorCount` and keep the pending stroke active so the player retries it — this keeps the UI and engine indices in sync and prevents a stuck exercise.
- `MockWritingEngine` (the current backend) validates each tap against the character's stroke list. A future implementation can plug a real stroke-recognition pipeline behind the same interface via the existing `WritingModule` binding.

## Data Models

### HanziCharacter / HanziStroke
A character's stroke model: `HanziCharacter` holds id, hanzi, pinyin, English, syllable tones, linked `wordId`, difficulty, XP reward, and ordered `strokes`. Each `HanziStroke` carries its `order`, `type` (8 `StrokeType`s: horizontal, vertical, left/right-falling, dot, hook, rising, turning), `direction` (6 `StrokeDirection`s), and name (EN + CN).

### WritingExercise
A single practice item with `type` — one of 3 `WritingExerciseType`s:
- `TRACE_STROKES` — trace each stroke of the character in order
- `STROKE_ORDER` — place the strokes in the correct order
- `DIRECTION_CHECK` — identify the correct direction of each stroke

plus `difficulty` (4 `WritingDifficulty`s), the embedded `character`, `prompt`, `xpReward`, optional `friendshipBonus`, `isUnlocked`, and `order`.

### WritingAttempt / WritingStrokeAnswer
`WritingAttempt` records an exercise result: per-stroke `WritingStrokeAnswer`s (stroke index, expected type/direction, `wasCorrect`, `attempts`), elapsed time, and timestamp. `correctStrokeCount`, `totalStrokeCount`, `wasCorrect`, and `accuracy` derive from the answers. Answers are built from the player's **actual** taps (each stroke records how many attempts it took), not fabricated as always-correct.

### WritingSession
An in-progress practice run: `exerciseIds`, `currentExerciseIndex`, accumulated attempts, `totalXpEarned` / `totalFriendshipBonus` / `totalCorrectStrokes`, `correctAttempts`, `isCompleted`.

### WritingProgress
Per-item tracking (itemId or wordId): attempts, correct attempts, `timesWritten`, stroke counts, best time, `successRate`, `strokeAccuracy`, `isMastered`, and `masteryLevel` computed from `WritingMastery`.

### WritingMastery
6 levels: `NEW` → `SEEN` (0.2 rate / 1 attempt) → `LEARNING` (0.4 / 3) → `FAMILIAR` (0.6 / 6) → `CONFIDENT` (0.75 / 10) → `MASTERED` (0.85 / 15).

### WritingStatistics
Rolled-up analytics: session/attempt/correct counts, total/correct strokes, streaks, characters written and mastered, average time per exercise, breakdowns by exercise type and difficulty, and badge state.

### WritingBadge
8 earnable badges: `write_first`, `write_streak_3`, `write_streak_7`, `write_streak_30`, `write_steady_hand`, `write_stroke_perfect`, `write_dialogue_ready`, `write_char_collector`. Progress is recomputed after every answer and badges flip to earned automatically.

### WritingResult / WritingResultStatus
`WritingResult` wraps an attempt with `xpEarned`, `friendshipBonusEarned`, `isNewPersonalBest`, streak info, a reward (first-character-written flag, new mastery, badge progress), and a `shouldCelebrate` helper. Repository calls return sealed `WritingResultStatus` variants (`ExerciseCompleted`, `SessionCompleted`, `StreakUpdated`, `BadgeEarned`, `ProgressUpdated`, `StrokeChecked`, `Success`, `Error`).

## Repository

`WritingRepository` provides:

- **Exercise queries** — all/by id/type/difficulty/word, unlocked, recommended
- **Progress** — per item, all progress, statistics, badges
- **Session lifecycle** — `startSession(config)`, `submitAnswer(attempt)`, `completeSession(session)`
- **Administration** — `updateProgress`, `unlockExercise`, `recordStreak`, `awardBadge`, `addExercises`

### Session Selection Behavior
- **By character/word** — a config's `characterIds` match both `character.id` and `character.wordId`, so vocabulary-word launches reuse seeded characters with full stroke data instead of falling back to stroke-less dynamic exercises.
- **Dynamic generation** — words with no seeded writing exercise generate a `write_dynamic_<wordId>` trace exercise from the vocabulary repository as a safe fallback.
- **Progressive default sessions** — a session started with the default config (no character ids, `TRACE_STROKES`) cycles across all 3 exercise types (`TRACE_STROKES` → `STROKE_ORDER` → `DIRECTION_CHECK`), making every type reachable from the village/dialogue entry point.

### MockWritingRepository Behavior
Production binds the Room-backed `RoomWritingRepository`; the mock mirrors the same behavior for tests and development (selection, streaks, rewards, badges, passport recording).

## ViewModel

`WritingViewModel` exposes `WritingUiState` with `session`, `exercises`, `currentExercise`, `strokesCompleted`, `expectedStrokeIndex`, `pendingStrokeAttempts`, `strokeAnswers`, `isExerciseComplete`, `lastStrokeFeedback`, `lastResult`, `statistics`, `isSessionComplete`, loading and error state.

Methods:

- `startPractice(wordId)` — starts a session (word-focused from vocabulary detail, or the progressive default from village/dialogue)
- `recordStroke(direction)` — feeds a tap into the engine; on a correct stroke advances the index and appends the real `WritingStrokeAnswer` (with attempts); on an incorrect stroke keeps the pending stroke active and surfaces feedback
- `nextExercise()` / `completeSession()` — navigate and finalize
- `dismissError()` — clear error state

The ViewModel never fabricates results: the submitted attempt reflects exactly which strokes were completed and how many attempts each took.

## UI Components

- **WritingProgressCard** — session progress bar, exercise counter, XP, streak line.
- **WritingCharacterCard** — large hanzi, pinyin + English, and stroke progress (`X / N 笔`).
- **StrokeDirectionButtons** — 6 direction buttons in two rows; disabled once the exercise completes.
- **WritingFeedbackCard** — per-stroke feedback (green correct / red incorrect) with the engine's message.
- **WritingResultCard** — exercise result with XP and the next-exercise button.
- **WritingCompletionDialog** — session summary with correct count / XP / total strokes / streak.

## System Integrations

### Vocabulary System
- Correct answers call `vocabularyRepository.incrementWritten(wordId)` (drives `timesWritten`)
- `VocabularyDetailScreen` writing button starts a word-focused session via `Screen.Writing.createRoute(wordId)`

### Quest System
- Seed quests include `WRITE_CHARACTERS` objectives (e.g., `QuestSeedData.kt`)
- Every correct answer advances `WRITE_CHARACTERS` objectives of all active quests (+1 each) via `updateObjectiveProgress`

### Game Progress System
- Every correct answer calls `gameProgressRepository.recordWritingPractice()`:
  - Increments `totalWritingPractices`
  - Marks the `FIRST_WRITING` milestone on first practice
- Progression system counts `WRITING_PRACTICE` XP (10/source) and a learning bar

### Friendship System
- Greeting-linked exercises (`greet_*`) grant `friendshipBonus` via `addFriendshipXp("grandma_mei", ...)` on success

### Passport System
- First-ever practice records a `WRITING_PRACTICE` entry ("第一次书写练习")
- Newly earned badges record `ACHIEVEMENT_UNLOCKED` entries

### Dialogue System
- `ActionType.PRACTICE_WRITING` — end-node action that unlocks linked writing exercises
- `ConversationCompleteCard` shows a "练习写" button navigating to the writing screen
- `CelebrationScreen` renders a `PRACTICE_WRITING` branch

### Navigation
- `Screen.Writing.createRoute(wordId)` reached from Qingyuan Village (dialogue completion), dialogue `PRACTICE_WRITING`, and vocabulary detail

## Testing

### Unit Tests

#### WritingEngineTest
- Session initialization, `expectedStroke`, single-stroke completion
- Wrong direction marked incorrect **without advancing** to the next stroke
- Wrong direction then correct retry completes the stroke (attempts/order/direction counters)
- Wrong index does not advance; full correct session completes; reset and endSession; engine info

#### WritingRepositoryTest (mock) & RoomWritingRepositoryTest (Robolectric)
- Exercise queries and seed-once behavior
- Session selection: by character id, by word id (seeded stroke data), dynamic generation for unknown words, progressive multi-type default sessions
- Rewards: XP, streak bonus (+5 from streak 2), friendship bonus, personal bests
- Wrong answers award no XP; correct answers update vocabulary `timesWritten`, game progress, quest objectives, and passport entries
- Mastery computation, badge awarding, streak statistics, and state persistence across repository instances

#### WritingViewModelTest
- Word-based practice loads a seeded exercise with stroke data
- Completing an exercise records real per-stroke attempts (retried strokes carry `attempts > 1`)
- Wrong-direction taps do not advance the pending stroke
- `nextExercise` advances through the session and resets stroke state

## Future Enhancements

- Real stroke-recognition / freehand drawing behind `WritingEngine`
- Stroke-order animations and a true canvas renderer
- More characters, vocab, and NPC-linked writing exercises
- Recall-from-memory exercises and writing-to-review signal for the review system
- Reduced-motion handling for any animated stroke guides
