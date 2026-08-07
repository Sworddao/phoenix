# Speaking & Pronunciation System

## Overview

The Speaking & Pronunciation System gives players their first real opportunity to **speak** Mandarin. It provides a complete offline speaking-practice workflow — exercise selection, guided recording with Bao's encouragement, per-word progress, streaks, badges, and rewards — while hiding speech analysis behind an engine abstraction so a real speech recognizer can be added later without touching higher layers.

## Architecture

```
feature/pronunciation/
├── data/
│   ├── PronunciationModels.kt       # Data classes and enums
│   ├── PronunciationEngine.kt       # Engine interface + MockPronunciationEngine
│   ├── SpeakingEntities.kt, SpeakingMappers.kt   # Room persistence
│   ├── RoomPronunciationRepository.kt # Production Room-backed implementation
│   └── MockPronunciationRepository.kt # In-memory implementation for tests/dev
├── domain/
│   └── PronunciationRepository.kt   # Repository interface
├── viewmodel/
│   └── PronunciationViewModel.kt    # ViewModel with recording state machine
├── di/
│   └── PronunciationModule.kt       # Hilt bindings (repository + engine)
└── ui/
    ├── PronunciationScreen.kt       # Practice flow screen
    └── PronunciationComponents.kt   # Cards, buttons, indicators
```

## Speech Engine Abstraction

`PronunciationEngine` decouples speech analysis from the rest of the feature:

```kotlin
interface PronunciationEngine {
    val name: String
    val isAvailable: Boolean
    val supportedLanguages: List<String>
    suspend fun initialize(): EngineResult
    suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult>
    suspend fun stopListening(): EngineResult
    suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult
    suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult
    fun getEngineInfo(): EngineInfo
    suspend fun shutdown()
}
```

- `MockPronunciationEngine` (the current backend) simulates offline recognition: it derives a similarity score from the recognized pinyin and the expected phrase and emits streamed partial results, so the full UI flow works with no permissions, network, or native libraries.
- A future implementation can swap in Android `SpeechRecognizer`, Vosk, or Whisper.cpp behind the same interface via the existing `PronunciationModule` binding.

## Data Models

### SpeakingExercise
A single practice item:
- `type` — `REPEAT_AFTER_NPC`, `VOCABULARY_WORD`, `DIALOGUE_PHRASE`, `TONE_PRACTICE`, `SENTENCE_BUILDING`, `FREESTYLE`
- `difficulty` — 5 levels (`SpeakingDifficulty`)
- `expectedText` / `expectedPinyin` / `expectedHanzi` — what the player says
- `wordId` / `phraseId` — link back to vocabulary/dialogue content
- `relatedNpcId` / `relatedQuestId` — source attribution
- `xpReward` / `friendshipBonus` — rewards granted on success
- `isUnlocked` / `order` — progression gating
- `displayText` — hanzi when available, otherwise pinyin

### PronunciationAttempt
A single evaluation result:
- Confidence, tone accuracy, and fluency scores (0..1)
- `overallScore` — average of the three
- `isHighConfidence` — confidence ≥ 0.7 and tone ≥ 0.6
- `feedbackType` — one of 8 encouraging `PronunciationFeedbackType`s
- `wasSuccessful` — passed the success threshold

### PronunciationSession
An in-progress practice run:
- `exerciseIds`, `currentExerciseIndex`, `attempts`
- `progress` — index-based completion fraction
- `averageConfidence`, `successfulAttempts`
- `totalXpEarned`, `totalFriendshipBonus` accumulated across attempts

### PronunciationProgress
Per-word tracking with:
- Attempt counts, best scores, streak data, practice time
- `successRate` — successful / total
- `masteryLevel` — computed via `SpeakingMastery` thresholds

### SpeakingMastery
5 levels: `NEW` → `LEARNING` (0.3 rate / 3 attempts) → `IMPROVING` (0.5 / 5) → `CONFIDENT` (0.7 / 10) → `MASTERED` (0.85 / 20).

### SpeakingStatistics
Rolled-up analytics: session/exercise/attempt counts, streaks, words practiced and mastered, average scores, breakdowns by exercise type and difficulty, and badge state.

### PronunciationBadge
8 earnable badges: `first_word`, `streak_3`, `streak_7`, `streak_30`, `confident_speaker`, `tone_master`, `conversation_ready`, `pronunciation_pro`. Progress is computed from statistics and badges flip to earned automatically.

