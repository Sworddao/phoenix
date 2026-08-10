package com.sworddao.phoenix.feature.reading.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.data.seed.ReadingSeedData
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus as Status
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
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
class RoomReadingRepository @Inject constructor(
    private val dao: ReadingDao,
    private val vocabularyRepository: VocabularyRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val passportRepository: PassportRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val listeningRepository: ListeningRepository,
    private val hanziRenderer: HanziRenderer,
) : ReadingRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countExercises() == 0) {
                dao.upsertExercises(ReadingSeedData.createInitialExercises().map { it.toEntity() })
            }
            if (dao.getProgressDocOnce() == null) {
                dao.upsertProgressDoc(ReadingProgressDocEntity("all", RoomJson.toJson<Map<String, ReadingProgress>>(emptyMap())))
            }
            if (dao.getStatisticsDocOnce() == null) {
                dao.upsertStatisticsDoc(ReadingStatisticsEntity("all", RoomJson.toJson(ReadingStatistics())))
            }
            if (dao.getBadgesDocOnce() == null) {
                dao.upsertBadgesDoc(ReadingBadgesEntity("all", RoomJson.toJsonList(ReadingBadge.ALL_BADGES)))
            }
            if (dao.getSessionsDocOnce() == null) {
                dao.upsertSessionsDoc(ReadingSessionsEntity("all", null, "[]"))
            }
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(ReadingState().toEntity())
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private suspend fun loadState(): ReadingState {
        val doc = dao.getStateDocOnce() ?: return ReadingState()
        return doc.toDomain(
            readWords = RoomJson.fromJsonList<String>(doc.readWordsJson).toSet(),
            recordedBadgeIds = RoomJson.fromJsonList<String>(doc.recordedBadgeIdsJson).toSet(),
        )
    }

    private suspend fun saveState(state: ReadingState) {
        dao.upsertStateDoc(state.toEntity())
    }

    private suspend fun loadProgress(): MutableMap<String, ReadingProgress> {
        val doc = dao.getProgressDocOnce()
            ?: return mutableMapOf()
        return RoomJson.fromJsonOrNull<Map<String, ReadingProgress>>(doc.progressJson)?.toMutableMap()
            ?: mutableMapOf()
    }

    private suspend fun saveProgress(progress: Map<String, ReadingProgress>) {
        dao.upsertProgressDoc(ReadingProgressDocEntity("all", RoomJson.toJson(progress)))
    }

    private suspend fun loadStatistics(): ReadingStatistics {
        val doc = dao.getStatisticsDocOnce()
        return RoomJson.fromJsonOrNull<ReadingStatistics>(doc?.statisticsJson) ?: ReadingStatistics()
    }

    private suspend fun saveStatistics(statistics: ReadingStatistics) {
        dao.upsertStatisticsDoc(ReadingStatisticsEntity("all", RoomJson.toJson(statistics)))
    }

    private suspend fun loadBadges(): MutableList<ReadingBadge> {
        val doc = dao.getBadgesDocOnce()
        return RoomJson.fromJsonOrNull<List<ReadingBadge>>(doc?.badgesJson)?.toMutableList()
            ?: ReadingBadge.ALL_BADGES.toMutableList()
    }

    private suspend fun saveBadges(badges: List<ReadingBadge>) {
        dao.upsertBadgesDoc(ReadingBadgesEntity("all", RoomJson.toJsonList(badges)))
    }

    private suspend fun loadSessions(): Pair<ReadingSession?, List<ReadingSession>> {
        val doc = dao.getSessionsDocOnce() ?: return null to emptyList()
        return doc.activeSessionJson?.let { RoomJson.fromJsonOrNull<ReadingSession>(it) } to
            RoomJson.fromJsonList(doc.completedSessionsJson)
    }

    private suspend fun saveSessions(active: ReadingSession?, completed: List<ReadingSession>) {
        dao.upsertSessionsDoc(
            ReadingSessionsEntity(
                id = "all",
                activeSessionJson = active?.let { RoomJson.toJson(it) },
                completedSessionsJson = RoomJson.toJsonList(completed),
            )
        )
    }

    override fun getAllExercises(): Flow<List<ReadingExercise>> =
        seededFlow { dao.getAllExercises().map { list -> list.map { it.toDomain() } } }

    override fun getExerciseById(exerciseId: String): Flow<ReadingExercise?> =
        seededFlow { dao.getExerciseById(exerciseId).map { it?.toDomain() } }

    override fun getExercisesByType(type: ReadingExerciseType): Flow<List<ReadingExercise>> =
        seededFlow { dao.getExercisesByType(type.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByDifficulty(difficulty: ReadingDifficulty): Flow<List<ReadingExercise>> =
        seededFlow { dao.getExercisesByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByWord(wordId: String): Flow<List<ReadingExercise>> =
        seededFlow { dao.getExercisesByWord(wordId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByNpc(npcId: String): Flow<List<ReadingExercise>> =
        seededFlow { dao.getExercisesByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByQuest(questId: String): Flow<List<ReadingExercise>> =
        seededFlow { dao.getExercisesByQuest(questId).map { list -> list.map { it.toDomain() } } }

    override fun getUnlockedExercises(): Flow<List<ReadingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() } } }

    override fun getRecommendedExercises(limit: Int): Flow<List<ReadingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() }.sortedBy { it.order }.take(limit) } }

    override fun getReadingProgress(itemId: String): Flow<ReadingProgress?> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, ReadingProgress>>(it.progressJson)?.get(itemId) } } }

    override fun getAllReadingProgress(): Flow<List<ReadingProgress>> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, ReadingProgress>>(it.progressJson)?.values?.toList() } ?: emptyList() } }

    override fun getReadingStatistics(): Flow<ReadingStatistics> =
        seededFlow { dao.getStatisticsDoc().map { doc -> RoomJson.fromJsonOrNull<ReadingStatistics>(doc?.statisticsJson) ?: ReadingStatistics() } }

    override fun getReadingBadges(): Flow<List<ReadingBadge>> =
        seededFlow { dao.getBadgesDoc().map { doc -> RoomJson.fromJsonOrNull<List<ReadingBadge>>(doc?.badgesJson) ?: ReadingBadge.ALL_BADGES } }

    override suspend fun startSession(config: ReadingSessionConfig): ReadingSession {
        ensureSeeded()
        val selected = selectExercises(config)
        val session = ReadingSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        val (_, completed) = loadSessions()
        saveSessions(session, completed)
        return session
    }

    override suspend fun submitAnswer(attempt: ReadingAttempt): ReadingResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val session = active ?: return Status.Error("No active reading session")

        val exercise = dao.getExerciseById(attempt.exerciseId).first()?.toDomain()
            ?: return Status.Error("Exercise not found: ${attempt.exerciseId}")

        val correct = attempt.wasCorrect
        var state = loadState()
        val (streakIncremented, updatedState) = updateStreak(correct, attempt.timestamp, state)
        state = updatedState
        val currentStreakValue = state.currentStreak
        val streakContinued = correct && currentStreakValue >= 1 && streakIncremented

        val xpEarned = if (correct) {
            val streakBonus = if (currentStreakValue >= 2) 5 else 0
            exercise.xpReward + streakBonus
        } else 0
        val friendshipBonusEarned = if (correct && exercise.relatedNpcId != null) exercise.friendshipBonus else 0

        val progressMap = loadProgress()
        val existingBestTime = progressFor(attempt, exercise, progressMap).bestTimeMs

        val updatedSession = session.copy(
            attempts = session.attempts + attempt,
            totalXpEarned = session.totalXpEarned + xpEarned,
            totalFriendshipBonus = session.totalFriendshipBonus + friendshipBonusEarned,
            totalReveals = session.totalReveals + if (attempt.revealedHanziBeforeAnswer) 1 else 0,
        )
        saveSessions(updatedSession, completed)

        val updatedProgressMap = updateReadingProgress(attempt, exercise, progressMap)
        saveProgress(updatedProgressMap)
        attempt.wordId?.let { state = state.copy(readWords = state.readWords + it) }
        var statistics = updateStatistics(attempt, exercise, correct, updatedProgressMap, state)
        saveStatistics(statistics)
        if (correct) {
            state = state.copy(correctCount = state.correctCount + 1)
            if (exercise.relatedNpcId != null) {
                state = state.copy(npcExerciseCount = state.npcExerciseCount + 1)
            }
        }
        saveState(state)

        if (correct) {
            attempt.wordId?.let { vocabularyRepository.incrementRead(it) }
            gameProgressRepository.recordReadingPractice()
            exercise.relatedSpeakingExerciseId?.let { speakingId ->
                val result = pronunciationRepository.unlockExercise(speakingId)
                if (result is PronunciationResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            exercise.relatedListeningExerciseId?.let { listeningId ->
                val result = listeningRepository.unlockExercise(listeningId)
                if (result is ListeningResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateReadingQuests()
        }
        recordFirstReadingPassportEntry(exercise, attempt, statistics)

        val badges = recomputeBadges(state, updatedProgressMap)
        saveBadges(badges)
        updateBadgePassportEntries(badges, state)
        saveState(state)

        val newMastery = progressFor(attempt, exercise, updatedProgressMap).masteryLevel
        val isNewPersonalBest = attempt.wasCorrect &&
            (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime)
        val wasFirstReading = correct && !state.firstReadingRecorded
        if (wasFirstReading) {
            state = state.copy(firstReadingRecorded = true)
        }

        val result = ReadingResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = isNewPersonalBest,
            streakContinued = streakContinued,
            currentStreak = currentStreakValue,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            reward = ReadingReward(
                xpEarned = xpEarned,
                friendshipBonusEarned = friendshipBonusEarned,
                streakContinued = streakContinued,
                isFirstWordRead = wasFirstReading,
                newMastery = if (correct) newMastery else null,
                isNewPersonalBest = isNewPersonalBest,
                badgeProgress = badges.associate { it.id to it.progress },
            ),
            badgeProgress = badges.associate { it.id to it.progress },
        )
        saveState(state)

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: ReadingSession): ReadingResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val activeSession = active ?: return Status.Error("No active reading session")
        if (activeSession.id != session.id) {
            return Status.Error("Session mismatch")
        }
        val completedSession = activeSession.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )
        saveSessions(null, completed + completedSession)
        var statistics = loadStatistics()
        statistics = statistics.copy(totalSessions = statistics.totalSessions + 1)
        saveStatistics(statistics)
        return Status.SessionCompleted(completedSession, statistics)
    }

    override suspend fun updateProgress(progress: ReadingProgress): ReadingResultStatus {
        ensureSeeded()
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        val map = loadProgress()
        map[mastered.itemId] = mastered
        saveProgress(map)
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): ReadingResultStatus {
        ensureSeeded()
        val exercise = dao.getExerciseById(exerciseId).first()
            ?: return Status.Error("Exercise not found: $exerciseId")
        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }
        dao.unlockExercise(exerciseId)
        return Status.Success("Reading exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): ReadingResultStatus {
        ensureSeeded()
        val original = loadState()
        val state = original.copy(
            currentStreak = streak,
            longestStreak = maxOf(streak, original.longestStreak),
        )
        saveState(state)
        var statistics = loadStatistics()
        statistics = statistics.copy(
            currentStreak = state.currentStreak,
            longestStreak = state.longestStreak,
            lastReadingDate = state.lastReadingDate,
        )
        saveStatistics(statistics)
        return Status.StreakUpdated(state.currentStreak, state.longestStreak)
    }

    override suspend fun awardBadge(badgeId: String): ReadingResultStatus {
        ensureSeeded()
        val badges = loadBadges()
        if (badges.any { it.id == badgeId && it.isEarned }) {
            return Status.Error("Badge already earned: $badgeId")
        }
        val now = System.currentTimeMillis()
        val updated = badges.map { badge ->
            if (badge.id == badgeId) badge.copy(progress = 1f, isEarned = true, earnedAt = now) else badge
        }
        saveBadges(updated)
        val badge = updated.find { it.id == badgeId }
        return if (badge?.isEarned == true) Status.BadgeEarned(badge)
        else Status.Error("Badge already earned: $badgeId")
    }

    override suspend fun addExercises(exercises: List<ReadingExercise>): ReadingResultStatus {
        ensureSeeded()
        val existingIds = dao.getAllExercises().first().map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        if (newExercises.isNotEmpty()) {
            dao.upsertExercises(newExercises.map { it.toEntity() })
        }
        return Status.Success("Added ${newExercises.size} reading exercises")
    }

    override suspend fun recordReveal(wordId: String): ReadingResultStatus {
        ensureSeeded()
        val map = loadProgress()
        val existing = map[wordId] ?: ReadingProgress(itemId = wordId, wordId = wordId)
        map[wordId] = existing.copy(
            timesRevealed = existing.timesRevealed + 1,
            hasRevealedHanzi = true,
        )
        saveProgress(map)
        var statistics = loadStatistics()
        statistics = statistics.copy(totalReveals = statistics.totalReveals + 1)
        saveStatistics(statistics)
        return Status.RevealRecorded(wordId, map[wordId]!!.timesRevealed)
    }

    private suspend fun selectExercises(config: ReadingSessionConfig): List<ReadingExercise> {
        val unlocked = dao.getUnlockedExercises().first().map { it.toDomain() }
        var selected: List<ReadingExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.relatedWordId in config.wordIds }
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
        } else if (config.npcId != null) {
            selected = unlocked.filter { it.relatedNpcId == config.npcId }
        } else if (config.questId != null) {
            selected = unlocked.filter { it.relatedQuestId == config.questId }
        } else {
            selected = unlocked.filter { it.type == config.exerciseType && it.difficulty == config.difficulty }
            if (selected.isEmpty()) {
                selected = unlocked.filter { it.type == config.exerciseType }
            }
        }

        return selected.take(config.exerciseCount).ifEmpty { unlocked.take(config.exerciseCount) }
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

    private fun updateReadingProgress(
        attempt: ReadingAttempt,
        exercise: ReadingExercise,
        progressMap: MutableMap<String, ReadingProgress>,
    ): Map<String, ReadingProgress> {
        val itemId = attempt.wordId ?: exercise.id
        val existing = progressMap[itemId]
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

        progressMap[itemId] = updated
        return progressMap
    }

    private suspend fun updateStatistics(
        attempt: ReadingAttempt,
        exercise: ReadingExercise,
        correct: Boolean,
        progressMap: Map<String, ReadingProgress>,
        state: ReadingState,
    ): ReadingStatistics {
        val current = loadStatistics()
        val attempts = current.totalAttempts + 1
        val timeSum = current.averageTimePerExerciseMs * (attempts - 1) + attempt.timeTakenMs
        val masteredCount = progressMap.values.count { it.isMastered }

        val updated = current.copy(
            totalExercises = state.readWords.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            charactersRead = if (correct) current.charactersRead + exercise.hanzi.length else current.charactersRead,
            wordsRead = state.readWords.size,
            wordsMastered = masteredCount,
            currentStreak = state.currentStreak,
            longestStreak = state.longestStreak,
            lastReadingDate = state.lastReadingDate,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            readingBadges = current.readingBadges,
        )
        return updated
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
        statistics: ReadingStatistics,
    ) {
        if (statistics.totalAttempts > 1) return
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

    private fun progressFor(
        attempt: ReadingAttempt,
        exercise: ReadingExercise,
        progressMap: Map<String, ReadingProgress>,
    ): ReadingProgress {
        val itemId = attempt.wordId ?: exercise.relatedWordId ?: exercise.id
        return progressMap[itemId] ?: ReadingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )
    }

    private fun isNewBestWordRead(state: ReadingState): ReadingState {
        if (state.firstReadingRecorded) return state
        return state.copy(firstReadingRecorded = true)
    }

    private suspend fun recomputeBadges(
        state: ReadingState,
        progressMap: Map<String, ReadingProgress>,
    ): List<ReadingBadge> {
        val streak = state.currentStreak
        val longestStreak = state.longestStreak
        val masteredCount = progressMap.values.count { it.isMastered }
        val now = System.currentTimeMillis()

        return loadBadges().map { badge ->
            val progress = when (badge.id) {
                "read_first" -> if (state.readWords.isNotEmpty()) 1f else 0f
                "read_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                "read_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                "read_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                "read_quick_eye" -> (longestStreak / 10f).coerceIn(0f, 1f)
                "read_accurate" -> (state.correctCount / 20f).coerceIn(0f, 1f)
                "read_dialogue_ready" -> (state.npcExerciseCount / 15f).coerceIn(0f, 1f)
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

    private suspend fun updateBadgePassportEntries(badges: List<ReadingBadge>, state: ReadingState) {
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
        if (newlyEarned.isNotEmpty()) {
            val updated = state.copy(recordedBadgeIds = state.recordedBadgeIds + newlyEarned.map { it.id })
            saveState(updated)
        }
    }

    private fun updateStreak(wasCorrect: Boolean, timestamp: Long, state: ReadingState): Pair<Boolean, ReadingState> {
        if (!wasCorrect) return false to state
        val today = getStartOfDay(timestamp)
        val last = state.lastReadingDate
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> true to state.copy(currentStreak = 1, lastReadingDate = timestamp)
            lastDay == today -> false to state.copy(lastReadingDate = timestamp)
            lastDay == today - DAY_MILLIS -> {
                val newStreak = state.currentStreak + 1
                true to state.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, state.longestStreak),
                    lastReadingDate = timestamp,
                )
            }
            else -> true to state.copy(currentStreak = 1, lastReadingDate = timestamp)
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

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
