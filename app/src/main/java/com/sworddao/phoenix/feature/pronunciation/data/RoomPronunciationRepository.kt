package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.data.local.RoomJson
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
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomPronunciationRepository @Inject constructor(
    private val dao: SpeakingDao,
    private val vocabularyRepository: VocabularyRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val passportRepository: PassportRepository,
) : PronunciationRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countExercises() == 0) {
                dao.upsertExercises(PronunciationSeedData.createInitialExercises().map { it.toEntity() })
            }
            if (dao.getProgressDocOnce() == null) {
                dao.upsertProgressDoc(SpeakingProgressDocEntity("all", RoomJson.toJson<Map<String, PronunciationProgress>>(emptyMap())))
            }
            if (dao.getStatisticsDocOnce() == null) {
                dao.upsertStatisticsDoc(SpeakingStatisticsEntity("all", RoomJson.toJson(SpeakingStatistics())))
            }
            if (dao.getBadgesDocOnce() == null) {
                dao.upsertBadgesDoc(SpeakingBadgesEntity("all", RoomJson.toJsonList(PronunciationBadge.ALL_BADGES)))
            }
            if (dao.getSessionsDocOnce() == null) {
                dao.upsertSessionsDoc(SpeakingSessionsEntity("all", null, "[]"))
            }
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(SpeakingState().toEntity())
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private suspend fun loadState(): SpeakingState =
        dao.getStateDocOnce()?.toDomain() ?: SpeakingState()

    private suspend fun saveState(state: SpeakingState) {
        dao.upsertStateDoc(state.toEntity())
    }

    private suspend fun loadProgress(): MutableMap<String, PronunciationProgress> {
        val doc = dao.getProgressDocOnce() ?: return mutableMapOf()
        return RoomJson.fromJsonOrNull<Map<String, PronunciationProgress>>(doc.progressJson)?.toMutableMap()
            ?: mutableMapOf()
    }

    private suspend fun saveProgress(progress: Map<String, PronunciationProgress>) {
        dao.upsertProgressDoc(SpeakingProgressDocEntity("all", RoomJson.toJson(progress)))
    }

    private suspend fun loadStatistics(): SpeakingStatistics {
        val doc = dao.getStatisticsDocOnce()
        return RoomJson.fromJsonOrNull<SpeakingStatistics>(doc?.statisticsJson) ?: SpeakingStatistics()
    }

    private suspend fun saveStatistics(statistics: SpeakingStatistics) {
        dao.upsertStatisticsDoc(SpeakingStatisticsEntity("all", RoomJson.toJson(statistics)))
    }

    private suspend fun loadBadges(): MutableList<PronunciationBadge> {
        val doc = dao.getBadgesDocOnce()
        return RoomJson.fromJsonOrNull<List<PronunciationBadge>>(doc?.badgesJson)?.toMutableList()
            ?: PronunciationBadge.ALL_BADGES.toMutableList()
    }

    private suspend fun saveBadges(badges: List<PronunciationBadge>) {
        dao.upsertBadgesDoc(SpeakingBadgesEntity("all", RoomJson.toJsonList(badges)))
    }

    private suspend fun loadSessions(): Pair<PronunciationSession?, List<PronunciationSession>> {
        val doc = dao.getSessionsDocOnce() ?: return null to emptyList()
        return doc.activeSessionJson?.let { RoomJson.fromJsonOrNull<PronunciationSession>(it) } to
            RoomJson.fromJsonList(doc.completedSessionsJson)
    }

    private suspend fun saveSessions(active: PronunciationSession?, completed: List<PronunciationSession>) {
        dao.upsertSessionsDoc(
            SpeakingSessionsEntity(
                id = "all",
                activeSessionJson = active?.let { RoomJson.toJson(it) },
                completedSessionsJson = RoomJson.toJsonList(completed),
            )
        )
    }

    // ---------------------------------------------------------------------
    // Exercise queries
    // ---------------------------------------------------------------------

    override fun getAllExercises(): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getAllExercises().map { list -> list.map { it.toDomain() } } }

    override fun getExerciseById(exerciseId: String): Flow<SpeakingExercise?> =
        seededFlow { dao.getExerciseById(exerciseId).map { it?.toDomain() } }

    override fun getExercisesByType(type: SpeakingExerciseType): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByType(type.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByDifficulty(difficulty: SpeakingDifficulty): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByWord(wordId: String): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByWord(wordId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByPhrase(phraseId: String): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByPhrase(phraseId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByNpc(npcId: String): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByQuest(questId: String): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getExercisesByQuest(questId).map { list -> list.map { it.toDomain() } } }

    override fun getUnlockedExercises(): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() } } }

    override fun getRecommendedExercises(limit: Int): Flow<List<SpeakingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() }.sortedBy { it.order }.take(limit) } }

    // ---------------------------------------------------------------------
    // Progress queries
    // ---------------------------------------------------------------------

    override fun getPronunciationProgress(wordId: String): Flow<PronunciationProgress?> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, PronunciationProgress>>(it.progressJson)?.get(wordId) } } }

    override fun getAllPronunciationProgress(): Flow<List<PronunciationProgress>> =
        seededFlow {
            dao.getProgressDoc().map { doc ->
                val progress = doc?.let { RoomJson.fromJsonOrNull<Map<String, PronunciationProgress>>(it.progressJson)?.values }
                    ?: emptyList()
                progress.sortedWith(compareByDescending<PronunciationProgress> { it.lastPracticedAt ?: 0L }.thenBy { it.wordId })
            }
        }

    override fun getSpeakingStatistics(): Flow<SpeakingStatistics> =
        seededFlow {
            combine(
                dao.getStatisticsDoc(),
                dao.getStateDoc(),
                dao.getBadgesDoc(),
                dao.getProgressDoc(),
            ) { statDoc, stateDoc, badgesDoc, progressDoc ->
                val stats = RoomJson.fromJsonOrNull<SpeakingStatistics>(statDoc?.statisticsJson) ?: SpeakingStatistics()
                val state = stateDoc?.toDomain() ?: SpeakingState()
                val badges = RoomJson.fromJsonOrNull<List<PronunciationBadge>>(badgesDoc?.badgesJson) ?: PronunciationBadge.ALL_BADGES
                val progress = progressDoc?.let { RoomJson.fromJsonOrNull<Map<String, PronunciationProgress>>(it.progressJson) } ?: emptyMap()
                stats.copy(
                    currentStreak = state.currentStreak,
                    longestStreak = state.longestStreak,
                    lastPracticeDate = state.lastPracticeDate,
                    pronunciationBadges = badges,
                    wordsPracticed = progress.values.count { it.totalAttempts > 0 },
                    wordsMastered = progress.values.count { it.isMastered },
                )
            }
        }

    override fun getPronunciationBadges(): Flow<List<PronunciationBadge>> =
        seededFlow { dao.getBadgesDoc().map { doc -> RoomJson.fromJsonOrNull<List<PronunciationBadge>>(doc?.badgesJson) ?: PronunciationBadge.ALL_BADGES } }

    // ---------------------------------------------------------------------
    // Session lifecycle
    // ---------------------------------------------------------------------

    override suspend fun startSession(config: PronunciationSessionConfig): PronunciationSession {
        ensureSeeded()
        val selected = selectExercises(config)
        val session = PronunciationSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        val (_, completed) = loadSessions()
        saveSessions(session, completed)
        return session
    }

    override suspend fun submitAttempt(attempt: PronunciationAttempt): PronunciationResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val session = active ?: return PronunciationResultStatus.Error("No active pronunciation session")

        val exercise = dao.getExerciseByIdOnce(attempt.exerciseId)?.toDomain()
            ?: return PronunciationResultStatus.Error("Exercise not found: ${attempt.exerciseId}")

        val success = attempt.wasSuccessful
        val progressBefore = attempt.wordId?.let { loadProgress()[it] }
        val isNewPersonalBest = attempt.wordId != null &&
            attempt.confidence > (progressBefore?.bestConfidence ?: 0f)

        var state = loadState()
        val (streakIncremented, streakState) = updateStreak(success, attempt.timestamp, state)
        state = streakState
        val currentStreak = state.currentStreak
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

        val updatedSession = session.copy(
            attempts = session.attempts + attempt,
            totalXpEarned = session.totalXpEarned + xpEarned,
            totalFriendshipBonus = session.totalFriendshipBonus + friendshipBonusEarned,
        )
        saveSessions(updatedSession, completed)

        val progressMap = loadProgress()
        val updatedProgress = updateWordProgress(attempt, exercise, progressMap)
        saveProgress(updatedProgress)

        val wordId = attempt.wordId ?: exercise.wordId
        if (wordId != null) {
            state = state.copy(practicedWords = state.practicedWords + wordId)
            if (updatedProgress[wordId]?.bestConfidence != null && updatedProgress[wordId]!!.bestConfidence >= 0.8f) {
                state = state.copy(highConfidenceWords = state.highConfidenceWords + wordId)
            }
        }

        val updatedState = updateStatistics(attempt, exercise, success, updatedProgress, state)
        saveState(updatedState)
        state = updatedState

        if (success) {
            attempt.wordId?.let { vocabularyRepository.incrementSpoken(it) }
            gameProgressRepository.recordSpeakingPractice()
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateSpeakingQuests()
            recordFirstPracticePassportEntry(exercise, attempt)
        }

        val badges = recomputeBadges(state, updatedProgress)
        saveBadges(badges)
        val newState = recordNewBadgeEntries(badges, state)
        saveState(newState)
        state = newState

        val result = PronunciationResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = isNewPersonalBest,
            streakContinued = streakContinued,
            currentStreak = currentStreak,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            badgeProgress = badges.associate { it.id to it.progress },
        )

        return PronunciationResultStatus.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: PronunciationSession): PronunciationResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val activeSession = active ?: return PronunciationResultStatus.Error("No active pronunciation session")
        if (activeSession.id != session.id) {
            return PronunciationResultStatus.Error("Session mismatch")
        }

        val completedSession = activeSession.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )
        saveSessions(null, completed + completedSession)
        var statistics = loadStatistics()
        statistics = statistics.copy(totalSessions = statistics.totalSessions + 1)
        saveStatistics(statistics)
        return PronunciationResultStatus.SessionCompleted(completedSession, statistics)
    }

    override suspend fun updateProgress(progress: PronunciationProgress): PronunciationResultStatus {
        ensureSeeded()
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        val map = loadProgress()
        map[mastered.wordId] = mastered
        saveProgress(map)
        return PronunciationResultStatus.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): PronunciationResultStatus {
        ensureSeeded()
        val exercise = dao.getExerciseByIdOnce(exerciseId)
            ?: return PronunciationResultStatus.Error("Exercise not found: $exerciseId")
        if (exercise.isUnlocked) {
            return PronunciationResultStatus.Success("Exercise already unlocked")
        }
        dao.unlockExercise(exerciseId)
        return PronunciationResultStatus.Success("Exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): PronunciationResultStatus {
        ensureSeeded()
        val original = loadState()
        val state = original.copy(
            currentStreak = streak,
            longestStreak = maxOf(streak, original.longestStreak),
        )
        saveState(state)
        return PronunciationResultStatus.StreakUpdated(
            currentStreak = streak,
            longestStreak = state.longestStreak,
        )
    }

    override suspend fun awardBadge(badgeId: String): PronunciationResultStatus {
        ensureSeeded()
        val definition = PronunciationBadge.getBadge(badgeId)
            ?: return PronunciationResultStatus.Error("Badge not found: $badgeId")

        val badges = loadBadges()
        if (badges.any { it.id == badgeId && it.isEarned }) {
            return PronunciationResultStatus.Error("Badge already earned: $badgeId")
        }

        val now = System.currentTimeMillis()
        val updated = badges.map { badge ->
            if (badge.id == badgeId) {
                badge.copy(progress = 1f, isEarned = true, earnedAt = now)
            } else {
                badge
            }
        }
        saveBadges(updated)

        val badge = updated.find { it.id == badgeId }
        return if (badge?.isEarned == true) {
            PronunciationResultStatus.BadgeEarned(badge)
        } else {
            PronunciationResultStatus.Error("Badge already earned: $badgeId")
        }
    }

    override suspend fun addExercises(exercises: List<SpeakingExercise>): PronunciationResultStatus {
        ensureSeeded()
        val existingIds = dao.getAllExercises().first().map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        if (newExercises.isNotEmpty()) {
            dao.upsertExercises(newExercises.map { it.toEntity() })
        }
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

    private suspend fun buildAttempt(
        expectedText: String,
        expectedPinyin: String,
        spokenText: String,
        similarity: Float,
    ): PronunciationAttempt {
        ensureSeeded()
        var state = loadState()
        val previous = state.lastConfidenceByKey[expectedText]
        val feedback = selectFeedback(similarity, previous)
        state = state.copy(lastConfidenceByKey = state.lastConfidenceByKey + (expectedText to similarity))
        saveState(state)

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
        val unlocked = dao.getUnlockedExercises().first().map { it.toDomain() }
        var selected: List<SpeakingExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.wordId in config.wordIds }
            if (selected.isEmpty()) {
                val dynamic = config.wordIds.mapNotNull { wordId ->
                    val word = vocabularyRepository.getWordById(wordId).first()
                    word?.let { createWordExercise(it) }
                }
                if (dynamic.isNotEmpty()) {
                    val existingIds = dao.getAllExercises().first().map { it.id }.toSet()
                    val fresh = dynamic.filter { it.id !in existingIds }
                    if (fresh.isNotEmpty()) {
                        dao.upsertExercises(fresh.map { it.toEntity() })
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

    private fun createWordExercise(word: VocabularyWord): SpeakingExercise {
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

    private fun updateWordProgress(
        attempt: PronunciationAttempt,
        exercise: SpeakingExercise,
        progressMap: MutableMap<String, PronunciationProgress>,
    ): Map<String, PronunciationProgress> {
        val wordId = attempt.wordId ?: exercise.wordId ?: return progressMap
        val success = attempt.wasSuccessful
        val current = progressMap[wordId]

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
        progressMap[wordId] = mastered
        return progressMap
    }

    private suspend fun updateStatistics(
        attempt: PronunciationAttempt,
        exercise: SpeakingExercise,
        success: Boolean,
        progressMap: Map<String, PronunciationProgress>,
        state: SpeakingState,
    ): SpeakingState {
        var newState = state.copy(
            attemptedExerciseIds = state.attemptedExerciseIds + attempt.exerciseId,
            practiceCountByType = state.practiceCountByType +
                (exercise.type to (state.practiceCountByType[exercise.type] ?: 0) + 1),
            practiceCountByDifficulty = state.practiceCountByDifficulty +
                (exercise.difficulty to (state.practiceCountByDifficulty[exercise.difficulty] ?: 0) + 1),
        )

        if (success) {
            if (exercise.type == SpeakingExerciseType.DIALOGUE_PHRASE ||
                exercise.type == SpeakingExerciseType.REPEAT_AFTER_NPC
            ) {
                newState = newState.copy(
                    dialoguePhraseExercises = newState.dialoguePhraseExercises + exercise.id,
                )
            }
            if (attempt.toneAccuracy >= 0.9f) {
                newState = newState.copy(
                    perfectToneExercises = newState.perfectToneExercises + exercise.id,
                )
            }
        }

        val current = loadStatistics()
        val attempts = current.totalAttempts + 1
        val confidenceSum = newState.confidenceSum + attempt.confidence
        val toneSum = newState.toneSum + attempt.toneAccuracy
        val fluencySum = newState.fluencySum + attempt.fluencyScore

        val updatedStatistics = current.copy(
            totalAttempts = attempts,
            successfulAttempts = current.successfulAttempts + (if (success) 1 else 0),
            totalPracticeTimeMs = current.totalPracticeTimeMs + attempt.durationMs,
            averageConfidence = confidenceSum / attempts,
            averageToneAccuracy = toneSum / attempts,
            averageFluencyScore = fluencySum / attempts,
            exercisesByType = newState.practiceCountByType,
            exercisesByDifficulty = newState.practiceCountByDifficulty,
        )
        saveStatistics(updatedStatistics)

        return newState.copy(
            confidenceSum = confidenceSum,
            toneSum = toneSum,
            fluencySum = fluencySum,
        )
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
        if (loadStatistics().totalAttempts > 1) return

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

    private suspend fun recomputeBadges(
        state: SpeakingState,
        progressMap: Map<String, PronunciationProgress>,
    ): List<PronunciationBadge> {
        val streak = state.currentStreak
        val now = System.currentTimeMillis()
        val masteredCount = progressMap.values.count { it.isMastered }

        return loadBadges().map { badge ->
            val progress = when (badge.id) {
                "first_word" -> if (state.practicedWords.isNotEmpty()) 1f else 0f
                "streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                "streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                "streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                "confident_speaker" -> (state.highConfidenceWords.size / 10f).coerceIn(0f, 1f)
                "tone_master" -> (state.perfectToneExercises.size / 20f).coerceIn(0f, 1f)
                "conversation_ready" -> (state.dialoguePhraseExercises.size / 50f).coerceIn(0f, 1f)
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

    private suspend fun recordNewBadgeEntries(badges: List<PronunciationBadge>, state: SpeakingState): SpeakingState {
        val newlyEarned = badges.filter { it.isEarned && it.id !in state.recordedBadgeIds }
        newlyEarned.forEach { badge ->
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
        if (newlyEarned.isEmpty()) return state
        return state.copy(recordedBadgeIds = state.recordedBadgeIds + newlyEarned.map { it.id })
    }

    private fun updateStreak(wasSuccessful: Boolean, timestamp: Long, state: SpeakingState): Pair<Boolean, SpeakingState> {
        if (!wasSuccessful) return false to state

        val today = getStartOfDay(timestamp)
        val last = state.lastPracticeDate
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> true to state.copy(currentStreak = 1, lastPracticeDate = timestamp)
            lastDay == today -> false to state.copy(lastPracticeDate = timestamp)
            lastDay == today - DAY_MILLIS -> {
                val newStreak = state.currentStreak + 1
                true to state.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, state.longestStreak),
                    lastPracticeDate = timestamp,
                )
            }
            else -> true to state.copy(currentStreak = 1, lastPracticeDate = timestamp)
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

    private companion object {
        const val SUCCESS_THRESHOLD = 0.7f
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
