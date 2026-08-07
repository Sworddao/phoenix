package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.data.seed.PronunciationSeedData

import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory mock implementation of [PronunciationRepository].
 *
 * The mock runs the complete offline pronunciation workflow:
 * session lifecycle, attempts, streaks, mastery, statistics, rewards and
 * badges. It also feeds the surrounding gameplay systems (vocabulary,
 * quests, friendship, game progress and passport) so speaking practice is
 * integrated end-to-end. A real speech recognition backend can later replace
 * the evaluation logic without touching the rest of the app.
 */
@Singleton
class MockPronunciationRepository @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val passportRepository: PassportRepository,
) : PronunciationRepository {

    private val _exercises = MutableStateFlow(createInitialExercises())
    private val _progressByWord = MutableStateFlow<Map<String, PronunciationProgress>>(emptyMap())
    private val _statistics = MutableStateFlow(SpeakingStatistics())
    private val _activeSession = MutableStateFlow<PronunciationSession?>(null)
    private val _completedSessions = MutableStateFlow<List<PronunciationSession>>(emptyList())
    private val _currentStreak = MutableStateFlow(0)
    private val _longestStreak = MutableStateFlow(0)
    private val _lastPracticeDate = MutableStateFlow<Long?>(null)
    private val _badges = MutableStateFlow(PronunciationBadge.ALL_BADGES)

    private val _practicedWords = mutableSetOf<String>()
    private val _highConfidenceWords = mutableSetOf<String>()
    private val _perfectToneExercises = mutableSetOf<String>()
    private val _dialoguePhraseExercises = mutableSetOf<String>()
    private val _attemptedExerciseIds = mutableSetOf<String>()
    private val _practiceCountByType = mutableMapOf<SpeakingExerciseType, Int>()
    private val _practiceCountByDifficulty = mutableMapOf<SpeakingDifficulty, Int>()
    private val _recordedBadgeIds = mutableSetOf<String>()
    private val _lastConfidenceByKey = mutableMapOf<String, Float>()
    private var _confidenceSum = 0f
    private var _toneSum = 0f
    private var _fluencySum = 0f

    // ---------------------------------------------------------------------
    // Exercise queries
    // ---------------------------------------------------------------------

    override fun getAllExercises(): Flow<List<SpeakingExercise>> = _exercises

    override fun getExerciseById(exerciseId: String): Flow<SpeakingExercise?> =
        _exercises.map { exercises -> exercises.find { it.id == exerciseId } }

    override fun getExercisesByType(type: SpeakingExerciseType): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.type == type } }

    override fun getExercisesByDifficulty(difficulty: SpeakingDifficulty): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.difficulty == difficulty } }

    override fun getExercisesByWord(wordId: String): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.wordId == wordId } }

    override fun getExercisesByPhrase(phraseId: String): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.phraseId == phraseId } }

    override fun getExercisesByNpc(npcId: String): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.relatedNpcId == npcId } }

    override fun getExercisesByQuest(questId: String): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.relatedQuestId == questId } }

    override fun getUnlockedExercises(): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises -> exercises.filter { it.isUnlocked } }

    override fun getRecommendedExercises(limit: Int): Flow<List<SpeakingExercise>> =
        _exercises.map { exercises ->
            exercises.filter { it.isUnlocked }.sortedBy { it.order }.take(limit)
        }

    // ---------------------------------------------------------------------
    // Progress queries
    // ---------------------------------------------------------------------

    override fun getPronunciationProgress(wordId: String): Flow<PronunciationProgress?> =
        _progressByWord.map { progress -> progress[wordId] }

    override fun getAllPronunciationProgress(): Flow<List<PronunciationProgress>> =
        _progressByWord.map { progress ->
            progress.values.sortedWith(compareByDescending<PronunciationProgress> { it.lastPracticedAt ?: 0L }.thenBy { it.wordId })
        }

    override fun getSpeakingStatistics(): Flow<SpeakingStatistics> =
        combine(
            combine(
                _statistics,
                _currentStreak,
                _longestStreak,
                _lastPracticeDate,
                _badges,
            ) { stats, streak, longestStreak, lastDate, badges ->
                stats.copy(
                    currentStreak = streak,
                    longestStreak = longestStreak,
                    lastPracticeDate = lastDate,
                    pronunciationBadges = badges,
                )
            },
            _progressByWord,
        ) { stats, progress ->
            stats.copy(
                wordsPracticed = progress.values.count { it.totalAttempts > 0 },
                wordsMastered = progress.values.count { it.isMastered },
            )
        }

    override fun getPronunciationBadges(): Flow<List<PronunciationBadge>> = _badges

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    override suspend fun startSession(config: PronunciationSessionConfig): PronunciationSession {
        val selected = selectExercises(config)
        val session = PronunciationSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        _activeSession.value = session
        return session
    }

    override suspend fun submitAttempt(attempt: PronunciationAttempt): PronunciationResultStatus {
        val session = _activeSession.value
            ?: return PronunciationResultStatus.Error("No active pronunciation session")

        val exercise = _exercises.value.find { it.id == attempt.exerciseId }
            ?: return PronunciationResultStatus.Error("Exercise not found: ${attempt.exerciseId}")

        val success = attempt.wasSuccessful
        val progressBefore = attempt.wordId?.let { _progressByWord.value[it] }
        val isNewPersonalBest = attempt.wordId != null &&
            attempt.confidence > (progressBefore?.bestConfidence ?: 0f)

        val streakIncremented = updateStreak(success, attempt.timestamp)
        val currentStreak = _currentStreak.value
        val streakContinued = success && currentStreak >= 1 && streakIncremented

        val xpEarned = if (success) {
            val streakBonus = if (currentStreak >= 2) 5 else 0
            exercise.xpReward + streakBonus
        } else {
            0
        }

        val friendshipBonusEarned = if (success && exercise.relatedNpcId != null) {
            exercise.friendshipBonus
        } else {
            0
        }

        _activeSession.update { active ->
            active?.copy(
                attempts = active.attempts + attempt,
                totalXpEarned = active.totalXpEarned + xpEarned,
                totalFriendshipBonus = active.totalFriendshipBonus + friendshipBonusEarned,
            )
        }

        updateWordProgress(attempt, exercise)
        updateStatistics(attempt, exercise, success)

        if (success) {
            attempt.wordId?.let { vocabularyRepository.incrementSpoken(it) }
            gameProgressRepository.recordSpeakingPractice()
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateSpeakingQuests()
            recordFirstPracticePassportEntry(exercise, attempt)
        }

        recomputeBadges()
        recordNewBadgeEntries()

        val result = PronunciationResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = isNewPersonalBest,
            streakContinued = streakContinued,
            currentStreak = currentStreak,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            badgeProgress = _badges.value.associate { it.id to it.progress },
        )

        return PronunciationResultStatus.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: PronunciationSession): PronunciationResultStatus {
        val active = _activeSession.value
            ?: return PronunciationResultStatus.Error("No active pronunciation session")
        if (active.id != session.id) {
            return PronunciationResultStatus.Error("Session mismatch")
        }

        val completed = active.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )

        _completedSessions.update { sessions -> sessions + completed }
        _activeSession.value = null
        _statistics.update { it.copy(totalSessions = it.totalSessions + 1) }

        return PronunciationResultStatus.SessionCompleted(completed, _statistics.value)
    }

    override suspend fun updateProgress(progress: PronunciationProgress): PronunciationResultStatus {
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        _progressByWord.update { it + (mastered.wordId to mastered) }
        return PronunciationResultStatus.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): PronunciationResultStatus {
        val exercise = _exercises.value.find { it.id == exerciseId }
            ?: return PronunciationResultStatus.Error("Exercise not found: $exerciseId")

        if (exercise.isUnlocked) {
            return PronunciationResultStatus.Success("Exercise already unlocked")
        }

        _exercises.update { exercises ->
            exercises.map { if (it.id == exerciseId) it.copy(isUnlocked = true) else it }
        }
        return PronunciationResultStatus.Success("Exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): PronunciationResultStatus {
        _currentStreak.value = streak
        if (streak > _longestStreak.value) {
            _longestStreak.value = streak
        }
        return PronunciationResultStatus.StreakUpdated(
            currentStreak = streak,
            longestStreak = _longestStreak.value,
        )
    }

    override suspend fun awardBadge(badgeId: String): PronunciationResultStatus {
        val definition = PronunciationBadge.getBadge(badgeId)
            ?: return PronunciationResultStatus.Error("Badge not found: $badgeId")

        if (_badges.value.any { it.id == badgeId && it.isEarned }) {
            return PronunciationResultStatus.Error("Badge already earned: $badgeId")
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
            PronunciationResultStatus.BadgeEarned(badge)
        } else {
            PronunciationResultStatus.Error("Badge already earned: $badgeId")
        }
    }

    override suspend fun addExercises(exercises: List<SpeakingExercise>): PronunciationResultStatus {
        val existingIds = _exercises.value.map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        _exercises.update { current -> current + newExercises }
        return PronunciationResultStatus.Success("Added ${newExercises.size} exercises")
    }

    // ---------------------------------------------------------------------
    // Evaluation
    // ---------------------------------------------------------------------

    override suspend fun evaluatePronunciation(
        expectedText: String,
        expectedPinyin: String,
        spokenAudioPath: String,
    ): PronunciationAttempt {
        val similarity = calculateSimilarity(expectedPinyin, expectedPinyin)
        return buildAttempt(expectedText, expectedPinyin, expectedPinyin, similarity)
    }

    override suspend fun evaluatePronunciationOffline(
        expectedText: String,
        expectedPinyin: String,
        spokenText: String,
    ): PronunciationAttempt {
        val similarity = calculateSimilarity(expectedPinyin, spokenText)
        return buildAttempt(expectedText, expectedPinyin, spokenText, similarity)
    }

    private fun buildAttempt(
        expectedText: String,
        expectedPinyin: String,
        spokenText: String,
        similarity: Float,
    ): PronunciationAttempt {
        val previous = _lastConfidenceByKey[expectedText]
        val feedback = selectFeedback(similarity, previous)
        _lastConfidenceByKey[expectedText] = similarity

        return PronunciationAttempt(
            exerciseId = "",
            wordId = null,
            phraseId = null,
            expectedText = expectedText,
            expectedPinyin = expectedPinyin,
            spokenText = spokenText,
            confidence = similarity,
            feedbackType = feedback,
            toneAccuracy = similarity * 0.9f,
            fluencyScore = similarity * 0.85f,
            wasSuccessful = similarity >= SUCCESS_THRESHOLD,
        )
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    private suspend fun selectExercises(config: PronunciationSessionConfig): List<SpeakingExercise> {
        val unlocked = _exercises.value.filter { it.isUnlocked }
        var selected: List<SpeakingExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.wordId in config.wordIds }
            if (selected.isEmpty()) {
                val dynamic = config.wordIds.mapNotNull { wordId ->
                    val word = vocabularyRepository.getWordById(wordId).first()
                    word?.let { createWordExercise(it) }
                }
                if (dynamic.isNotEmpty()) {
                    val existingIds = _exercises.value.map { it.id }.toSet()
                    _exercises.update { current ->
                        current + dynamic.filter { it.id !in existingIds }
                    }
                    selected = dynamic
                }
            }
        } else if (config.phraseIds.isNotEmpty()) {
            selected = unlocked.filter { it.phraseId in config.phraseIds }
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

    private fun createWordExercise(
        word: com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord,
    ): SpeakingExercise {
        return SpeakingExercise(
            id = "dynamic_${word.id}",
            type = SpeakingExerciseType.VOCABULARY_WORD,
            difficulty = SpeakingDifficulty.entries.find { it.level == word.difficulty.level }
                ?: SpeakingDifficulty.BEGINNER,
            expectedText = word.pinyin,
            expectedPinyin = word.pinyin,
            expectedHanzi = word.hanzi,
            wordId = word.id,
            context = word.exampleSentence,
            relatedNpcId = word.relatedNpcId,
            xpReward = 10,
            isUnlocked = true,
        )
    }

    private fun updateWordProgress(attempt: PronunciationAttempt, exercise: SpeakingExercise) {
        val wordId = attempt.wordId ?: exercise.wordId ?: return
        val success = attempt.wasSuccessful
        val current = _progressByWord.value[wordId]

        val newCurrentStreak = if (success) (current?.currentStreak ?: 0) + 1 else 0
        val updated = PronunciationProgress(
            wordId = wordId,
            totalAttempts = (current?.totalAttempts ?: 0) + 1,
            successfulAttempts = (current?.successfulAttempts ?: 0) + (if (success) 1 else 0),
            bestConfidence = maxOf(current?.bestConfidence ?: 0f, attempt.confidence),
            bestToneAccuracy = maxOf(current?.bestToneAccuracy ?: 0f, attempt.toneAccuracy),
            bestFluencyScore = maxOf(current?.bestFluencyScore ?: 0f, attempt.fluencyScore),
            currentStreak = newCurrentStreak,
            longestStreak = maxOf(current?.longestStreak ?: 0, newCurrentStreak),
            lastPracticedAt = attempt.timestamp,
            totalPracticeTimeMs = (current?.totalPracticeTimeMs ?: 0) + attempt.durationMs,
            masteryLevel = SpeakingMastery.NEW,
        )

        val mastered = updated.copy(masteryLevel = calculateMastery(updated))
        _progressByWord.update { it + (wordId to mastered) }
        _practicedWords += wordId
        if (mastered.bestConfidence >= 0.8f) {
            _highConfidenceWords += wordId
        }
    }

    private fun updateStatistics(
        attempt: PronunciationAttempt,
        exercise: SpeakingExercise,
        success: Boolean,
    ) {
        _attemptedExerciseIds += attempt.exerciseId
        _practiceCountByType[exercise.type] = (_practiceCountByType[exercise.type] ?: 0) + 1
        _practiceCountByDifficulty[exercise.difficulty] =
            (_practiceCountByDifficulty[exercise.difficulty] ?: 0) + 1

        if (success) {
            if (exercise.type == SpeakingExerciseType.DIALOGUE_PHRASE ||
                exercise.type == SpeakingExerciseType.REPEAT_AFTER_NPC
            ) {
                _dialoguePhraseExercises += exercise.id
            }
            if (attempt.toneAccuracy >= 0.9f) {
                _perfectToneExercises += exercise.id
            }
        }

        val attempts = _statistics.value.totalAttempts + 1
        _confidenceSum += attempt.confidence
        _toneSum += attempt.toneAccuracy
        _fluencySum += attempt.fluencyScore

        _statistics.update { stats ->
            stats.copy(
                totalAttempts = attempts,
                successfulAttempts = stats.successfulAttempts + (if (success) 1 else 0),
                totalPracticeTimeMs = stats.totalPracticeTimeMs + attempt.durationMs,
                averageConfidence = _confidenceSum / attempts,
                averageToneAccuracy = _toneSum / attempts,
                averageFluencyScore = _fluencySum / attempts,
                exercisesByType = _practiceCountByType.toMap(),
                exercisesByDifficulty = _practiceCountByDifficulty.toMap(),
            )
        }
    }

    private suspend fun updateSpeakingQuests() {
        val quests = questRepository.getAllQuests().first()
        quests.filter { it.status == QuestStatus.ACTIVE }
            .forEach { quest ->
                quest.objectives
                    .filter { it.type == ObjectiveType.PRACTICE_SPEAKING }
                    .forEach { objective ->
                        questRepository.updateObjectiveProgress(quest.id, objective.id, 1)
                    }
            }
    }

    private suspend fun recordFirstPracticePassportEntry(
        exercise: SpeakingExercise,
        attempt: PronunciationAttempt,
    ) {
        if (_statistics.value.totalAttempts > 1) return

        passportRepository.recordEntry(
            PassportEntry(
                id = UUID.randomUUID().toString(),
                regionId = "qingyuan_village",
                type = EntryType.SPEAKING_PRACTICE,
                title = "第一次开口练习",
                description = "你第一次开口练习了“${exercise.expectedText}”！",
                metadata = mapOf(
                    "wordId" to (exercise.wordId ?: ""),
                    "confidence" to attempt.confidence.toString(),
                ),
            )
        )
    }

    private fun recomputeBadges() {
        val streak = _currentStreak.value
        val now = System.currentTimeMillis()
        val masteredCount = _progressByWord.value.values.count { it.isMastered }

        _badges.update { badges ->
            badges.map { badge ->
                val progress = when (badge.id) {
                    "first_word" ->
                        if (_practicedWords.isNotEmpty()) 1f else 0f
                    "streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                    "streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                    "streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                    "confident_speaker" -> (_highConfidenceWords.size / 10f).coerceIn(0f, 1f)
                    "tone_master" -> (_perfectToneExercises.size / 20f).coerceIn(0f, 1f)
                    "conversation_ready" -> (_dialoguePhraseExercises.size / 50f).coerceIn(0f, 1f)
                    "pronunciation_pro" -> (masteredCount / 100f).coerceIn(0f, 1f)
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

    private fun updateStreak(wasSuccessful: Boolean, timestamp: Long): Boolean {
        if (!wasSuccessful) return false

        val today = getStartOfDay(timestamp)
        val last = _lastPracticeDate.value
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> {
                _currentStreak.value = 1
                _lastPracticeDate.value = timestamp
                true
            }
            lastDay == today -> {
                _lastPracticeDate.value = timestamp
                false
            }
            lastDay == today - DAY_MILLIS -> {
                _currentStreak.value += 1
                if (_currentStreak.value > _longestStreak.value) {
                    _longestStreak.value = _currentStreak.value
                }
                _lastPracticeDate.value = timestamp
                true
            }
            else -> {
                _currentStreak.value = 1
                _lastPracticeDate.value = timestamp
                true
            }
        }
    }

    private fun selectFeedback(similarity: Float, previous: Float?): PronunciationFeedbackType {
        return when {
            previous != null && similarity > previous ->
                PronunciationFeedbackType.NICE_IMPROVEMENT
            similarity >= 0.85f -> PronunciationFeedbackType.EXCELLENT
            similarity >= 0.7f -> {
                if (previous != null && similarity > previous) {
                    PronunciationFeedbackType.NICE_IMPROVEMENT
                } else {
                    PronunciationFeedbackType.GREAT_START
                }
            }
            similarity >= 0.55f -> PronunciationFeedbackType.ALMOST
            similarity >= 0.4f -> PronunciationFeedbackType.GETTING_CLOSER
            similarity >= 0.25f -> PronunciationFeedbackType.TRY_AGAIN
            else -> PronunciationFeedbackType.KEEP_PRACTICING
        }
    }

    private fun calculateMastery(progress: PronunciationProgress): SpeakingMastery {
        return SpeakingMastery.entries.lastOrNull { mastery ->
            progress.totalAttempts >= mastery.minAttempts &&
                progress.successRate >= mastery.requiredSuccessRate
        } ?: SpeakingMastery.NEW
    }

    private fun calculateSimilarity(expected: String, actual: String): Float {
        val expectedNormalized = normalize(expected)
        val actualNormalized = normalize(actual)

        if (expectedNormalized == actualNormalized) return 1.0f
        if (expectedNormalized.isEmpty() && actualNormalized.isEmpty()) return 1.0f

        val maxLen = maxOf(expectedNormalized.length, actualNormalized.length)
        if (maxLen == 0) return 1.0f

        var matches = 0
        for (i in 0 until minOf(expectedNormalized.length, actualNormalized.length)) {
            if (expectedNormalized[i] == actualNormalized[i]) matches++
        }

        return matches.toFloat() / maxLen
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(" ", "")
            .replace("，", "")
            .replace("。", "")
            .replace("！", "")
            .replace("？", "")
            .replace(",", "")
            .replace(".", "")
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

    
    private fun createInitialExercises(): List<SpeakingExercise> =
        PronunciationSeedData.createInitialExercises()

    companion object {
        const val SUCCESS_THRESHOLD = 0.7f
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