### SpeakingQuestObjective
Future-proof objective type for quest-integrated speaking targets (exercise type, difficulty, target count, optional word list).

## Repository

`PronunciationRepository` provides:

- **Exercise queries** — all/by id/type/difficulty/word/phrase/npc/quest, unlocked, recommended (unlocked sorted by order, limited)
- **Progress** — per word, all progress, statistics, badges
- **Session lifecycle** — `startSession(config)`, `submitAttempt(attempt)`, `completeSession(session)`
- **Administration** — `updateProgress`, `unlockExercise`, `recordStreak`, `awardBadge`, `addExercises`
- **Evaluation** — `evaluatePronunciation` (audio path) and `evaluatePronunciationOffline` (text) returning evaluated `PronunciationAttempt`s

### MockPronunciationRepository Behavior
Production binds the Room-backed `RoomPronunciationRepository`; the mock below documents the in-memory behavior retained for tests and development.

- **Selection** — filters unlocked exercises by config (wordIds, phraseIds, type/difficulty); words without a matching exercise are **dynamically generated** (`dynamic_<wordId>`) from the vocabulary repository and added to the catalog
- **Success threshold** — `SUCCESS_THRESHOLD = 0.7`; `wasSuccessful` below it
- **Offline evaluation** — pinyin normalization + positional similarity; feedback tiers with `NICE_IMPROVEMENT` when the player improves on a previous attempt
- **Streaks** — daily boundary semantics; consecutive days increment, missed days reset; longest streak tracked
- **Rewards** — XP per exercise (streak bonus +5 from day 2+), friendship bonus for NPC-linked exercises, personal-best detection
- **Badges** — recomputed after every attempt; newly earned badges are recorded to the passport

## System Integrations

### Vocabulary System
- Successful attempts call `vocabularyRepository.incrementSpoken(wordId)` (drives `timesSpoken`)
- `VocabularyDetailScreen` practice button starts a session for that word
- Dynamic exercise generation for discovered words lacking an exercise

### Quest System
- Every successful attempt advances `PRACTICE_SPEAKING` objectives of all active quests (+1 each) via `updateObjectiveProgress`

### Game Progress System
- Every successful attempt calls `gameProgressRepository.recordSpeakingPractice()`, which:
  - Increments `totalSpeakingPractices`
  - Marks the `FIRST_SPEAKING` milestone on first practice

### Friendship System
- NPC-linked exercises grant `friendshipBonus` via `addFriendshipXp` on success

### Passport System
- First-ever practice records a `SPEAKING_PRACTICE` entry
- Newly earned badges record `ACHIEVEMENT_UNLOCKED` entries
- `MockPassportRepository.recordEntry` now persists entries so they appear in the passport timeline

### Dialogue System
- New `ActionType.PRACTICE_SPEAKING` (`@SerialName("practice_speaking")`) — end-node action with comma-separated exercise ids
- `DialogueViewModel` unlocks those exercises and exposes `isPracticeAvailable`
- `ConversationCompleteCard` shows a "练习说" button navigating to the pronunciation screen
- Grandma Mei's conversation unlocks `pron_ex_dlg_hao_chi` and `pron_ex_dlg_meet`

### Listening System Integration
- Listening exercises with a `relatedSpeakingExerciseId` unlock the linked speaking exercise on a correct answer, so listening practice feeds directly into speaking practice
- Both systems share the reduced-motion helper `rememberReducedMotion()` from `ui/components/Accessibility.kt` for pulsing/recording animations

## ViewModel

`PronunciationViewModel` exposes `PronunciationUiState` with:

- `session`, `exercises`, `currentExercise`, `currentProgress`, `statistics`
- Recording state: `isRecording`, `isListening`, `isSpeaking`
- Results: `lastResult`, `isSessionComplete`, `transcript`
- Loading and error state

Methods:

- `startPractice()` — builds a session (from a `wordId` argument or defaults) and loads the first exercise
- `demonstrate()` — toggles Bao's speaking state (audio playback placeholder for the real engine)
- `startRecording()` — starts the engine's `startListening` flow, streams partial transcripts into state, evaluates the final transcript offline (`evaluatePronunciationOffline`), attaches exercise metadata, and submits the attempt
- `nextExercise()` / `repeatExercise()` — session navigation
- `dismissError()` — clear error state

The ViewModel subscribes to the engine's `startListening` flow and streams partial transcripts into state; the mock emits the expected phrase so recording always evaluates successfully.## UI Components

