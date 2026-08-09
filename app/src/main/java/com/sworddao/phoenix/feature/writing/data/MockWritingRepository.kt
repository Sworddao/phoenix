package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.seed.WritingSeedData
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus as Status
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockWritingRepository @Inject constructor(
    private val vocabularyRepository: MockVocabularyRepository,
    private val questRepository: MockQuestRepository,
    private val friendshipRepository: MockFriendshipRepository,
    private val gameProgressRepository: MockGameProgressRepository,
    private val passportRepository: MockPassportRepository,
) : WritingRepository {

    private val _exercises = MutableStateFlow(WritingSeedData.createInitialExercises())
    private val _progressByItem = MutableStateFlow<Map<String, WritingProgress>>(emptyMap())
    private val _statistics = MutableStateFlow(WritingStatistics())
    private val _badges = MutableStateFlow(WritingBadge.ALL_BADGES)
    private val _activeSession = MutableStateFlow<WritingSession?>(null)
    private val _completedSessions = MutableStateFlow<List<WritingSession>>(emptyList())
    private val _currentStreak = MutableStateFlow(0)
    private val _longestStreak = MutableStateFlow(0)
    private val _lastWritingDate = MutableStateFlow<Long?>(null)
    private val _writtenCharacters = mutableSetOf<String>()
    private val _correctCount = MutableStateFlow(0)
    private val _recordedBadgeIds = mutableSetOf<String>()
    private val _firstWritingRecorded = MutableStateFlow(false)

    // ---------------------------------------------------------------------
    // Exercise queries
    // ---------------------------------------------------------------------

    override fun getAllExercises(): Flow<List<WritingExercise>> = _exercises

    override fun getExerciseById(exerciseId: String): Flow<WritingExercise?> =
        _exercises.map { exercises -> exercises.find { it.id == exerciseId } }

    override fun getExercisesByType(type: WritingExerciseType): Flow<List<WritingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.type == type } }

    override fun getExercisesByDifficulty(difficulty: WritingDifficulty): Flow<List<WritingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.difficulty == difficulty } }

    override fun getExercisesByWord(wordId: String): Flow<List<WritingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.character.wordId == wordId }
        }

    override fun getUnlockedExercises(): Flow<List<WritingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.isUnlocked } }

    override fun getRecommendedExercises(limit: Int): Flow<List<WritingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.isUnlocked }.sortedBy { it.order }.take(limit)
        }

    // ---------------------------------------------------------------------
    // Progress and statistics
    // ---------------------------------------------------------------------

    override fun getWritingProgress(itemId: String): Flow<WritingProgress?> =
        _progressByItem.map { progress -> progress[itemId] }

    override fun getAllWritingProgress(): Flow<List<WritingProgress>> =
        _progressByItem.map { it.values.toList() }

    override fun getWritingStatistics(): Flow<WritingStatistics> = _statistics

    override fun getWritingBadges(): Flow<List<WritingBadge>> = _badges

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    override suspend fun startSession(config: WritingSessionConfig): WritingSession {
        val selected = selectExercises(config)
        val session = WritingSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        _activeSession.value = session
        return session
    }

    override suspend fun submitAnswer(attempt: WritingAttempt): WritingResultStatus {
        val session = _activeSession.value
            ?: return Status.Error("No active writing session")

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

        val friendshipBonusEarned = if (correct && exercise.character.wordId?.startsWith("greet_") == true) {
            exercise.friendshipBonus
        } else {
            0
        }

        val existingBestTime = progressFor(attempt, exercise).bestTimeMs
        val isFirstCharacterWritten = correct && !_firstWritingRecorded.value

        _activeSession.update { active ->
            active?.copy(
                attempts = active.attempts + attempt,
                totalXpEarned = active.totalXpEarned + xpEarned,
                totalFriendshipBonus = active.totalFriendshipBonus + friendshipBonusEarned,
                totalCorrectStrokes = active.totalCorrectStrokes + attempt.correctStrokeCount,
            )
        }

        updateWritingProgress(attempt, exercise)
        updateStatistics(attempt, exercise, correct)

        if (correct) {
            attempt.wordId?.let { vocabularyRepository.incrementWritten(it) }
            gameProgressRepository.recordWritingPractice()
            if (friendshipBonusEarned > 0) {
                friendshipRepository.addFriendshipXp("grandma_mei", friendshipBonusEarned)
            }
            updateWritingQuests()
            recordFirstWritingPassportEntry(exercise, attempt)
        }

        recomputeBadges()
        updateBadgePassportEntries()

        val newMastery = progressFor(attempt, exercise).masteryLevel
        val result = WritingResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = attempt.wasCorrect &&
                (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
            streakContinued = streakContinued,
            currentStreak = currentStreakValue,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            reward = WritingReward(
                xpEarned = xpEarned,
                friendshipBonusEarned = friendshipBonusEarned,
                streakContinued = streakContinued,
                isFirstCharacterWritten = correct && isNewBestCharacterWritten(),
                newMastery = if (correct) newMastery else null,
                isNewPersonalBest = attempt.wasCorrect &&
                    (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
                badgeProgress = _badges.value.associate { it.id to it.progress },
            ),
            badgeProgress = _badges.value.associate { it.id to it.progress },
        )

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: WritingSession): WritingResultStatus {
        val active = _activeSession.value
            ?: return Status.Error("No active writing session")
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

    override suspend fun updateProgress(progress: WritingProgress): WritingResultStatus {
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        _progressByItem.update { it + (mastered.itemId to mastered) }
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): WritingResultStatus {
        val exercise = _exercises.value.find { it.id == exerciseId }
            ?: return Status.Error("Exercise not found: $exerciseId")

        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }

        _exercises.update { exercises ->
            exercises.map { if (it.id == exerciseId) it.copy(isUnlocked = true) else it }
        }
        return Status.Success("Writing exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): WritingResultStatus {
        _currentStreak.value = streak
        if (streak > _longestStreak.value) {
            _longestStreak.value = streak
        }
        _statistics.update { stats ->
            stats.copy(
                currentStreak = streak,
                longestStreak = _longestStreak.value,
                lastWritingDate = _lastWritingDate.value,
            )
        }
        return Status.StreakUpdated(
            currentStreak = streak,
            longestStreak = _longestStreak.value,
        )
    }

    override suspend fun awardBadge(badgeId: String): WritingResultStatus {
        val definition = WritingBadge.getBadge(badgeId)
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

    override suspend fun addExercises(exercises: List<WritingExercise>): WritingResultStatus {
        val existingIds = _exercises.value.map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        _exercises.update { current -> current + newExercises }
        return Status.Success("Added ${newExercises.size} writing exercises")
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private suspend fun selectExercises(config: WritingSessionConfig): List<WritingExercise> {
        val unlocked = _exercises.value.filter { it.isUnlocked }
        var selected: List<WritingExercise> = emptyList()

        if (config.characterIds.isNotEmpty()) {
            selected = unlocked.filter {
                it.character.id in config.characterIds || it.character.wordId in config.characterIds
            }
            if (selected.isEmpty()) {
                val dynamic = config.characterIds.mapNotNull { characterId ->
                    val word = vocabularyRepository.getWordById(characterId).first()
                    word?.let { createWordExercise(it) }
                }
                if (dynamic.isNotEmpty()) {
                    val existingIds = _exercises.value.map { it.id }.toSet()
                    _exercises.update { current -> current + dynamic.filter { it.id !in existingIds } }
                    selected = dynamic
                }
            }
        } else {
            selected = if (config.exerciseType == WritingExerciseType.TRACE_STROKES) {
                val pool = unlocked
                    .filter { it.difficulty == config.difficulty }
                    .ifEmpty { unlocked }
                buildProgressiveSelection(pool, config.exerciseCount)
            } else {
                unlocked.filter { it.type == config.exerciseType && it.difficulty == config.difficulty }
                    .ifEmpty { unlocked.filter { it.type == config.exerciseType } }
            }
        }

        return selected.take(config.exerciseCount).ifEmpty {
            unlocked.take(config.exerciseCount)
        }
    }

    private fun buildProgressiveSelection(
        unlocked: List<WritingExercise>,
        exerciseCount: Int,
    ): List<WritingExercise> {
        val byType = WritingExerciseType.entries.map { type ->
            unlocked.filter { it.type == type }.sortedBy { it.order }
        }
        return buildList {
            var depth = 0
            while (size < exerciseCount && byType.any { depth < it.size }) {
                byType.forEach { list ->
                    if (size < exerciseCount && depth < list.size) add(list[depth])
                }
                depth++
            }
        }
    }

    private fun createWordExercise(word: VocabularyWord): WritingExercise {
        val hanziText = word.hanzi ?: word.mandarin ?: ""
        return WritingExercise(
            id = "write_dynamic_${word.id}",
            type = WritingExerciseType.TRACE_STROKES,
            difficulty = WritingDifficulty.BEGINNER,
            character = HanziCharacter(
                id = "write_char_dynamic_${word.id}",
                hanzi = hanziText,
                pinyin = word.pinyin ?: "",
                english = word.english ?: "",
                wordId = word.id,
                difficulty = WritingDifficulty.BEGINNER,
                xpReward = 10,
                order = 0,
            ),
            prompt = "试着描摹“$hanziText”",
            xpReward = 10,
            isUnlocked = true,
            order = 0,
        )
    }

    private fun updateWritingProgress(attempt: WritingAttempt, exercise: WritingExercise) {
        val itemId = attempt.wordId ?: exercise.id
        val existing = _progressByItem.value[itemId]
        val correct = attempt.wasCorrect

        val bestTime = when {
            !correct -> existing?.bestTimeMs ?: 0
            existing == null || existing.bestTimeMs == 0L -> attempt.timeTakenMs
            else -> minOf(existing.bestTimeMs, attempt.timeTakenMs)
        }

        val updated = (existing ?: WritingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )).copy(
            totalAttempts = (existing?.totalAttempts ?: 0) + 1,
            correctAttempts = (existing?.correctAttempts ?: 0) + if (correct) 1 else 0,
            timesWritten = (existing?.timesWritten ?: 0) + if (correct) 1 else 0,
            totalStrokes = (existing?.totalStrokes ?: 0) + attempt.totalStrokeCount,
            correctStrokes = (existing?.correctStrokes ?: 0) + attempt.correctStrokeCount,
            bestTimeMs = bestTime,
            lastWrittenAt = attempt.timestamp,
        ).let { it.copy(masteryLevel = calculateMastery(it)) }

        _progressByItem.update { progress -> progress + (itemId to updated) }
        attempt.wordId?.let { _writtenCharacters += it }
    }

    private fun updateStatistics(attempt: WritingAttempt, exercise: WritingExercise, correct: Boolean) {
        val current = _statistics.value
        val attempts = current.totalAttempts + 1
        val timeSum = current.averageTimePerExerciseMs * (attempts - 1) + attempt.timeTakenMs
        val masteredCount = _progressByItem.value.values.count { it.isMastered }

        _statistics.value = current.copy(
            totalExercises = _writtenCharacters.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            totalStrokes = current.totalStrokes + attempt.totalStrokeCount,
            correctStrokes = current.correctStrokes + attempt.correctStrokeCount,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            charactersWritten = if (correct) current.charactersWritten + 1 else current.charactersWritten,
            charactersMastered = masteredCount,
            currentStreak = _currentStreak.value,
            longestStreak = _longestStreak.value,
            lastWritingDate = _lastWritingDate.value,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            writingBadges = _badges.value,
        )

        if (correct) {
            _correctCount.value += 1
        }
    }

    private suspend fun updateWritingQuests() {
        val quests = questRepository.getAllQuests().first()
        quests.filter { it.status == QuestStatus.ACTIVE }
            .forEach { quest ->
                quest.objectives
                    .filter { it.type == ObjectiveType.WRITE_CHARACTERS }
                    .forEach { objective ->
                        questRepository.updateObjectiveProgress(quest.id, objective.id, 1)
                    }
            }
    }

    private suspend fun recordFirstWritingPassportEntry(
        exercise: WritingExercise,
        attempt: WritingAttempt,
    ) {
        if (_statistics.value.totalAttempts > 1) return

        passportRepository.recordEntry(
            PassportEntry(
                id = UUID.randomUUID().toString(),
                regionId = "qingyuan_village",
                type = EntryType.WRITING_PRACTICE,
                title = "第一次书写练习",
                description = "你写出了“${exercise.hanzi}”！",
                metadata = mapOf(
                    "exerciseId" to exercise.id,
                    "timeTakenMs" to attempt.timeTakenMs.toString(),
                ),
            )
        )
    }

    private fun progressFor(attempt: WritingAttempt, exercise: WritingExercise): WritingProgress {
        val itemId = attempt.wordId ?: exercise.character.wordId ?: exercise.id
        return _progressByItem.value[itemId] ?: WritingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )
    }

    private fun isNewBestCharacterWritten(): Boolean {
        if (_firstWritingRecorded.value) return false
        _firstWritingRecorded.value = true
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
                    "write_first" -> if (_writtenCharacters.isNotEmpty()) 1f else 0f
                    "write_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                    "write_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                    "write_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                    "write_steady_hand" -> (longestStreak / 10f).coerceIn(0f, 1f)
                    "write_stroke_perfect" -> (_correctCount.value / 20f).coerceIn(0f, 1f)
                    "write_dialogue_ready" -> (_correctCount.value / 15f).coerceIn(0f, 1f)
                    "write_char_collector" -> (masteredCount / 10f).coerceIn(0f, 1f)
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
        val last = _lastWritingDate.value
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> {
                _currentStreak.value = 1
                _lastWritingDate.value = timestamp
                true
            }
            lastDay == today -> {
                _lastWritingDate.value = timestamp
                false
            }
            lastDay == today - DAY_MILLIS -> {
                _currentStreak.value += 1
                if (_currentStreak.value > _longestStreak.value) {
                    _longestStreak.value = _currentStreak.value
                }
                _lastWritingDate.value = timestamp
                true
            }
            else -> {
                _currentStreak.value = 1
                _lastWritingDate.value = timestamp
                true
            }
        }
    }

    private fun calculateMastery(progress: WritingProgress): WritingMastery {
        return WritingMastery.entries.lastOrNull { mastery ->
            progress.totalAttempts >= mastery.minAttempts &&
                progress.successRate >= mastery.requiredSuccessRate
        } ?: WritingMastery.NEW
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

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
