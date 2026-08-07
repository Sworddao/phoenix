# Reading & Hanzi Foundation System

## Overview

The Reading System lets players practice **reading** Mandarin through a pinyin-first, hanzi-later progression — the written counterpart to the Listening (4.5) and Speaking (4.4) Systems. Every hanzi is hidden by default and revealed only through gameplay (tap-to-reveal, auto-reveal, or an accessibility toggle that shows hanzi immediately). All rendering is behind a `HanziRenderer` abstraction so a real font/OCR/TTS pipeline can be added later without touching higher layers.

## Architecture

```
feature/reading/
├── data/
│   ├── ReadingModels.kt             # Data classes, enums, badges, result statuses
│   ├── HanziRenderer.kt             # Hanzi renderer interface + MockHanziRenderer
│   ├── ReadingEntities.kt, ReadingDao.kt, ReadingMappers.kt   # Room persistence
│   ├── RoomReadingRepository.kt     # Production Room-backed implementation
│   └── MockReadingRepository.kt     # In-memory implementation for tests/dev
├── domain/
│   └── ReadingRepository.kt         # Repository interface
├── viewmodel/
│   └── ReadingViewModel.kt          # ViewModel with reveal + answer flow
├── di/
│   └── ReadingModule.kt             # Hilt bindings (repository + renderer)
└── ui/
    ├── ReadingScreen.kt             # Practice flow screen
    └── ReadingComponents.kt         # Hanzi card, choice, hint, completion UI
```

## Hanzi Renderer Abstraction

`HanziRenderer` decouples hanzi rendering from the rest of the feature:

```kotlin
interface HanziRenderer {
    val name: String
    val isAvailable: Boolean
    fun render(hanzi: String?, pinyin: String, mode: CharacterRevealState): RenderedHanzi
    fun tonesOf(pinyin: String): List<Int>
    fun getRendererInfo(): HanziRendererInfo
}
```

- **Reveal modes** are modelled by `CharacterRevealState` (7 states): `HIDDEN`, `PINYIN_ONLY`, `HANZI_ONLY`, `HANZI_AND_PINYIN`, `TONE_COLORED_PINYIN`, `TAP_TO_REVEAL`, `AUTO_REVEAL`.
- `RenderedHanzi` carries the visible hanzi (or null when masked), a masked-hanzi string (`▢` per character), plain pinyin, and tone-colored pinyin spans.
- `MockHanziRenderer` (the current backend) analyzes tone diacritics per syllable: `āēīōūǖ`=tone 1, `áéíóúǘ`=tone 2, `ǎěǐǒǔǚ`=tone 3, `àèìòùǜ`=tone 4, else neutral. `ToneColor` maps 5 colors (NEUTRAL, TONE1–TONE4) to the spans.
- A future implementation can swap in a real font-rendering / OCR / accessibility pipeline behind the same interface via the existing `ReadingModule` binding.

## Data Models

### ReadingExercise
A single practice item:
- `type` — one of 8 `ReadingExerciseType`s: `MATCH_SPOKEN_TO_WRITTEN`, `MATCH_PINYIN_TO_HANZI`, `MATCH_HANZI_TO_MEANING`, `SENTENCE_READING`, `PHRASE_RECOGNITION`, `CHARACTER_RECOGNITION`, `CONTEXT_READING`, `NPC_DIALOGUE_READING`
- `difficulty` — 4 levels (`ReadingDifficulty`: BEGINNER → ADVANCED)
- `hanzi` / `pinyin` / `english` / `syllableTones` — the written target, its pinyin, meaning, and per-syllable tone data
- `prompt` / `context` — the on-screen instruction and story setting
- `choices` + `correctChoiceIndex` — the answer set; `correctChoice` resolves the winning option
- `relatedNpcId` / `relatedQuestId` / `relatedWordId` — content attribution
- `relatedSpeakingExerciseId` / `relatedListeningExerciseId` — cross-links unlocked after a correct read
- `xpReward` / `friendshipBonus` — rewards granted on success
- `isUnlocked` / `order` — progression gating
- `card` — builds the `HanziCard` used by the reveal display

### HanziCard
The display unit: hanzi, pinyin, English, syllable tones, and a `revealState` defaulting to `PINYIN_ONLY` (pinyin-first philosophy).

