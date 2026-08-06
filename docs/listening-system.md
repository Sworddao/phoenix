# Listening & Audio Comprehension System

## Overview

The Listening System lets players practice **understanding** Mandarin through spoken audio — the receptive counterpart to the Speaking & Pronunciation System. It provides a complete offline listening-practice workflow: session selection, audio playback with slow/replay controls that model a real TTS backend, per-item progress, streaks, badges, and rewards, all behind an audio-engine abstraction so a real media player (ExoPlayer / Media3) or TTS can be added later without touching higher layers.

## Architecture

```
feature/listening/
├── data/
│   ├── ListeningModels.kt            # Data classes, enums, badges, result statuses
│   ├── AudioEngine.kt                # Audio engine interface + MockAudioEngine
│   └── MockListeningRepository.kt    # Mock implementation with system integrations
├── domain/
│   └── ListeningRepository.kt        # Repository interface
├── viewmodel/
│   └── ListeningViewModel.kt         # ViewModel with playback + answer flow
├── di/
│   └── ListeningModule.kt            # Hilt bindings (repository + audio engine)
└── ui/
    ├── ListeningScreen.kt            # Practice flow screen
    ├── ListeningComponents.kt        # Player, exercise, choice, hint, completion UI
    └── ListeningScreen.kt            # Screen route
```

## Audio Engine Abstraction

`AudioEngine` decouples playback from the rest of the feature:

```kotlin
interface AudioEngine {
    val name: String
    val isAvailable: Boolean
    val supportedFormats: List<String>
    suspend fun initialize(): AudioEngineResult
    suspend fun play(clip: AudioClip, playbackRate: Float = 1f): AudioEngineResult
    suspend fun pause(): AudioEngineResult
    suspend fun resume(): AudioEngineResult
    suspend fun stop(): AudioEngineResult
    fun getPlaybackState(): Flow<AudioPlaybackStateInfo>
    fun getEngineInfo(): AudioEngineInfo
    suspend fun shutdown()
}
```

- Playback state is modelled by `AudioPlaybackState` (`IDLE / LOADING / PLAYING / PAUSED / COMPLETED / ERROR`) and streamed via `AudioPlaybackStateInfo` (state, clip id, playback rate, position, duration).
- `AudioEngineFeature` flags what a backend supports (`OFFLINE_PLAYBACK`, `SLOW_PLAYBACK`, `LOOP_REPLAY`, `STREAMING`, `VOLUME_CONTROL`) and is surfaced through `AudioEngineInfo`.
- `MockAudioEngine` (the current backend) simulates playback offline: it sets the state to `PLAYING`, waits for the clip duration scaled by the playback rate (0.75x slow = longer), then transitions to `COMPLETED`. No permissions, network, or native libs required.
- A future implementation can swap in ExoPlayer / Media3 (or a real TTS that fetches clips from a CDN) behind the same interface via the existing `ListeningModule` binding.

## Data Models

### ListeningExercise
A single practice item:
- `type` — one of 8 `ListeningExerciseType`s: `HEAR_AND_CHOOSE_MEANING`, `HEAR_AND_IDENTIFY_VOCABULARY`, `HEAR_AND_MATCH_IMAGE`, `HEAR_AND_CHOOSE_NPC_RESPONSE`, `HEAR_NUMBERS`, `HEAR_GREETINGS`, `HEAR_DIRECTIONS`, `HEAR_FOOD_ORDERS`
- `difficulty` — 4 levels (`ListeningDifficulty`: BEGINNER → ADVANCED)
- `clip` — the `AudioClip` that is "played"
- `prompt` / `context` — the on-screen instruction and story setting
- `choices` + `correctChoiceIndex` — the answer set; `correctChoice` resolves the winning option
- `relatedNpcId` / `relatedQuestId` / `relatedWordId` — content attribution
- `relatedSpeakingExerciseId` — cross-link to a speaking exercise unlocked after a correct listen
- `xpReward` / `friendshipBonus` — rewards granted on success
- `isUnlocked` / `order` — progression gating

