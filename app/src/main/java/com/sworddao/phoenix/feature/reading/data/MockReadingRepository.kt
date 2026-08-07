package com.sworddao.phoenix.feature.reading.data

import com.sworddao.phoenix.data.seed.ReadingSeedData

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus as Status
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockReadingRepository @Inject constructor(
    private val vocabularyRepository: MockVocabularyRepository,
    private val questRepository: MockQuestRepository,
    private val friendshipRepository: MockFriendshipRepository,
    private val gameProgressRepository: MockGameProgressRepository,
    private val passportRepository: MockPassportRepository,
    private val pronunciationRepository: MockPronunciationRepository,
    private val listeningRepository: MockListeningRepository,
    private val hanziRenderer: MockHanziRenderer,
) : ReadingRepository {

    private val _exercises = MutableStateFlow(createInitialExercises())
    private val _progressByItem = MutableStateFlow<Map<String, ReadingProgress>>(emptyMap())
    private val _statistics = MutableStateFlow(ReadingStatistics())
    private val _badges = MutableStateFlow(ReadingBadge.ALL_BADGES)
    private val _revealCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _activeSession = MutableStateFlow<ReadingSession?>(null)
    private val _completedSessions = MutableStateFlow<List<ReadingSession>>(emptyList())
    private val _currentStreak = MutableStateFlow(0)
    private val _longestStreak = MutableStateFlow(0)
    private val _lastReadingDate = MutableStateFlow<Long?>(null)
    private val _readWords = mutableSetOf<String>()
    private val _readCharacters = MutableStateFlow(0)
    private val _correctCount = MutableStateFlow(0)
    private val _npcExerciseCount = MutableStateFlow(0)
    private val _recordedBadgeIds = mutableSetOf<String>()
    private val _firstReadingRecorded = MutableStateFlow(false)

    // ---------------------------------------------------------------------
    // Exercise queries
    // ---------------------------------------------------------------------

    override fun getAllExercises(): Flow<List<ReadingExercise>> = _exercises

    override fun getExerciseById(exerciseId: String): Flow<ReadingExercise?> =
        _exercises.map { exercises -> exercises.find { it.id == exerciseId } }

    override fun getExercisesByType(type: ReadingExerciseType): Flow<List<ReadingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.type == type } }

    override fun getExercisesByDifficulty(difficulty: ReadingDifficulty): Flow<List<ReadingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.difficulty == difficulty } }

    override fun getExercisesByWord(wordId: String): Flow<List<ReadingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.relatedWordId == wordId }
        }

    override fun getExercisesByNpc(npcId: String): Flow<List<ReadingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.relatedNpcId == npcId }
        }

    override fun getExercisesByQuest(questId: String): Flow<List<ReadingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.relatedQuestId == questId } }

    override fun getUnlockedExercises(): Flow<List<ReadingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.isUnlocked } }

    override fun getRecommendedExercises(limit: Int): Flow<List<ReadingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.isUnlocked }.sortedBy { it.order }.take(limit)
        }

    // ---------------------------------------------------------------------
    // Progress and statistics
    // ---------------------------------------------------------------------

    override fun getReadingProgress(itemId: String): Flow<ReadingProgress?> =
        _progressByItem.map { progress -> progress[itemId] }

    override fun getAllReadingProgress(): Flow<List<ReadingProgress>> =
        _progressByItem.map { it.values.toList() }

    override fun getReadingStatistics(): Flow<ReadingStatistics> = _statistics

    override fun getReadingBadges(): Flow<List<ReadingBadge>> = _badges

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    override suspend fun startSession(config: ReadingSessionConfig): ReadingSession {
        val selected = selectExercises(config)
        val session = ReadingSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        _activeSession.value = session
        return session
    }

    override suspend fun submitAnswer(attempt: ReadingAttempt): ReadingResultStatus {
        val session = _activeSession.value
            ?: return Status.Error("No active reading session")

        val exercise = _exercises.value.find { it.id == attempt.exerciseId }
            ?: return Status.Error("Exercise not found: ${attempt.exerciseId}")

        val correct = attempt.wasCorrect
        val streakIncremented = updateStreak(correct, attempt.timestamp)
        val currentStreakValue = _currentStreak.value
        val streakContinued = correct && currentStreakValue >= 1 && streakIncremented

        val xpEarned = if (correct) {
            val streakBonus = if (currentStreakValue >= 2) 5 else 0
            exercise.xpReward + streakBonus
        } else {
            0
        }

        val friendshipBonusEarned = if (correct && exercise.relatedNpcId != null) {
            exercise.friendshipBonus
        } else {
            0
        }

        val existingBestTime = progressFor(attempt, exercise).bestTimeMs
        val isFirstWordRead = correct && !_firstReadingRecorded.value

        _activeSession.update { active ->
            active?.copy(
                attempts = active.attempts + attempt,
                totalXpEarned = active.totalXpEarned + xpEarned,
                totalFriendshipBonus = active.totalFriendshipBonus + friendshipBonusEarned,
                totalReveals = active.totalReveals + if (attempt.revealedHanziBeforeAnswer) 1 else 0,
            )
        }

        updateReadingProgress(attempt, exercise)
        updateStatistics(attempt, exercise, correct)

        if (correct) {
            attempt.wordId?.let { vocabularyRepository.incrementRead(it) }
            gameProgressRepository.recordReadingPractice()
            exercise.relatedSpeakingExerciseId?.let { speakingId ->
                val speakingResult = pronunciationRepository.unlockExercise(speakingId)
                if (speakingResult is PronunciationResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            exercise.relatedListeningExerciseId?.let { listeningId ->
                val listeningResult = listeningRepository.unlockExercise(listeningId)
                if (listeningResult is ListeningResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateReadingQuests()
            recordFirstReadingPassportEntry(exercise, attempt)
        }

        recomputeBadges()
        updateBadgePassportEntries()

        val newMastery = progressFor(attempt, exercise).masteryLevel
        val result = ReadingResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = attempt.wasCorrect &&
                (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
            streakContinued = streakContinued,
            currentStreak = currentStreakValue,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            reward = ReadingReward(
                xpEarned = xpEarned,
                friendshipBonusEarned = friendshipBonusEarned,
                streakContinued = streakContinued,
                isFirstWordRead = correct && isNewBestWordRead(),
                newMastery = if (correct) newMastery else null,
                isNewPersonalBest = attempt.wasCorrect &&
                    (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
                badgeProgress = _badges.value.associate { it.id to it.progress },
            ),
            badgeProgress = _badges.value.associate { it.id to it.progress },
        )

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: ReadingSession): ReadingResultStatus {
        val active = _activeSession.value
            ?: return Status.Error("No active reading session")
        if (active.id != session.id) {
            return Status.Error("Session mismatch")
        }

        val completed = active.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )

        _completedSessions.update { sessions -> sessions + completed }
        _activeSession.value = null
        _statistics.update { it.copy(totalSessions = it.totalSessions + 1) }

        return Status.SessionCompleted(completed, _statistics.value)
    }

    override suspend fun updateProgress(progress: ReadingProgress): ReadingResultStatus {
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        _progressByItem.update { it + (mastered.itemId to mastered) }
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): ReadingResultStatus {
        val exercise = _exercises.value.find { it.id == exerciseId }
            ?: return Status.Error("Exercise not found: $exerciseId")

        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }

        _exercises.update { exercises ->
            exercises.map { if (it.id == exerciseId) it.copy(isUnlocked = true) else it }
        }
        return Status.Success("Reading exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): ReadingResultStatus {
        _currentStreak.value = streak
        if (streak > _longestStreak.value) {
            _longestStreak.value = streak
        }
        _statistics.update { stats ->
            stats.copy(
                currentStreak = streak,
                longestStreak = _longestStreak.value,
                lastReadingDate = _lastReadingDate.value,
            )
        }
        return Status.StreakUpdated(
            currentStreak = streak,
            longestStreak = _longestStreak.value,
        )
    }

    override suspend fun awardBadge(badgeId: String): ReadingResultStatus {
        val definition = ReadingBadge.getBadge(badgeId)
            ?: return Status.Error("Badge not found: $badgeId")

        if (_badges.value.any { it.id == badgeId && it.isEarned }) {
            return Status.Error("Badge already earned: $badgeId")
        }

        _badges.update { badges ->
            badges.map { badge ->
                if (badge.id == badgeId) {
                    badge.copy(progress = 1f, isEarned = true, earnedAt = System.currentTimeMillis())
                } else {
                    badge
                }
            }
        }

        val badge = _badges.value.find { it.id == badgeId }
        return if (badge?.isEarned == true) {
            Status.BadgeEarned(badge)
        } else {
            Status.Error("Badge already earned: $badgeId")
        }
    }

    override suspend fun addExercises(exercises: List<ReadingExercise>): ReadingResultStatus {
        val existingIds = _exercises.value.map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        _exercises.update { current -> current + newExercises }
        return Status.Success("Added ${newExercises.size} reading exercises")
    }

    override suspend fun recordReveal(wordId: String): ReadingResultStatus {
        val newCount = (_revealCounts.value[wordId] ?: 0) + 1
        _revealCounts.update { it + (wordId to newCount) }

        _progressByItem.update { progress ->
            val existing = progress[wordId] ?: ReadingProgress(itemId = wordId, wordId = wordId)
            val updated = existing.copy(
                timesRevealed = existing.timesRevealed + 1,
                hasRevealedHanzi = true,
            )
            progress + (wordId to updated)
        }

        _statistics.update { it.copy(totalReveals = it.totalReveals + 1) }
        return Status.RevealRecorded(wordId, newCount)
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private suspend fun selectExercises(config: ReadingSessionConfig): List<ReadingExercise> {
        val unlocked = _exercises.value.filter { it.isUnlocked }
        var selected: List<ReadingExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.relatedWordId in config.wordIds }
            if (selected.isEmpty()) {
                val dynamic = config.wordIds.mapNotNull { wordId ->
                    val word = vocabularyRepository.getWordById(wordId).first()
                    word?.let { createWordExercise(it) }
                }
                if (dynamic.isNotEmpty()) {
                    val existingIds = _exercises.value.map { it.id }.toSet()
                    _exercises.update { current -> current + dynamic.filter { it.id !in existingIds } }
                    selected = dynamic
                }
            }
        } else if (config.npcId != null) {
            selected = unlocked.filter { it.relatedNpcId == config.npcId }
        } else if (config.questId != null) {
            selected = unlocked.filter { it.relatedQuestId == config.questId }
        } else {
            selected = unlocked.filter {
                it.type == config.exerciseType && it.difficulty == config.difficulty
            }
            if (selected.isEmpty()) {
                selected = unlocked.filter { it.type == config.exerciseType }
            }
        }

        return selected.take(config.exerciseCount).ifEmpty {
            unlocked.take(config.exerciseCount)
        }
    }

    private fun createWordExercise(word: VocabularyWord): ReadingExercise {
        val distractorHanzi = listOf("再见", "谢谢", "吃饭", "喝水", "一", "你好吗")
            .filter { it != word.hanzi }
            .take(3)
        val hanziText = word.hanzi ?: word.mandarin ?: ""
        val choices = listOf(
            ReadingChoice("choice_0", hanziText, word.pinyin ?: "", hanziText)
        ) +
            distractorHanzi.mapIndexed { index, text ->
                ReadingChoice(id = "choice_${index + 1}", text = text, pinyin = "", hanzi = text)
            }
        return ReadingExercise(
            id = "read_dynamic_${word.id}",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = hanziText,
            pinyin = word.pinyin ?: "",
            english = word.english ?: "",
            syllableTones = hanziRenderer.tonesOf(word.pinyin ?: ""),
            prompt = "哪个汉字是你看到的拼音？",
            choices = choices,
            correctChoiceIndex = 0,
            context = word.exampleSentence,
            relatedNpcId = word.relatedNpcId,
            relatedWordId = word.id,
            xpReward = 10,
            isUnlocked = true,
        )
    }

    private fun updateReadingProgress(attempt: ReadingAttempt, exercise: ReadingExercise) {
        val itemId = attempt.wordId ?: exercise.id
        val existing = _progressByItem.value[itemId]
        val correct = attempt.wasCorrect

        val bestTime = when {
            !correct -> existing?.bestTimeMs ?: 0
            existing == null || existing.bestTimeMs == 0L -> attempt.timeTakenMs
            else -> minOf(existing.bestTimeMs, attempt.timeTakenMs)
        }

        val updated = (existing ?: ReadingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )).copy(
            totalAttempts = (existing?.totalAttempts ?: 0) + 1,
            correctAttempts = (existing?.correctAttempts ?: 0) + if (correct) 1 else 0,
            timesRead = (existing?.timesRead ?: 0) + if (correct) 1 else 0,
            bestTimeMs = bestTime,
            lastReadAt = attempt.timestamp,
        ).let { it.copy(masteryLevel = calculateMastery(it)) }

        _progressByItem.update { progress -> progress + (itemId to updated) }
        attempt.wordId?.let { _readWords += it }
    }

    private fun updateStatistics(attempt: ReadingAttempt, exercise: ReadingExercise, correct: Boolean) {
        val current = _statistics.value
        val attempts = current.totalAttempts + 1
        val timeSum = current.averageTimePerExerciseMs * (attempts - 1) + attempt.timeTakenMs
        val masteredCount = _progressByItem.value.values.count { it.isMastered }

        _statistics.value = current.copy(
            totalExercises = _readWords.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            charactersRead = if (correct) current.charactersRead + exercise.hanzi.length else current.charactersRead,
            wordsRead = _readWords.size,
            wordsMastered = masteredCount,
            currentStreak = _currentStreak.value,
            longestStreak = _longestStreak.value,
            lastReadingDate = _lastReadingDate.value,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            readingBadges = _badges.value,
        )

        if (correct) {
            _correctCount.value += 1
            if (exercise.relatedNpcId != null) {
                _npcExerciseCount.value += 1
            }
        }
    }

    private suspend fun updateReadingQuests() {
        val quests = questRepository.getAllQuests().first()
        quests.filter { it.status == QuestStatus.ACTIVE }
            .forEach { quest ->
                quest.objectives
                    .filter { it.type == ObjectiveType.READ_CHARACTERS }
                    .forEach { objective ->
                        questRepository.updateObjectiveProgress(quest.id, objective.id, 1)
                    }
            }
    }

    private suspend fun recordFirstReadingPassportEntry(
        exercise: ReadingExercise,
        attempt: ReadingAttempt,
    ) {
        if (_statistics.value.totalAttempts > 1) return

        passportRepository.recordEntry(
            PassportEntry(
                id = UUID.randomUUID().toString(),
                regionId = "qingyuan_village",
                type = EntryType.READING_PRACTICE,
                title = "第一次阅读练习",
                description = "你读懂了“${exercise.hanzi}”！",
                metadata = mapOf(
                    "exerciseId" to exercise.id,
                    "timeTakenMs" to attempt.timeTakenMs.toString(),
                ),
            )
        )
    }

    private fun progressFor(attempt: ReadingAttempt, exercise: ReadingExercise): ReadingProgress {
        val itemId = attempt.wordId ?: exercise.relatedWordId ?: exercise.id
        return _progressByItem.value[itemId] ?: ReadingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )
    }

    private fun isNewBestWordRead(): Boolean {
        if (_firstReadingRecorded.value) return false
        _firstReadingRecorded.value = true
        return true
    }

    private fun recomputeBadges() {
        val streak = _currentStreak.value
        val longestStreak = _longestStreak.value
        val masteredCount = _progressByItem.value.values.count { it.isMastered }
        val now = System.currentTimeMillis()

        _badges.update { badges ->
            badges.map { badge ->
                val progress = when (badge.id) {
                    "read_first" -> if (_readWords.isNotEmpty()) 1f else 0f
                    "read_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                    "read_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                    "read_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                    "read_quick_eye" -> (longestStreak / 10f).coerceIn(0f, 1f)
                    "read_accurate" -> (_correctCount.value / 20f).coerceIn(0f, 1f)
                    "read_dialogue_ready" -> (_npcExerciseCount.value / 15f).coerceIn(0f, 1f)
                    "read_char_collector" -> (masteredCount / 10f).coerceIn(0f, 1f)
                    else -> badge.progress
                }

                if (!badge.isEarned && progress >= 1f) {
                    badge.copy(progress = 1f, isEarned = true, earnedAt = now)
                } else {
                    badge.copy(progress = progress)
                }
            }
        }
    }

    private suspend fun updateBadgePassportEntries() {
        val newlyEarned = _badges.value.filter { it.isEarned && it.id !in _recordedBadgeIds }
        newlyEarned.forEach { badge ->
            _recordedBadgeIds += badge.id
            passportRepository.recordEntry(
                PassportEntry(
                    id = UUID.randomUUID().toString(),
                    regionId = "qingyuan_village",
                    type = EntryType.ACHIEVEMENT_UNLOCKED,
                    title = "获得徽章：${badge.name}",
                    description = badge.description,
                )
            )
        }
    }

    private fun updateStreak(wasCorrect: Boolean, timestamp: Long): Boolean {
        if (!wasCorrect) return false

        val today = getStartOfDay(timestamp)
        val last = _lastReadingDate.value
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> {
                _currentStreak.value = 1
                _lastReadingDate.value = timestamp
                true
            }
            lastDay == today -> {
                _lastReadingDate.value = timestamp
                false
            }
            lastDay == today - DAY_MILLIS -> {
                _currentStreak.value += 1
                if (_currentStreak.value > _longestStreak.value) {
                    _longestStreak.value = _currentStreak.value
                }
                _lastReadingDate.value = timestamp
                true
            }
            else -> {
                _currentStreak.value = 1
                _lastReadingDate.value = timestamp
                true
            }
        }
    }

    private fun calculateMastery(progress: ReadingProgress): ReadingMastery {
        return ReadingMastery.entries.lastOrNull { mastery ->
            progress.totalAttempts >= mastery.minAttempts &&
                progress.successRate >= mastery.requiredSuccessRate
        } ?: ReadingMastery.NEW
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    
    private fun createInitialExercises(): List<ReadingExercise> =
        ReadingSeedData.createInitialExercises()

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}