### PronunciationScreen
Full practice flow: loading state, exercise card with Bao tip, demonstration button, circular speaking button, recording indicator with waveform placeholder, per-attempt feedback card, progress card, and completion card with XP/streak summary.

### SpeakingButton
Large 96dp circular button with a reduced-motion-aware pulse animation while recording.

### RecordingIndicator / WaveformPlaceholder
Visual feedback during listening; respects reduced motion.

### BaoPronunciationTip
Bao's encouraging tip, pinyin display of the target phrase, and exercise context.

### SpeakingProgressCard / PronunciationResultCard / SpeakingCompleteCard
Session progress, feedback-only results (numeric scores are never shown to beginners — only encouraging messages), and completion summary with rewards.

### Accessibility
- `rememberReducedMotion()` reads `Settings.Global.ANIMATOR_DURATION_SCALE` and disables pulsing animations
- Large touch targets, contrast-friendly colors, and text-only feedback

## Navigation

- Route: `Screen.Pronunciation` = `pronunciation/{wordId}` with `createRoute(wordId: String = "")`
- Entry points:
  - Dialogue completion card ("练习说") — navigates with the unlocked exercise set
  - `VocabularyDetailScreen` practice button — navigates with the word id
- Registered in `PhoenixApp.kt` NavHost with the wordId argument

## Exercises Catalog

11 initial exercises:

| id | type | phrase | source |
|----|------|--------|--------|
| `pron_ex_greet_hello` | VOCABULARY_WORD | nǐ hǎo 你好 | greet_001 (Grandma Mei) |
| `pron_ex_greet_thanks` | VOCABULARY_WORD | xiè xie 谢谢 | greet_003 (Grandma Mei) |
| `pron_ex_greet_goodbye` | VOCABULARY_WORD | zài jiàn 再见 | greet_002 |
| `pron_ex_tone_hello` | TONE_PRACTICE | nǐ hǎo ma 你好吗 | greet_007 (Grandma Mei) |
| `pron_ex_food_eat` | VOCABULARY_WORD | chī fàn 吃饭 | food_001 (Owner Lin) |
| `pron_ex_food_drink` | VOCABULARY_WORD | hē 喝 | food_002 (Owner Lin) |
| `pron_ex_dlg_hao_chi` | DIALOGUE_PHRASE | hǎo chī 好吃 | Grandma Mei dialogue |
| `pron_ex_dlg_meet` | DIALOGUE_PHRASE | hěn gāo xìng rèn shi nǐ 很高兴认识你 | Grandma Mei dialogue |
| `pron_ex_dlg_order_tea` | DIALOGUE_PHRASE | wǒ yào yī bēi chá 我要一杯茶 | tea house dialogue |
| `pron_ex_dlg_dumplings` | DIALOGUE_PHRASE | wǒ yào liǎng ge bāo zi 我要两个包子 | dumpling shop dialogue |
| `pron_ex_freestyle_intro` | FREESTYLE | wǒ jiào... 我叫... | free speaking |

## Testing

### Unit Tests

#### PronunciationModelsTest
- Attempt scoring (`overallScore`, `isHighConfidence`)
- Session progress, averages, successful attempt counting
- Result celebration and feedback message logic
- Exercise display text, mastery thresholds, progress success rate
- Statistics overall success rate, badge catalog completeness
- Quest objective progress and session config defaults

#### PronunciationRepositoryTest
- Exercise queries (by id, type, word, npc, quest, unlocked, recommended)
- Session lifecycle (start, dynamic word exercises, complete, mismatch errors)
- Progress and statistics accumulation across attempts
- Streak build-up, reset, and badge thresholds
- Rewards: XP, friendship bonus, personal bests
- Badge awarding and passport recording
- System integration: vocabulary `timesSpoken`, quest objectives, game progress milestone, passport entries
- Offline evaluation: perfect/partial/poor matches and improvement detection
- Recorded streak and badge APIs

#### DialogueViewModelActionTest (updated)
- Verifies `PRACTICE_SPEAKING` action processing still passes with the injected pronunciation repository

## Future Enhancements

- Real speech recognition backend (Android SpeechRecognizer / Vosk / Whisper.cpp) behind `PronunciationEngine`
- Audio playback for phrases and examples
- Pronunciation guide with tone visualization
- More NPC-linked dialogue exercises and quest-linked speaking objectives (`SpeakingQuestObjective`)
- Speaking results surfaced in quest completion and passport stamps
- Room persistence for pronunciation progress and statistics
