package com.sworddao.phoenix.feature.reading.data

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

    private fun createInitialExercises(): List<ReadingExercise> = listOf(
        ReadingExercise(
            id = "read_ex_greet_hello",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好",
            pinyin = "nǐ hǎo",
            english = "Hello",
            syllableTones = hanziRenderer.tonesOf("nǐ hǎo"),
            prompt = "哪个汉字对应拼音 nǐ hǎo？",
            choices = listOf(
                ReadingChoice("choice_0", "你好", "nǐ hǎo", "你好"),
                ReadingChoice("choice_1", "谢谢", "xiè xie", "谢谢"),
                ReadingChoice("choice_2", "再见", "zài jiàn", "再见"),
                ReadingChoice("choice_3", "吃饭", "chī fàn", "吃饭"),
            ),
            correctChoiceIndex = 0,
            context = "Grandma Mei greets you at her door",
            relatedNpcId = "grandma_mei",
            relatedWordId = "greet_001",
            relatedListeningExerciseId = "listen_ex_greet_hello",
            xpReward = 10,
            friendshipBonus = 2,
            isUnlocked = true,
            order = 1,
        ),
        ReadingExercise(
            id = "read_ex_greet_thanks",
            type = ReadingExerciseType.MATCH_HANZI_TO_MEANING,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "谢谢",
            pinyin = "xiè xie",
            english = "Thank you",
            syllableTones = listOf(4, 0),
            prompt = "“谢谢”是什么意思？",
            choices = listOf(
                ReadingChoice("choice_0", "Thank you", "xiè xie", "谢谢"),
                ReadingChoice("choice_1", "Goodbye", "zài jiàn", "再见"),
                ReadingChoice("choice_2", "Hello", "nǐ hǎo", "你好"),
                ReadingChoice("choice_3", "Delicious", "hǎo chī", "好吃"),
            ),
            correctChoiceIndex = 0,
            context = "Grandma Mei hands you warm bread",
            relatedNpcId = "grandma_mei",
            relatedWordId = "greet_003",
            relatedListeningExerciseId = "listen_ex_greet_thanks",
            xpReward = 10,
            friendshipBonus = 2,
            isUnlocked = true,
            order = 2,
        ),
        ReadingExercise(
            id = "read_ex_greet_goodbye",
            type = ReadingExerciseType.CHARACTER_RECOGNITION,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "再见",
            pinyin = "zài jiàn",
            english = "Goodbye",
            syllableTones = hanziRenderer.tonesOf("zài jiàn"),
            prompt = "找出“再见”两个字。",
            choices = listOf(
                ReadingChoice("choice_0", "再见", "zài jiàn", "再见"),
                ReadingChoice("choice_1", "你好", "nǐ hǎo", "你好"),
                ReadingChoice("choice_2", "谢谢", "xiè xie", "谢谢"),
                ReadingChoice("choice_3", "欢迎", "huān yíng", "欢迎"),
            ),
            correctChoiceIndex = 0,
            context = "End of a friendly chat in the village",
            relatedWordId = "greet_002",
            xpReward = 10,
            isUnlocked = true,
            order = 3,
        ),
        ReadingExercise(
            id = "read_ex_mei_greeting",
            type = ReadingExerciseType.SENTENCE_READING,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好吗？",
            pinyin = "nǐ hǎo ma?",
            english = "How are you?",
            syllableTones = hanziRenderer.tonesOf("nǐ hǎo ma"),
            prompt = "读一读，梅奶奶在说什么？",
            choices = listOf(
                ReadingChoice("choice_0", "你好吗？ How are you?", "nǐ hǎo ma?", "你好吗？"),
                ReadingChoice("choice_1", "再见 Goodbye", "zài jiàn", "再见"),
                ReadingChoice("choice_2", "谢谢 Thank you", "xiè xie", "谢谢"),
                ReadingChoice("choice_3", "吃饭 Eating", "chī fàn", "吃饭"),
            ),
            correctChoiceIndex = 0,
            context = "Grandma Mei welcomes you warmly",
            relatedNpcId = "grandma_mei",
            relatedWordId = "greet_007",
            relatedSpeakingExerciseId = "pron_ex_tone_hello",
            xpReward = 10,
            friendshipBonus = 2,
            isUnlocked = false,
            order = 4,
        ),
        ReadingExercise(
            id = "read_ex_food_eat",
            type = ReadingExerciseType.PHRASE_RECOGNITION,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "吃饭",
            pinyin = "chī fàn",
            english = "to eat",
            syllableTones = hanziRenderer.tonesOf("chī fàn"),
            prompt = "选出“吃饭”这个词。",
            choices = listOf(
                ReadingChoice("choice_0", "吃饭", "chī fàn", "吃饭"),
                ReadingChoice("choice_1", "喝水", "hē shuǐ", "喝水"),
                ReadingChoice("choice_2", "睡觉", "shuì jiào", "睡觉"),
                ReadingChoice("choice_3", "跑步", "pǎo bù", "跑步"),
            ),
            correctChoiceIndex = 0,
            context = "Owner Lin asks if you've eaten",
            relatedNpcId = "owner_lin",
            relatedWordId = "food_001",
            xpReward = 10,
            isUnlocked = true,
            order = 5,
        ),
        ReadingExercise(
            id = "read_ex_food_drink",
            type = ReadingExerciseType.MATCH_SPOKEN_TO_WRITTEN,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "喝",
            pinyin = "hē",
            english = "to drink",
            syllableTones = hanziRenderer.tonesOf("hē"),
            prompt = "“hē”这个发音对应哪个汉字？",
            choices = listOf(
                ReadingChoice("choice_0", "喝", "hē", "喝"),
                ReadingChoice("choice_1", "吃", "chī", "吃"),
                ReadingChoice("choice_2", "做", "zuò", "做"),
                ReadingChoice("choice_3", "买", "mǎi", "买"),
            ),
            correctChoiceIndex = 0,
            context = "At the dumpling shop counter",
            relatedNpcId = "owner_lin",
            relatedWordId = "food_002",
            relatedListeningExerciseId = "listen_ex_food_drink",
            xpReward = 10,
            isUnlocked = true,
            order = 6,
        ),
        ReadingExercise(
            id = "read_ex_num_one",
            type = ReadingExerciseType.CHARACTER_RECOGNITION,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "一",
            pinyin = "yī",
            english = "one",
            syllableTones = hanziRenderer.tonesOf("yī"),
            prompt = "哪个数字汉字是“一”？",
            choices = listOf(
                ReadingChoice("choice_0", "一", "yī", "一"),
                ReadingChoice("choice_1", "二", "èr", "二"),
                ReadingChoice("choice_2", "三", "sān", "三"),
                ReadingChoice("choice_3", "五", "wǔ", "五"),
            ),
            correctChoiceIndex = 0,
            context = "Counting at the market stall",
            xpReward = 10,
            isUnlocked = true,
            order = 7,
        ),
        ReadingExercise(
            id = "read_ex_num_three",
            type = ReadingExerciseType.CHARACTER_RECOGNITION,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "三",
            pinyin = "sān",
            english = "three",
            syllableTones = hanziRenderer.tonesOf("sān"),
            prompt = "找出数字汉字“三”。",
            choices = listOf(
                ReadingChoice("choice_0", "三", "sān", "三"),
                ReadingChoice("choice_1", "一", "yī", "一"),
                ReadingChoice("choice_2", "四", "sì", "四"),
                ReadingChoice("choice_3", "二", "èr", "二"),
            ),
            correctChoiceIndex = 0,
            context = "Buying three buns at the shop",
            xpReward = 10,
            isUnlocked = true,
            order = 8,
        ),
        ReadingExercise(
            id = "read_ex_dir_left",
            type = ReadingExerciseType.CONTEXT_READING,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "左转",
            pinyin = "zuǒ zhuǎn",
            english = "turn left",
            syllableTones = hanziRenderer.tonesOf("zuǒ zhuǎn"),
            prompt = "看到“左转”，你该往哪边走？",
            choices = listOf(
                ReadingChoice("choice_0", "turn left 左转", "zuǒ zhuǎn", "左转"),
                ReadingChoice("choice_1", "go straight 直走", "zhí zǒu", "直走"),
                ReadingChoice("choice_2", "turn right 右转", "yòu zhuǎn", "右转"),
                ReadingChoice("choice_3", "stop 停下", "tíng xià", "停下"),
            ),
            correctChoiceIndex = 0,
            context = "A villager points toward the tea house",
            xpReward = 12,
            isUnlocked = true,
            order = 9,
        ),
        ReadingExercise(
            id = "read_ex_dir_straight",
            type = ReadingExerciseType.CONTEXT_READING,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "直走",
            pinyin = "zhí zǒu",
            english = "go straight",
            syllableTones = hanziRenderer.tonesOf("zhí zǒu"),
            prompt = "牌子上写着“直走”，这意味着？",
            choices = listOf(
                ReadingChoice("choice_0", "go straight 直走", "zhí zǒu", "直走"),
                ReadingChoice("choice_1", "turn left 左转", "zuǒ zhuǎn", "左转"),
                ReadingChoice("choice_2", "go back 回去", "huí qù", "回去"),
                ReadingChoice("choice_3", "cross the street 过马路", "guò mǎ lù", "过马路"),
            ),
            correctChoiceIndex = 0,
            context = "Directions to the temple",
            xpReward = 12,
            isUnlocked = true,
            order = 10,
        ),
        ReadingExercise(
            id = "read_ex_order_tea",
            type = ReadingExerciseType.PHRASE_RECOGNITION,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "我要一杯茶",
            pinyin = "wǒ yào yī bēi chá",
            english = "I'd like a cup of tea",
            syllableTones = hanziRenderer.tonesOf("wǒ yào yī bēi chá"),
            prompt = "选出表达“I'd like a cup of tea”的句子。",
            choices = listOf(
                ReadingChoice("choice_0", "我要一杯茶", "wǒ yào yī bēi chá", "我要一杯茶"),
                ReadingChoice("choice_1", "我要两个包子", "wǒ yào liǎng ge bāo zi", "我要两个包子"),
                ReadingChoice("choice_2", "多少钱", "duō shǎo qián", "多少钱"),
                ReadingChoice("choice_3", "不用了", "bù yòng le", "不用了"),
            ),
            correctChoiceIndex = 0,
            context = "A customer orders at the tea house",
            relatedNpcId = "owner_lin",
            relatedListeningExerciseId = "listen_ex_order_tea",
            xpReward = 12,
            isUnlocked = true,
            order = 11,
        ),
        ReadingExercise(
            id = "read_ex_order_dumplings",
            type = ReadingExerciseType.PHRASE_RECOGNITION,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "我要两个包子",
            pinyin = "wǒ yào liǎng ge bāo zi",
            english = "I'd like two steamed buns",
            syllableTones = hanziRenderer.tonesOf("wǒ yào liǎng ge bāo zi"),
            prompt = "选出“我要两个包子”这句中文。",
            choices = listOf(
                ReadingChoice("choice_0", "我要两个包子", "wǒ yào liǎng ge bāo zi", "我要两个包子"),
                ReadingChoice("choice_1", "我要一杯茶", "wǒ yào yī bēi chá", "我要一杯茶"),
                ReadingChoice("choice_2", "店在哪里", "diàn zài nǎ lǐ", "店在哪里"),
                ReadingChoice("choice_3", "太贵了", "tài guì le", "太贵了"),
            ),
            correctChoiceIndex = 0,
            context = "A customer orders at the dumpling shop",
            relatedNpcId = "owner_lin",
            relatedWordId = "food_001",
            relatedListeningExerciseId = "listen_ex_order_dumplings",
            xpReward = 12,
            isUnlocked = true,
            order = 12,
        ),
        ReadingExercise(
            id = "read_ex_npc_li",
            type = ReadingExerciseType.NPC_DIALOGUE_READING,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "来了！今天吃什么？",
            pinyin = "lái le! Jīn tiān chī shén me?",
            english = "Here you are! What will you have today?",
            syllableTones = hanziRenderer.tonesOf("lái le Jīn tiān chī shén me"),
            prompt = "李叔叔写了这句话。选出你的回答。",
            choices = listOf(
                ReadingChoice("choice_0", "我要两个包子", "wǒ yào liǎng ge bāo zi", "我要两个包子"),
                ReadingChoice("choice_1", "再见", "zài jiàn", "再见"),
                ReadingChoice("choice_2", "多少钱？", "duō shǎo qián", "多少钱"),
                ReadingChoice("choice_3", "今天天气不错", "jīn tiān tiān qì bú cuò", "今天天气不错"),
            ),
            correctChoiceIndex = 0,
            context = "Uncle Li greets you at the dumpling shop",
            relatedNpcId = "npc_li",
            xpReward = 12,
            isUnlocked = false,
            order = 13,
        ),
        ReadingExercise(
            id = "read_ex_npc_grandma",
            type = ReadingExerciseType.NPC_DIALOGUE_READING,
            difficulty = ReadingDifficulty.ELEMENTARY,
            hanzi = "欢迎来到青元村！",
            pinyin = "huān yíng lái dào qīng yuán cūn!",
            english = "Welcome to Qingyuan Village!",
            syllableTones = hanziRenderer.tonesOf("huān yíng lái dào qīng yuán cūn"),
            prompt = "梅奶奶写的这句话是什么意思？",
            choices = listOf(
                ReadingChoice("choice_0", "Welcome to Qingyuan Village 欢迎", "huān yíng", "欢迎"),
                ReadingChoice("choice_1", "Goodbye 再见", "zài jiàn", "再见"),
                ReadingChoice("choice_2", "Thank you 谢谢", "xiè xie", "谢谢"),
                ReadingChoice("choice_3", "Please sit 请坐", "qǐng zuò", "请坐"),
            ),
            correctChoiceIndex = 0,
            context = "Grandma Mei welcomes you to the village",
            relatedNpcId = "grandma_mei",
            friendshipBonus = 2,
            isUnlocked = false,
            order = 14,
        ),
    )

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}