### ReadingAttempt
A single answer: chosen choice, `wasCorrect`, `revealedHanziBeforeAnswer` (used to reward hanzi-free reading), `timeTakenMs`, timestamp.

### ReadingSession
An in-progress practice run: `exerciseIds`, `currentExerciseIndex`, `attempts`, accumulated `totalXpEarned` / `totalFriendshipBonus` / `totalReveals`, `correctAttempts`, and `revealCount` via attempt counting.

### ReadingProgress
Per-item tracking (itemId or wordId): attempts, correct attempts, times read, times revealed, `hasRevealedHanzi`, best time, `successRate`, and `masteryLevel` computed from `ReadingMastery`.

### ReadingMastery
6 levels: `NEW` → `SEEN` (0.2 rate / 1 attempt) → `LEARNING` (0.4 / 3) → `FAMILIAR` (0.6 / 6) → `CONFIDENT` (0.75 / 10) → `MASTERED` (0.85 / 15).

### ReadingStatistics
Rolled-up analytics: session/attempt/correct counts, total reveals, streaks, words and characters read, words mastered, average time per exercise, breakdowns by exercise type and difficulty, and badge state.

### ReadingBadge
8 earnable badges: `read_first`, `read_streak_3`, `read_streak_7`, `read_streak_30`, `read_quick_eye`, `read_accurate`, `read_dialogue_ready`, `read_char_collector`. Progress is computed from statistics and badges flip to earned automatically.

### ReadingResult / ReadingResultStatus
`ReadingResult` wraps an attempt with `xpEarned`, `friendshipBonusEarned`, `isNewPersonalBest`, streak info, reward (first-word-read flag, new mastery, badge progress), and a `shouldCelebrate` helper. Repository calls return sealed `ReadingResultStatus` variants (`ExerciseCompleted`, `SessionCompleted`, `StreakUpdated`, `BadgeEarned`, `ProgressUpdated`, `RevealRecorded`).

## Repository

`ReadingRepository` provides:

- **Exercise queries** — all/by id/type/difficulty/word/npc/quest, unlocked, recommended
- **Progress** — per item, all progress, statistics, badges
- **Session lifecycle** — `startSession(config)`, `submitAnswer(attempt)`, `completeSession(session)`
- **Administration** — `updateProgress`, `unlockExercise`, `recordStreak`, `awardBadge`, `addExercises`, `recordReveal`

### MockReadingRepository Behavior
Production binds the Room-backed `RoomReadingRepository`; the mock below documents the in-memory behavior retained for tests and development.

- **Selection** — filters unlocked exercises by config (wordIds first, then npcId/questId, then type+difficulty); words without a matching exercise are **dynamically generated** (`read_dynamic_<wordId>`) from the vocabulary repository with safe hanzi fallback
- **Streaks** — daily-boundary semantics; consecutive days increment, missed days reset; longest streak tracked and mirrored into statistics
- **Rewards** — XP per exercise (streak bonus +5 from streak 2+), friendship bonus for NPC-linked exercises, personal-best detection (first attempt or faster time)
- **Badges** — recomputed after every answer; newly earned badges recorded to the passport
- **Reveals** — `recordReveal(wordId)` tracks reveal counts in progress and statistics

## ViewModel

`ReadingViewModel` exposes `ReadingUiState` with `session`, `exercises`, `currentExercise`, `currentProgress`, `statistics`, `lastResult`, `selectedChoiceId`, `revealMode` (default `PINYIN_ONLY`), `isHanziRevealed`, `autoRevealDelayMs` (default 2000ms, min 500ms), `isSessionComplete`, loading and error state.

Methods:

- `startPractice(wordId, showHanzi)` — builds a session (from a vocabulary word or defaults); `showHanzi` (accessibility setting) switches the reveal mode to `HANZI_AND_PINYIN`
- `revealHanzi()` — reveals the hanzi and records the reveal via `recordReveal`
- `setAutoRevealDelay(delayMs)` — configures the auto-reveal timer for accessibility
- `selectChoice(choiceId)` — submits the answer with elapsed time; marks `revealedHanziBeforeAnswer`
- `nextExercise()` / `completeSession()` — navigate and finalize
- `dismissError()` — clear error state

The ViewModel exposes the `HanziRenderer` directly to the UI for rendering current/exercise hanzi.

## UI Components

