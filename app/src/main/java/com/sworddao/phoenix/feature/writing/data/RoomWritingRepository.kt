package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.data.seed.WritingSeedData
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus
import com.sworddao.phoenix.feature.writing.data.WritingResultStatus as Status
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
class RoomWritingRepository @Inject constructor(
    private val dao: WritingDao,
    private val vocabularyRepository: VocabularyRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val passportRepository: PassportRepository,
) : WritingRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countExercises() == 0) {
                dao.upsertExercises(WritingSeedData.createInitialExercises().map { it.toEntity() })
            }
            if (dao.getProgressDocOnce() == null) {
                dao.upsertProgressDoc(WritingProgressDocEntity("all", RoomJson.toJson<Map<String, WritingProgress>>(emptyMap())))
            }
            if (dao.getStatisticsDocOnce() == null) {
                dao.upsertStatisticsDoc(WritingStatisticsEntity("all", RoomJson.toJson(WritingStatistics())))
            }
            if (dao.getBadgesDocOnce() == null) {
                dao.upsertBadgesDoc(WritingBadgesEntity("all", RoomJson.toJsonList(WritingBadge.ALL_BADGES)))
            }
            if (dao.getSessionsDocOnce() == null) {
                dao.upsertSessionsDoc(WritingSessionsEntity("all", null, "[]"))
            }
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(WritingState().toEntity())
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private suspend fun loadState(): WritingState {
        val doc = dao.getStateDocOnce() ?: return WritingState()
        return doc.toDomain(
            writtenCharacters = RoomJson.fromJsonList<String>(doc.writtenCharactersJson).toSet(),
            recordedBadgeIds = RoomJson.fromJsonList<String>(doc.recordedBadgeIdsJson).toSet(),
        )
    }

    private suspend fun saveState(state: WritingState) {
        dao.upsertStateDoc(state.toEntity())
    }

    private suspend fun loadProgress(): MutableMap<String, WritingProgress> {
        val doc = dao.getProgressDocOnce()
            ?: return mutableMapOf()
        return RoomJson.fromJsonOrNull<Map<String, WritingProgress>>(doc.progressJson)?.toMutableMap()
            ?: mutableMapOf()
    }

    private suspend fun saveProgress(progress: Map<String, WritingProgress>) {
        dao.upsertProgressDoc(WritingProgressDocEntity("all", RoomJson.toJson(progress)))
    }

    private suspend fun loadStatistics(): WritingStatistics {
        val doc = dao.getStatisticsDocOnce()
        return RoomJson.fromJsonOrNull<WritingStatistics>(doc?.statisticsJson) ?: WritingStatistics()
    }

    private suspend fun saveStatistics(statistics: WritingStatistics) {
        dao.upsertStatisticsDoc(WritingStatisticsEntity("all", RoomJson.toJson(statistics)))
    }

    private suspend fun loadBadges(): MutableList<WritingBadge> {
        val doc = dao.getBadgesDocOnce()
        return RoomJson.fromJsonOrNull<List<WritingBadge>>(doc?.badgesJson)?.toMutableList()
            ?: WritingBadge.ALL_BADGES.toMutableList()
    }

    private suspend fun saveBadges(badges: List<WritingBadge>) {
        dao.upsertBadgesDoc(WritingBadgesEntity("all", RoomJson.toJsonList(badges)))
    }

    private suspend fun loadSessions(): Pair<WritingSession?, List<WritingSession>> {
        val doc = dao.getSessionsDocOnce() ?: return null to emptyList()
        return doc.activeSessionJson?.let { RoomJson.fromJsonOrNull<WritingSession>(it) } to
            RoomJson.fromJsonList(doc.completedSessionsJson)
    }

    private suspend fun saveSessions(active: WritingSession?, completed: List<WritingSession>) {
        dao.upsertSessionsDoc(
            WritingSessionsEntity(
                id = "all",
                activeSessionJson = active?.let { RoomJson.toJson(it) },
                completedSessionsJson = RoomJson.toJsonList(completed),
            )
        )
    }

    override fun getAllExercises(): Flow<List<WritingExercise>> =
        seededFlow { dao.getAllExercises().map { list -> list.map { it.toDomain() } } }

    override fun getExerciseById(exerciseId: String): Flow<WritingExercise?> =
        seededFlow { dao.getExerciseById(exerciseId).map { it?.toDomain() } }

    override fun getExercisesByType(type: WritingExerciseType): Flow<List<WritingExercise>> =
        seededFlow { dao.getExercisesByType(type.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByDifficulty(difficulty: WritingDifficulty): Flow<List<WritingExercise>> =
        seededFlow { dao.getExercisesByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByWord(wordId: String): Flow<List<WritingExercise>> =
        seededFlow { dao.getExercisesByWord(wordId).map { list -> list.map { it.toDomain() } } }

    override fun getUnlockedExercises(): Flow<List<WritingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() } } }

    override fun getRecommendedExercises(limit: Int): Flow<List<WritingExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() }.sortedBy { it.order }.take(limit) } }

    override fun getWritingProgress(itemId: String): Flow<WritingProgress?> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, WritingProgress>>(it.progressJson)?.get(itemId) } } }

    override fun getAllWritingProgress(): Flow<List<WritingProgress>> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, WritingProgress>>(it.progressJson)?.values?.toList() } ?: emptyList() } }

    override fun getWritingStatistics(): Flow<WritingStatistics> =
        seededFlow { dao.getStatisticsDoc().map { doc -> RoomJson.fromJsonOrNull<WritingStatistics>(doc?.statisticsJson) ?: WritingStatistics() } }

    override fun getWritingBadges(): Flow<List<WritingBadge>> =
        seededFlow { dao.getBadgesDoc().map { doc -> RoomJson.fromJsonOrNull<List<WritingBadge>>(doc?.badgesJson) ?: WritingBadge.ALL_BADGES } }

    override suspend fun startSession(config: WritingSessionConfig): WritingSession {
        ensureSeeded()
        val selected = selectExercises(config)
        val session = WritingSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        val (_, completed) = loadSessions()
        saveSessions(session, completed)
        return session
    }

    override suspend fun submitAnswer(attempt: WritingAttempt): WritingResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val session = active ?: return Status.Error("No active writing session")

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
        val friendshipBonusEarned = if (correct && exercise.character.wordId?.startsWith("greet_") == true) {
            exercise.friendshipBonus
        } else 0

        val progressMap = loadProgress()
        val existingBestTime = progressFor(attempt, exercise, progressMap).bestTimeMs

        val updatedSession = session.copy(
            attempts = session.attempts + attempt,
            totalXpEarned = session.totalXpEarned + xpEarned,
            totalFriendshipBonus = session.totalFriendshipBonus + friendshipBonusEarned,
            totalCorrectStrokes = session.totalCorrectStrokes + attempt.correctStrokeCount,
        )
        saveSessions(updatedSession, completed)

        val updatedProgressMap = updateWritingProgress(attempt, exercise, progressMap)
        saveProgress(updatedProgressMap)
        attempt.wordId?.let { state = state.copy(writtenCharacters = state.writtenCharacters + it) }
        var statistics = updateStatistics(attempt, exercise, correct, updatedProgressMap, state)
        saveStatistics(statistics)
        saveState(state)

        if (correct) {
            attempt.wordId?.let { vocabularyRepository.incrementWritten(it) }
            gameProgressRepository.recordWritingPractice()
            if (friendshipBonusEarned > 0 && exercise.character.wordId?.startsWith("greet_") == true) {
                friendshipRepository.addFriendshipXp("grandma_mei", friendshipBonusEarned)
            }
            updateWritingQuests()
            recordFirstWritingPassportEntry(exercise, attempt, statistics)
        }

        val badges = recomputeBadges(state, updatedProgressMap)
        saveBadges(badges)
        updateBadgePassportEntries(badges, state)
        saveState(state)

        val newMastery = progressFor(attempt, exercise, updatedProgressMap).masteryLevel
        val isNewPersonalBest = attempt.wasCorrect &&
            (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime)
        val wasFirstWriting = correct && !state.firstWritingRecorded
        if (wasFirstWriting) {
            state = state.copy(firstWritingRecorded = true)
        }

        val result = WritingResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = isNewPersonalBest,
            streakContinued = streakContinued,
            currentStreak = currentStreakValue,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            reward = WritingReward(
                xpEarned = xpEarned,
                friendshipBonusEarned = friendshipBonusEarned,
                streakContinued = streakContinued,
                isFirstCharacterWritten = wasFirstWriting,
                newMastery = if (correct) newMastery else null,
                isNewPersonalBest = isNewPersonalBest,
                badgeProgress = badges.associate { it.id to it.progress },
            ),
            badgeProgress = badges.associate { it.id to it.progress },
        )
        saveState(state)

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: WritingSession): WritingResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val activeSession = active ?: return Status.Error("No active writing session")
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

    override suspend fun updateProgress(progress: WritingProgress): WritingResultStatus {
        ensureSeeded()
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        val map = loadProgress()
        map[mastered.itemId] = mastered
        saveProgress(map)
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): WritingResultStatus {
        ensureSeeded()
        val exercise = dao.getExerciseById(exerciseId).first()
            ?: return Status.Error("Exercise not found: $exerciseId")
        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }
        dao.unlockExercise(exerciseId)
        return Status.Success("Writing exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): WritingResultStatus {
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
            lastWritingDate = state.lastWritingDate,
        )
        saveStatistics(statistics)
        return Status.StreakUpdated(state.currentStreak, state.longestStreak)
    }

    override suspend fun awardBadge(badgeId: String): WritingResultStatus {
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

    override suspend fun addExercises(exercises: List<WritingExercise>): WritingResultStatus {
        ensureSeeded()
        val existingIds = dao.getAllExercises().first().map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        if (newExercises.isNotEmpty()) {
            dao.upsertExercises(newExercises.map { it.toEntity() })
        }
        return Status.Success("Added ${newExercises.size} writing exercises")
    }

    private suspend fun selectExercises(config: WritingSessionConfig): List<WritingExercise> {
        val unlocked = dao.getUnlockedExercises().first().map { it.toDomain() }
        var selected: List<WritingExercise> = emptyList()

        if (config.characterIds.isNotEmpty()) {
            selected = unlocked.filter { it.character.id in config.characterIds }
            if (selected.isEmpty()) {
                val dynamic = config.characterIds.mapNotNull { characterId ->
                    val word = vocabularyRepository.getWordById(characterId).first()
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
        } else {
            selected = unlocked.filter { it.type == config.exerciseType && it.difficulty == config.difficulty }
            if (selected.isEmpty()) {
                selected = unlocked.filter { it.type == config.exerciseType }
            }
        }

        return selected.take(config.exerciseCount).ifEmpty { unlocked.take(config.exerciseCount) }
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

    private fun updateWritingProgress(
        attempt: WritingAttempt,
        exercise: WritingExercise,
        progressMap: MutableMap<String, WritingProgress>,
    ): Map<String, WritingProgress> {
        val itemId = attempt.wordId ?: exercise.id
        val existing = progressMap[itemId]
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

        progressMap[itemId] = updated
        return progressMap
    }

    private suspend fun updateStatistics(
        attempt: WritingAttempt,
        exercise: WritingExercise,
        correct: Boolean,
        progressMap: Map<String, WritingProgress>,
        state: WritingState,
    ): WritingStatistics {
        val current = loadStatistics()
        val attempts = current.totalAttempts + 1
        val timeSum = current.averageTimePerExerciseMs * (attempts - 1) + attempt.timeTakenMs
        val masteredCount = progressMap.values.count { it.isMastered }

        val updated = current.copy(
            totalExercises = state.writtenCharacters.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            totalStrokes = current.totalStrokes + attempt.totalStrokeCount,
            correctStrokes = current.correctStrokes + attempt.correctStrokeCount,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            charactersWritten = if (correct) current.charactersWritten + 1 else current.charactersWritten,
            charactersMastered = masteredCount,
            currentStreak = state.currentStreak,
            longestStreak = state.longestStreak,
            lastWritingDate = state.lastWritingDate,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            writingBadges = current.writingBadges,
        )
        return updated
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
        statistics: WritingStatistics,
    ) {
        if (statistics.totalAttempts > 1) return
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

    private fun progressFor(
        attempt: WritingAttempt,
        exercise: WritingExercise,
        progressMap: Map<String, WritingProgress>,
    ): WritingProgress {
        val itemId = attempt.wordId ?: exercise.character.wordId ?: exercise.id
        return progressMap[itemId] ?: WritingProgress(
            itemId = itemId,
            wordId = attempt.wordId,
            hanzi = exercise.hanzi,
        )
    }

    private suspend fun recomputeBadges(
        state: WritingState,
        progressMap: Map<String, WritingProgress>,
    ): List<WritingBadge> {
        val streak = state.currentStreak
        val longestStreak = state.longestStreak
        val masteredCount = progressMap.values.count { it.isMastered }
        val now = System.currentTimeMillis()

        return loadBadges().map { badge ->
            val progress = when (badge.id) {
                "write_first" -> if (state.writtenCharacters.isNotEmpty()) 1f else 0f
                "write_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                "write_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                "write_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                "write_steady_hand" -> (longestStreak / 10f).coerceIn(0f, 1f)
                "write_stroke_perfect" -> (state.correctCount / 20f).coerceIn(0f, 1f)
                "write_dialogue_ready" -> (state.correctCount / 15f).coerceIn(0f, 1f)
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

    private suspend fun updateBadgePassportEntries(badges: List<WritingBadge>, state: WritingState) {
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

    private fun updateStreak(wasCorrect: Boolean, timestamp: Long, state: WritingState): Pair<Boolean, WritingState> {
        if (!wasCorrect) return false to state
        val today = getStartOfDay(timestamp)
        val last = state.lastWritingDate
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> true to state.copy(currentStreak = 1, lastWritingDate = timestamp)
            lastDay == today -> false to state.copy(lastWritingDate = timestamp)
            lastDay == today - DAY_MILLIS -> {
                val newStreak = state.currentStreak + 1
                true to state.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, state.longestStreak),
                    lastWritingDate = timestamp,
                )
            }
            else -> true to state.copy(currentStreak = 1, lastWritingDate = timestamp)
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

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