### AudioClip
The playable unit: `text` (pinyin), optional `hanzi`, `english`, audio path, duration, and optional `wordId`/`phraseId`/`npcId`.

### ListeningAttempt
A single answer: chosen choice, `wasCorrect`, `replayCount` (how many times the player replayed before answering), `timeTakenMs`, timestamp.

### ListeningSession
An in-progress practice run: `exerciseIds`, `currentExerciseIndex`, `attempts`, accumulated `totalXpEarned` / `totalFriendshipBonus`, `correctAttempts` via attempt counting.

### ListeningProgress
Per-item tracking (itemId or wordId): attempts, correct attempts, replay count, best time, `successRate`, and `masteryLevel` computed from `ListeningMastery`.

### ListeningMastery
5 levels: `NEW` → `LEARNING` (0.3 rate / 3 attempts) → `IMPROVING` (0.5 / 5) → `CONFIDENT` (0.7 / 10) → `MASTERED` (0.85 / 20).

### ListeningStatistics
Rolled-up analytics: session/attempt/correct counts, total replay count, total and average time listened, streaks, words practiced and mastered, breakdowns by exercise type and difficulty, and badge state.

### ListeningBadge
8 earnable badges: `listen_first`, `listen_streak_3`, `listen_streak_7`, `listen_streak_30`, `listen_quick_ear`, `listen_accurate`, `listen_npc_ready`, `listen_word_collector`. Progress is computed from statistics and badges flip to earned automatically.

### ListeningResult / ListeningResultStatus
`ListeningResult` wraps an attempt with `xpEarned`, `friendshipBonusEarned`, `isNewPersonalBest`, streak info, badge progress, and a `shouldCelebrate` helper. Repository calls return sealed `ListeningResultStatus` variants (`ExerciseCompleted`, `SessionCompleted`, `StreakUpdated`, `BadgeEarned`, `ProgressUpdated`, `ReplayRecorded`).

## Repository

`ListeningRepository` provides:

- **Exercise queries** — all/by id/type/difficulty/word/npc/quest, unlocked, recommended
- **Progress** — per item, all progress, statistics, badges
- **Session lifecycle** — `startSession(config)`, `submitAnswer(attempt)`, `completeSession(session)`
- **Administration** — `updateProgress`, `unlockExercise`, `recordStreak`, `awardBadge`, `addExercises`, `recordReplay`

### MockListeningRepository Behavior

- **Selection** — filters unlocked exercises by config (wordIds first, then npcId/questId, then type+difficulty); words without a matching exercise are **dynamically generated** (`listen_dynamic_<wordId>`) from the vocabulary repository
- **Streaks** — daily-boundary semantics; consecutive days increment, missed days reset; longest streak tracked and mirrored into statistics
- **Rewards** — XP per exercise (streak bonus +5 from day 2+), friendship bonus for NPC-linked exercises, personal-best detection (first attempt or faster time)
- **Badges** — recomputed after every answer; newly earned badges recorded to the passport

## ViewModel

`ListeningViewModel` exposes `ListeningUiState` with `session`, `exercises`, `currentExercise`, `currentProgress`, `statistics`, `lastResult`, `selectedChoiceId`, `playbackState`, `playbackRate` (1x / 0.75x), `replayCount`, `isSessionComplete`, loading and error state.

Methods:

- `startPractice(wordId)` — builds a session (from a vocabulary word or defaults) and loads the first exercise
- `playCurrent()` — starts/pauses/resumes the active clip through the engine
- `replay()` — restarts the clip and records a replay via `recordReplay`
- `toggleSlowPlayback()` — switches between 1x and 0.75x
- `selectChoice(choiceId)` — submits the answer with elapsed time
- `nextExercise()` / `completeSession()` — navigate and finalize
- `dismissError()` — clear error state