- **HanziDisplayCard** — the reveal card: masked hanzi (`▢▢`), plain pinyin, tone-colored pinyin, and hanzi depending on the current reveal state.
- **ToneColoredPinyin** — pinyin with per-syllable tone colors from the renderer.
- **ReadingExerciseCard** — prompt, context, and the current hanzi display.
- **ReadingChoiceCard** — Button-styled answer options that reveal correct/incorrect states after an answer.
- **ReadingProgressCard** — session progress bar, XP, streak line.
- **BaoReadingHint** — Bao's rotating encouragement with a gentle hop animation (reduced-motion aware).
- **RevealButton** — tap-to-reveal action shown in TAP_TO_REVEAL mode.
- **ReadingCompletionDialog** — session summary dialog with correct count / XP / streak.

The reading screen starts each exercise in pinyin-first mode; hanzi appears only after a reveal (tap, auto, or accessibility toggle).

## System Integrations

### Vocabulary System
- Correct answers call `vocabularyRepository.incrementRead(wordId)` (drives `timesRead`)
- `VocabularyDetailScreen` reading button starts a session for that word and shows a "阅读" stats column
- Dynamic exercise generation for discovered words lacking an exercise

### Quest System
- `quest_order_tea` gains objective `obj_3_5` of type `READ_CHARACTERS` ("阅读茶馆菜单上的汉字", target 4)
- Every correct answer advances `READ_CHARACTERS` objectives of all active quests (+1 each) via `updateObjectiveProgress`

### Game Progress System
- Every correct answer calls `gameProgressRepository.recordReadingPractice()`:
  - Increments `totalReadingPractices`
  - Marks the `FIRST_READING` milestone on first practice

### Friendship System
- NPC-linked exercises grant `friendshipBonus` via `addFriendshipXp` on success

### Passport System
- First-ever practice records a `READING_PRACTICE` entry
- Newly earned badges record `ACHIEVEMENT_UNLOCKED` entries

### Pronunciation & Listening Systems
- Exercises with `relatedSpeakingExerciseId` unlock the linked speaking exercise on a correct answer
- Exercises with `relatedListeningExerciseId` unlock the linked listening exercise on a correct answer (reading reinforces hearing)

### Dialogue System
- New `ActionType.PRACTICE_READING` — end-node action with comma-separated exercise ids
- `DialogueViewModel` unlocks those exercises and exposes `isReadingPracticeAvailable`
- `ConversationCompleteCard` shows a "练习读" button navigating to the reading screen
- NPC bubbles with hanzi show a "阅读这句话" action directly in `DialogueBubble`
- Grandma Mei's conversation unlocks `read_ex_greet_hello` and `read_ex_greet_thanks`

### Celebration System
- `CelebrationScreen` renders a `PRACTICE_READING` branch ("阅读练习解锁") with the reading icon

## Testing

### Unit Tests

#### ReadingRepositoryTest
- Exercise queries (by id, type, difficulty, word, npc, quest, unlocked, recommended)
- Session lifecycle (start, dynamic word exercises, complete, mismatch errors)
- Progress and statistics accumulation across answers, reveal tracking
- Streak build-up, reset, and `read_streak_3` badge earning
- Rewards: XP, streak bonus, friendship bonus, personal bests
- Badge awarding and passport recording
- System integration: vocabulary `timesRead`, quest `READ_CHARACTERS` objectives, game progress milestone, passport entries, speaking/listening exercise unlocks
- Hanzi renderer tone detection (`tonesOf`)

#### ReadingModelsTest
- Exercise correct choice resolution and `card` construction
- Session progress and attempt/reveal counts
- Result celebration and feedback logic (incl. first-word-read reward)
- Mastery/difficulty/type/reveal-state/badge-catalog completeness

#### DialogueViewModelActionTest (updated)
- Verifies `PRACTICE_READING` action processing still passes with the injected reading repository

#### PassportModelsTest (updated)
- Verifies the `READING_PRACTICE` entry type is present in `EntryType`

## Future Enhancements

- Real hanzi renderer / font pipeline or packaged hanzi assets behind `HanziRenderer`
- Stroke-order animations and writing practice
- More vocab, NPC, and quest-linked reading exercises
- Persistent (Room) persistence for reading progress
- Reading speed metrics and comprehension quizzes
