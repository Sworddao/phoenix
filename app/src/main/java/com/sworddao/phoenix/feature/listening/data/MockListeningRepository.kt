package com.sworddao.phoenix.feature.listening.data

import com.sworddao.phoenix.data.seed.ListeningSeedData

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus as Status
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
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
class MockListeningRepository @Inject constructor(
    private val vocabularyRepository: MockVocabularyRepository,
    private val questRepository: MockQuestRepository,
    private val friendshipRepository: MockFriendshipRepository,
    private val gameProgressRepository: MockGameProgressRepository,
    private val passportRepository: MockPassportRepository,
    private val pronunciationRepository: MockPronunciationRepository,
) : ListeningRepository {

    private val _exercises = MutableStateFlow(createInitialExercises())
    private val _progressByItem = MutableStateFlow<Map<String, ListeningProgress>>(emptyMap())
    private val _statistics = MutableStateFlow(ListeningStatistics())
    private val _badges = MutableStateFlow(ListeningBadge.ALL_BADGES)
    private val _replayCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _activeSession = MutableStateFlow<ListeningSession?>(null)
    private val _completedSessions = MutableStateFlow<List<ListeningSession>>(emptyList())
    private val _currentStreak = MutableStateFlow(0)
    private val _longestStreak = MutableStateFlow(0)
    private val _lastListeningDate = MutableStateFlow<Long?>(null)
    private val _practicedWords = mutableSetOf<String>()
    private val _correctCount = MutableStateFlow(0)
    private val _npcExerciseCount = MutableStateFlow(0)
    private val _recordedBadgeIds = mutableSetOf<String>()

    // ---------------------------------------------------------------------
    // Exercise queries
    // ---------------------------------------------------------------------

    override fun getAllExercises(): Flow<List<ListeningExercise>> = _exercises

    override fun getExerciseById(exerciseId: String): Flow<ListeningExercise?> =
        _exercises.map { exercises -> exercises.find { it.id == exerciseId } }

    override fun getExercisesByType(type: ListeningExerciseType): Flow<List<ListeningExercise>> =
        _exercises.map { exercises -> exercises.filter { it.type == type } }

    override fun getExercisesByDifficulty(difficulty: ListeningDifficulty): Flow<List<ListeningExercise>> =
        _exercises.map { exercises -> exercises.filter { it.difficulty == difficulty } }

    override fun getExercisesByWord(wordId: String): Flow<List<ListeningExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.relatedWordId == wordId || it.clip.wordId == wordId }
        }

    override fun getExercisesByNpc(npcId: String): Flow<List<ListeningExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.relatedNpcId == npcId || it.clip.npcId == npcId }
        }

    override fun getExercisesByQuest(questId: String): Flow<List<ListeningExercise>> =
        _exercises.map { exercises -> exercises.filter { it.relatedQuestId == questId } }

    override fun getUnlockedExercises(): Flow<List<ListeningExercise>> =
        _exercises.map { exercises -> exercises.filter { it.isUnlocked } }

    override fun getRecommendedExercises(limit: Int): Flow<List<ListeningExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.isUnlocked }.sortedBy { it.order }.take(limit)
        }

    // ---------------------------------------------------------------------
    // Progress and statistics
    // ---------------------------------------------------------------------

    override fun getListeningProgress(itemId: String): Flow<ListeningProgress?> =
        _progressByItem.map { progress -> progress[itemId] }

    override fun getAllListeningProgress(): Flow<List<ListeningProgress>> =
        _progressByItem.map { it.values.toList() }

    override fun getListeningStatistics(): Flow<ListeningStatistics> = _statistics

    override fun getListeningBadges(): Flow<List<ListeningBadge>> = _badges

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    override suspend fun startSession(config: ListeningSessionConfig): ListeningSession {
        val selected = selectExercises(config)
        val session = ListeningSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        _activeSession.value = session
        return session
    }

    override suspend fun submitAnswer(attempt: ListeningAttempt): ListeningResultStatus {
        val session = _activeSession.value
            ?: return Status.Error("No active listening session")

        val exercise = _exercises.value.find { it.id == attempt.exerciseId }
            ?: return Status.Error("Exercise not found: ${attempt.exerciseId}")

        val correct = attempt.wasCorrect
        val streakIncremented = updateStreak(correct, attempt.timestamp)
        val currentStreak = _currentStreak.value
        val streakContinued = correct && currentStreak >= 1 && streakIncremented

        val xpEarned = if (correct) {
            val streakBonus = if (currentStreak >= 2) 5 else 0
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

        _activeSession.update { active ->
            active?.copy(
                attempts = active.attempts + attempt,
                totalXpEarned = active.totalXpEarned + xpEarned,
                totalFriendshipBonus = active.totalFriendshipBonus + friendshipBonusEarned,
            )
        }

        updateListeningProgress(attempt, exercise)
        updateStatistics(attempt, exercise, correct)

        if (correct) {
            attempt.wordId?.let { vocabularyRepository.incrementHeard(it) }
            gameProgressRepository.recordListeningPractice()
            exercise.relatedSpeakingExerciseId?.let { speakingId ->
                val speakingResult = pronunciationRepository.unlockExercise(speakingId)
                if (speakingResult is PronunciationResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateListeningQuests()
            recordFirstListeningPassportEntry(exercise, attempt)
        }

        recomputeBadges()
        recordNewBadgeEntries()

        val result = ListeningResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = attempt.wasCorrect &&
                (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
            streakContinued = streakContinued,
            currentStreak = currentStreak,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            badgeProgress = _badges.value.associate { it.id to it.progress },
        )

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: ListeningSession): ListeningResultStatus {
        val active = _activeSession.value
            ?: return Status.Error("No active listening session")
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

    override suspend fun updateProgress(progress: ListeningProgress): ListeningResultStatus {
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        _progressByItem.update { it + (mastered.itemId to mastered) }
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): ListeningResultStatus {
        val exercise = _exercises.value.find { it.id == exerciseId }
            ?: return Status.Error("Exercise not found: $exerciseId")

        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }

        _exercises.update { exercises ->
            exercises.map { if (it.id == exerciseId) it.copy(isUnlocked = true) else it }
        }
        return Status.Success("Exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): ListeningResultStatus {
        _currentStreak.value = streak
        if (streak > _longestStreak.value) {
            _longestStreak.value = streak
        }
        _statistics.update { stats ->
            stats.copy(
                currentStreak = streak,
                longestStreak = _longestStreak.value,
                lastListeningDate = _lastListeningDate.value,
            )
        }
        return Status.StreakUpdated(
            currentStreak = streak,
            longestStreak = _longestStreak.value,
        )
    }

    override suspend fun awardBadge(badgeId: String): ListeningResultStatus {
        val definition = ListeningBadge.getBadge(badgeId)
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

    override suspend fun addExercises(exercises: List<ListeningExercise>): ListeningResultStatus {
        val existingIds = _exercises.value.map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        _exercises.update { current -> current + newExercises }
        return Status.Success("Added ${newExercises.size} exercises")
    }

    override suspend fun recordReplay(exerciseId: String): ListeningResultStatus {
        if (_exercises.value.none { it.id == exerciseId }) {
            return Status.Error("Exercise not found: $exerciseId")
        }
        val newCount = (_replayCounts.value[exerciseId] ?: 0) + 1
        _replayCounts.update { it + (exerciseId to newCount) }
        _statistics.update { it.copy(totalReplayCount = it.totalReplayCount + 1) }
        return Status.ReplayRecorded(exerciseId, newCount)
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private suspend fun selectExercises(config: ListeningSessionConfig): List<ListeningExercise> {
        val unlocked = _exercises.value.filter { it.isUnlocked }
        var selected: List<ListeningExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.relatedWordId in config.wordIds || it.clip.wordId in config.wordIds }
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
            selected = unlocked.filter {
                it.relatedNpcId == config.npcId || it.clip.npcId == config.npcId
            }
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

    private fun createWordExercise(word: VocabularyWord): ListeningExercise {
        val distractors = listOf("zài jiàn", "xiè xie", "chī fàn", "hē", "yī", "nǐ hǎo ma")
            .filter { it != word.pinyin }
            .take(3)
        val choices = listOf(ListeningChoice("choice_0", word.pinyin)) +
            distractors.mapIndexed { index, text ->
                ListeningChoice(id = "choice_${index + 1}", text = text)
            }
        return ListeningExercise(
            id = "listen_dynamic_${word.id}",
            type = ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY,
            difficulty = ListeningDifficulty.BEGINNER,
            clip = AudioClip(
                id = "clip_${word.id}",
                text = word.pinyin,
                hanzi = word.hanzi,
                english = word.english,
                wordId = word.id,
            ),
            prompt = "听一听，选出你听到的词汇",
            choices = choices,
            correctChoiceIndex = 0,
            context = word.exampleSentence,
            relatedNpcId = word.relatedNpcId,
            relatedWordId = word.id,
            relatedSpeakingExerciseId = null,
            xpReward = 10,
            isUnlocked = true,
        )
    }

    private fun updateListeningProgress(attempt: ListeningAttempt, exercise: ListeningExercise) {
        val itemId = attempt.wordId ?: exercise.clip.id
        val existing = _progressByItem.value[itemId]
        val correct = attempt.wasCorrect

        val bestTime = when {
            !correct -> existing?.bestTimeMs ?: 0
            existing == null || existing.bestTimeMs == 0L -> attempt.timeTakenMs
            else -> minOf(existing.bestTimeMs, attempt.timeTakenMs)
        }

        val updated = (existing ?: ListeningProgress(itemId = itemId, wordId = attempt.wordId)).copy(
            totalAttempts = (existing?.totalAttempts ?: 0) + 1,
            correctAttempts = (existing?.correctAttempts ?: 0) + if (correct) 1 else 0,
            replayCount = (existing?.replayCount ?: 0) + attempt.replayCount,
            bestTimeMs = bestTime,
            lastListenedAt = attempt.timestamp,
        ).let { it.copy(masteryLevel = calculateMastery(it)) }

        _progressByItem.update { progress -> progress + (itemId to updated) }
        attempt.wordId?.let { _practicedWords += it }
    }

    private fun updateStatistics(attempt: ListeningAttempt, exercise: ListeningExercise, correct: Boolean) {
        val current = _statistics.value
        val attempts = current.totalAttempts + 1
        val timeSum = current.totalTimeListenedMs + attempt.timeTakenMs
        val masteredCount = _progressByItem.value.values.count { it.isMastered }

        _statistics.value = current.copy(
            totalExercises = _practicedWords.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            totalTimeListenedMs = timeSum,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            wordsPracticed = _practicedWords.size,
            wordsMastered = masteredCount,
            currentStreak = _currentStreak.value,
            longestStreak = _longestStreak.value,
            lastListeningDate = _lastListeningDate.value,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            listeningBadges = _badges.value,
        )

        if (correct) {
            _correctCount.value += 1
            if (exercise.relatedNpcId != null) {
                _npcExerciseCount.value += 1
            }
        }
    }

    private suspend fun updateListeningQuests() {
        val quests = questRepository.getAllQuests().first()
        quests.filter { it.status == QuestStatus.ACTIVE }
            .forEach { quest ->
                quest.objectives
                    .filter { it.type == ObjectiveType.LISTEN_TO_AUDIO }
                    .forEach { objective ->
                        questRepository.updateObjectiveProgress(quest.id, objective.id, 1)
                    }
            }
    }

    private suspend fun recordFirstListeningPassportEntry(
        exercise: ListeningExercise,
        attempt: ListeningAttempt,
    ) {
        if (_statistics.value.totalAttempts > 1) return

        passportRepository.recordEntry(
            PassportEntry(
                id = UUID.randomUUID().toString(),
                regionId = "qingyuan_village",
                type = EntryType.LISTENING_PRACTICE,
                title = "第一次聆听练习",
                description = "你听懂了“${exercise.clip.text}”！",
                metadata = mapOf(
                    "exerciseId" to exercise.id,
                    "timeTakenMs" to attempt.timeTakenMs.toString(),
                ),
            )
        )
    }

    private fun progressFor(attempt: ListeningAttempt, exercise: ListeningExercise): ListeningProgress {
        val itemId = attempt.wordId ?: exercise.clip.id
        return _progressByItem.value[itemId] ?: ListeningProgress(itemId = itemId)
    }

    private fun recomputeBadges() {
        val streak = _currentStreak.value
        val longestStreak = _longestStreak.value
        val masteredCount = _progressByItem.value.values.count { it.isMastered }
        val now = System.currentTimeMillis()

        _badges.update { badges ->
            badges.map { badge ->
                val progress = when (badge.id) {
                    "listen_first" -> if (_practicedWords.isNotEmpty()) 1f else 0f
                    "listen_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                    "listen_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                    "listen_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                    "listen_quick_ear" -> (longestStreak / 10f).coerceIn(0f, 1f)
                    "listen_accurate" -> (_correctCount.value / 20f).coerceIn(0f, 1f)
                    "listen_npc_ready" -> (_npcExerciseCount.value / 15f).coerceIn(0f, 1f)
                    "listen_word_collector" -> (masteredCount / 20f).coerceIn(0f, 1f)
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

    private suspend fun recordNewBadgeEntries() {
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
        val last = _lastListeningDate.value
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> {
                _currentStreak.value = 1
                _lastListeningDate.value = timestamp
                true
            }
            lastDay == today -> {
                _lastListeningDate.value = timestamp
                false
            }
            lastDay == today - DAY_MILLIS -> {
                _currentStreak.value += 1
                if (_currentStreak.value > _longestStreak.value) {
                    _longestStreak.value = _currentStreak.value
                }
                _lastListeningDate.value = timestamp
                true
            }
            else -> {
                _currentStreak.value = 1
                _lastListeningDate.value = timestamp
                true
            }
        }
    }

    private fun calculateMastery(progress: ListeningProgress): ListeningMastery {
        return ListeningMastery.entries.lastOrNull { mastery ->
            progress.totalAttempts >= mastery.minAttempts &&
                progress.successRate >= mastery.requiredSuccessRate
        } ?: ListeningMastery.NEW
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

    
    private fun createInitialExercises(): List<ListeningExercise> =
        ListeningSeedData.createInitialExercises()

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