The ViewModel subscribes to the engine's playback-state flow and mirrors it into UI state.

## UI Components

- **AudioPlayerCard** — clip display (hanzi/pinyin/English), pulsing play/pause button (reduced-motion aware), and replay + slow/normal toggle buttons. While playing a button is shown and a pulsing ring animates (disabled under Reduced Motion).
- **ListeningExerciseCard** — prompt, context.
- **ListeningChoiceCard** — Button-styled answer options that reveal correct/incorrect states after an answer.
- **ReplayButton** — restart the clip with an optional `×n` replay counter.
- **ListeningProgressCard** — session progress bar, XP, streak line.
- **BaoListeningHint** — Bao's rotating encouragement with a gentle hop animation (reduced-motion aware).
- **ListeningCompletionDialog** — session summary dialog with correct count / XP / streak.

The listening screen automatically plays the first clip of each new exercise, and the slow (0.75x) toggle honors reduced motion.

## System Integrations

### Vocabulary System
- Correct answers call `vocabularyRepository.incrementHeard(wordId)` (drives `timesHeard`)
- `VocabularyDetailScreen` listening button starts a session for that word
- Dynamic exercise generation for discovered words lacking an exercise

### Quest System
- `quest_order_tea` gains objective `obj_3_4` of type `LISTEN_TO_AUDIO`
- Every correct answer advances `LISTEN_TO_AUDIO` objectives of all active quests (+1 each) via `updateObjectiveProgress`

### Game Progress System
- Every correct answer calls `gameProgressRepository.recordListeningPractice()`:
  - Increments `totalPracticedItems`-style counter `totalListeningPractices`
  - Marks the `FIRST_LISTENING` milestone on first practice

### Friendship System
- NPC-linked exercises grant `friendshipBonus` via `addFriendshipXp` on success

### Passport System
- First-ever practice records a `LISTENING_PRACTICE` entry
- Newly earned badges record `ACHIEVEMENT_UNLOCKED` entries

### Pronunciation System
- Exercises with `relatedSpeakingExerciseId` unlock the linked speaking exercise on a correct answer (listening leads to speaking)

### Dialogue System
- New `ActionType.PRACTICE_LISTENING` M (which `practice_listening`) — end-node action with comma-separated exercise ids
- `DialogueViewModel` unlocks those exercises and exposes `isListeningPracticeAvailable`
- `ConversationCompleteCard` shows a "练习听" button navigating to the listening screen
- Grandma Mei's conversation unlocks `listen_ex_greet_hello` and `listen_ex_greet_thanks`

## Testing

### Unit Tests

#### ListeningRepositoryTest
- Exercise queries (by id, type, difficulty, word, npc, unlocked, recommended)
- Session lifecycle (start, dynamic word exercises, complete, mismatch errors)
- Progress and statistics accumulation across answers
- Streak build-up, reset, and replay counting
- Rewards: XP, friendship bonus, personal bests
- Badge awarding and passport recording
- System integration: vocabulary `timesHeard`, quest `LISTEN_TO_AUDIO` objectives, game progress milestone, passport entries

#### ListeningModelsTest
- Exercise correct choice resolution, audio display text
- Session progress and attempt counts
- Result celebration and feedback logic
- Mastery/difficulty/type thresholds
- Badge catalog completeness

#### DialogueViewModelActionTest (updated)
- Verifies `PRACTICE_LISTENING` action processing still passes with the injected listening repository

#### PassportModelsTest (updated)
- Verifies the `LISTENING_PRACTICE` entry type is present in `EntryType`

## Future Enhancements

- Real audio backend (Android MediaPlayer / ExoPlayer / Media3, or packaged TTS samples) behind `AudioEngine`
- Continuous playback with gesture controls and Chinese font-size-friendly answers
- More vocab and NPC-linked listening exercises and quest-linked listening targets
- Medial persistence for listening progress
- Volume normalization and optional background music toggle (parents toggle)