package com.sworddao.phoenix.feature.listening.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.data.seed.ListeningSeedData
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.PassportEntry
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus as Status
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
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
class RoomListeningRepository @Inject constructor(
    private val dao: ListeningDao,
    private val vocabularyRepository: VocabularyRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val passportRepository: PassportRepository,
    private val pronunciationRepository: PronunciationRepository,
) : ListeningRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countExercises() == 0) {
                dao.upsertExercises(ListeningSeedData.createInitialExercises().map { it.toEntity() })
            }
            if (dao.getProgressDocOnce() == null) {
                dao.upsertProgressDoc(ListeningProgressDocEntity("all", RoomJson.toJson<Map<String, ListeningProgress>>(emptyMap())))
            }
            if (dao.getStatisticsDocOnce() == null) {
                dao.upsertStatisticsDoc(ListeningStatisticsEntity("all", RoomJson.toJson(ListeningStatistics())))
            }
            if (dao.getBadgesDocOnce() == null) {
                dao.upsertBadgesDoc(ListeningBadgesEntity("all", RoomJson.toJsonList(ListeningBadge.ALL_BADGES)))
            }
            if (dao.getSessionsDocOnce() == null) {
                dao.upsertSessionsDoc(ListeningSessionsEntity("all", null, "[]"))
            }
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(ListeningState().toEntity())
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private suspend fun loadState(): ListeningState =
        dao.getStateDocOnce()?.toDomain() ?: ListeningState()

    private suspend fun saveState(state: ListeningState) {
        dao.upsertStateDoc(state.toEntity())
    }

    private suspend fun loadProgress(): MutableMap<String, ListeningProgress> {
        val doc = dao.getProgressDocOnce() ?: return mutableMapOf()
        return RoomJson.fromJsonOrNull<Map<String, ListeningProgress>>(doc.progressJson)?.toMutableMap()
            ?: mutableMapOf()
    }

    private suspend fun saveProgress(progress: Map<String, ListeningProgress>) {
        dao.upsertProgressDoc(ListeningProgressDocEntity("all", RoomJson.toJson(progress)))
    }

    private suspend fun loadStatistics(): ListeningStatistics {
        val doc = dao.getStatisticsDocOnce()
        return RoomJson.fromJsonOrNull<ListeningStatistics>(doc?.statisticsJson) ?: ListeningStatistics()
    }

    private suspend fun saveStatistics(statistics: ListeningStatistics) {
        dao.upsertStatisticsDoc(ListeningStatisticsEntity("all", RoomJson.toJson(statistics)))
    }

    private suspend fun loadBadges(): MutableList<ListeningBadge> {
        val doc = dao.getBadgesDocOnce()
        return RoomJson.fromJsonOrNull<List<ListeningBadge>>(doc?.badgesJson)?.toMutableList()
            ?: ListeningBadge.ALL_BADGES.toMutableList()
    }

    private suspend fun saveBadges(badges: List<ListeningBadge>) {
        dao.upsertBadgesDoc(ListeningBadgesEntity("all", RoomJson.toJsonList(badges)))
    }

    private suspend fun loadSessions(): Pair<ListeningSession?, List<ListeningSession>> {
        val doc = dao.getSessionsDocOnce() ?: return null to emptyList()
        return doc.activeSessionJson?.let { RoomJson.fromJsonOrNull<ListeningSession>(it) } to
            RoomJson.fromJsonList(doc.completedSessionsJson)
    }

    private suspend fun saveSessions(active: ListeningSession?, completed: List<ListeningSession>) {
        dao.upsertSessionsDoc(
            ListeningSessionsEntity(
                id = "all",
                activeSessionJson = active?.let { RoomJson.toJson(it) },
                completedSessionsJson = RoomJson.toJsonList(completed),
            )
        )
    }

    override fun getAllExercises(): Flow<List<ListeningExercise>> =
        seededFlow { dao.getAllExercises().map { list -> list.map { it.toDomain() } } }

    override fun getExerciseById(exerciseId: String): Flow<ListeningExercise?> =
        seededFlow { dao.getExerciseById(exerciseId).map { it?.toDomain() } }

    override fun getExercisesByType(type: ListeningExerciseType): Flow<List<ListeningExercise>> =
        seededFlow { dao.getExercisesByType(type.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByDifficulty(difficulty: ListeningDifficulty): Flow<List<ListeningExercise>> =
        seededFlow { dao.getExercisesByDifficulty(difficulty.name).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByWord(wordId: String): Flow<List<ListeningExercise>> =
        seededFlow { dao.getExercisesByWord(wordId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByNpc(npcId: String): Flow<List<ListeningExercise>> =
        seededFlow { dao.getExercisesByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override fun getExercisesByQuest(questId: String): Flow<List<ListeningExercise>> =
        seededFlow { dao.getExercisesByQuest(questId).map { list -> list.map { it.toDomain() } } }

    override fun getUnlockedExercises(): Flow<List<ListeningExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() } } }

    override fun getRecommendedExercises(limit: Int): Flow<List<ListeningExercise>> =
        seededFlow { dao.getUnlockedExercises().map { list -> list.map { it.toDomain() }.sortedBy { it.order }.take(limit) } }

    override fun getListeningProgress(itemId: String): Flow<ListeningProgress?> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, ListeningProgress>>(it.progressJson)?.get(itemId) } } }

    override fun getAllListeningProgress(): Flow<List<ListeningProgress>> =
        seededFlow { dao.getProgressDoc().map { doc -> doc?.let { RoomJson.fromJsonOrNull<Map<String, ListeningProgress>>(it.progressJson)?.values?.toList() } ?: emptyList() } }

    override fun getListeningStatistics(): Flow<ListeningStatistics> =
        seededFlow { dao.getStatisticsDoc().map { doc -> RoomJson.fromJsonOrNull<ListeningStatistics>(doc?.statisticsJson) ?: ListeningStatistics() } }

    override fun getListeningBadges(): Flow<List<ListeningBadge>> =
        seededFlow { dao.getBadgesDoc().map { doc -> RoomJson.fromJsonOrNull<List<ListeningBadge>>(doc?.badgesJson) ?: ListeningBadge.ALL_BADGES } }

    override suspend fun startSession(config: ListeningSessionConfig): ListeningSession {
        ensureSeeded()
        val selected = selectExercises(config)
        val session = ListeningSession(
            exerciseIds = selected.map { it.id },
            startedAt = System.currentTimeMillis(),
        )
        val (_, completed) = loadSessions()
        saveSessions(session, completed)
        return session
    }

    override suspend fun submitAnswer(attempt: ListeningAttempt): ListeningResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val session = active ?: return Status.Error("No active listening session")

        val exercise = dao.getExerciseById(attempt.exerciseId).first()?.toDomain()
            ?: return Status.Error("Exercise not found: ${attempt.exerciseId}")

        val correct = attempt.wasCorrect
        var state = loadState()
        val (streakIncremented, updatedState) = updateStreak(correct, attempt.timestamp, state)
        state = updatedState
        val currentStreak = state.currentStreak
        val streakContinued = correct && currentStreak >= 1 && streakIncremented

        val xpEarned = if (correct) {
            val streakBonus = if (currentStreak >= 2) 5 else 0
            exercise.xpReward + streakBonus
        } else 0
        val friendshipBonusEarned = if (correct && exercise.relatedNpcId != null) exercise.friendshipBonus else 0

        val progressMap = loadProgress()
        val existingBestTime = progressFor(attempt, exercise, progressMap).bestTimeMs

        val updatedSession = session.copy(
            attempts = session.attempts + attempt,
            totalXpEarned = session.totalXpEarned + xpEarned,
            totalFriendshipBonus = session.totalFriendshipBonus + friendshipBonusEarned,
        )
        saveSessions(updatedSession, completed)

        val updatedProgressMap = updateListeningProgress(attempt, exercise, progressMap)
        saveProgress(updatedProgressMap)
        attempt.wordId?.let { state = state.copy(practicedWords = state.practicedWords + it) }
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
            attempt.wordId?.let { vocabularyRepository.incrementHeard(it) }
            gameProgressRepository.recordListeningPractice()
            exercise.relatedSpeakingExerciseId?.let { speakingId ->
                val result = pronunciationRepository.unlockExercise(speakingId)
                if (result is PronunciationResultStatus.Error) {
                    // Already unlocked or unknown — no action needed
                }
            }
            if (friendshipBonusEarned > 0 && exercise.relatedNpcId != null) {
                friendshipRepository.addFriendshipXp(exercise.relatedNpcId, friendshipBonusEarned)
            }
            updateListeningQuests()
            recordFirstListeningPassportEntry(exercise, attempt, statistics)
        }

        val badges = recomputeBadges(state, updatedProgressMap)
        saveBadges(badges)
        val newState = recordNewBadgeEntries(badges, state)
        saveState(newState)
        state = newState

        val result = ListeningResult(
            attempt = attempt,
            exercise = exercise,
            isNewPersonalBest = attempt.wasCorrect &&
                (existingBestTime == 0L || attempt.timeTakenMs < existingBestTime),
            streakContinued = streakContinued,
            currentStreak = currentStreak,
            xpEarned = xpEarned,
            friendshipBonusEarned = friendshipBonusEarned,
            badgeProgress = badges.associate { it.id to it.progress },
        )

        return Status.ExerciseCompleted(result)
    }

    override suspend fun completeSession(session: ListeningSession): ListeningResultStatus {
        ensureSeeded()
        val (active, completed) = loadSessions()
        val activeSession = active ?: return Status.Error("No active listening session")
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

    override suspend fun updateProgress(progress: ListeningProgress): ListeningResultStatus {
        ensureSeeded()
        val mastered = progress.copy(masteryLevel = calculateMastery(progress))
        val map = loadProgress()
        map[mastered.itemId] = mastered
        saveProgress(map)
        return Status.ProgressUpdated(mastered)
    }

    override suspend fun unlockExercise(exerciseId: String): ListeningResultStatus {
        ensureSeeded()
        val exercise = dao.getExerciseById(exerciseId).first()
            ?: return Status.Error("Exercise not found: $exerciseId")
        if (exercise.isUnlocked) {
            return Status.Success("Exercise already unlocked")
        }
        dao.unlockExercise(exerciseId)
        return Status.Success("Exercise unlocked: $exerciseId")
    }

    override suspend fun recordStreak(streak: Int): ListeningResultStatus {
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
            lastListeningDate = state.lastListeningDate,
        )
        saveStatistics(statistics)
        return Status.StreakUpdated(state.currentStreak, state.longestStreak)
    }

    override suspend fun awardBadge(badgeId: String): ListeningResultStatus {
        ensureSeeded()
        val definition = ListeningBadge.getBadge(badgeId)
            ?: return Status.Error("Badge not found: $badgeId")
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

    override suspend fun addExercises(exercises: List<ListeningExercise>): ListeningResultStatus {
        ensureSeeded()
        val existingIds = dao.getAllExercises().first().map { it.id }.toSet()
        val newExercises = exercises.filter { it.id !in existingIds }
        if (newExercises.isNotEmpty()) {
            dao.upsertExercises(newExercises.map { it.toEntity() })
        }
        return Status.Success("Added ${newExercises.size} exercises")
    }

    override suspend fun recordReplay(exerciseId: String): ListeningResultStatus {
        ensureSeeded()
        if (dao.getExerciseById(exerciseId).first() == null) {
            return Status.Error("Exercise not found: $exerciseId")
        }
        var state = loadState()
        val newCount = (state.replayCounts[exerciseId] ?: 0) + 1
        state = state.copy(replayCounts = state.replayCounts + (exerciseId to newCount))
        saveState(state)
        var statistics = loadStatistics()
        statistics = statistics.copy(totalReplayCount = statistics.totalReplayCount + 1)
        saveStatistics(statistics)
        return Status.ReplayRecorded(exerciseId, newCount)
    }

    private suspend fun selectExercises(config: ListeningSessionConfig): List<ListeningExercise> {
        val unlocked = dao.getUnlockedExercises().first().map { it.toDomain() }
        var selected: List<ListeningExercise> = emptyList()

        if (config.wordIds.isNotEmpty()) {
            selected = unlocked.filter { it.relatedWordId in config.wordIds || it.clip.wordId in config.wordIds }
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
            selected = unlocked.filter { it.relatedNpcId == config.npcId || it.clip.npcId == config.npcId }
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

    private fun updateListeningProgress(
        attempt: ListeningAttempt,
        exercise: ListeningExercise,
        progressMap: MutableMap<String, ListeningProgress>,
    ): Map<String, ListeningProgress> {
        val itemId = attempt.wordId ?: exercise.clip.id
        val existing = progressMap[itemId]
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

        progressMap[itemId] = updated
        return progressMap
    }

    private suspend fun updateStatistics(
        attempt: ListeningAttempt,
        exercise: ListeningExercise,
        correct: Boolean,
        progressMap: Map<String, ListeningProgress>,
        state: ListeningState,
    ): ListeningStatistics {
        val current = loadStatistics()
        val attempts = current.totalAttempts + 1
        val timeSum = current.totalTimeListenedMs + attempt.timeTakenMs
        val masteredCount = progressMap.values.count { it.isMastered }

        return current.copy(
            totalExercises = state.practicedWords.size,
            totalAttempts = attempts,
            correctAttempts = current.correctAttempts + if (correct) 1 else 0,
            totalTimeListenedMs = timeSum,
            averageTimePerExerciseMs = if (attempts > 0) timeSum / attempts else 0,
            wordsPracticed = state.practicedWords.size,
            wordsMastered = masteredCount,
            currentStreak = state.currentStreak,
            longestStreak = state.longestStreak,
            lastListeningDate = state.lastListeningDate,
            exercisesByType = current.exercisesByType + (exercise.type to (current.exercisesByType[exercise.type] ?: 0) + 1),
            exercisesByDifficulty = current.exercisesByDifficulty +
                (exercise.difficulty to (current.exercisesByDifficulty[exercise.difficulty] ?: 0) + 1),
            listeningBadges = current.listeningBadges,
        )
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
        statistics: ListeningStatistics,
    ) {
        if (statistics.totalAttempts > 1) return
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

    private fun progressFor(
        attempt: ListeningAttempt,
        exercise: ListeningExercise,
        progressMap: Map<String, ListeningProgress>,
    ): ListeningProgress {
        val itemId = attempt.wordId ?: exercise.clip.id
        return progressMap[itemId] ?: ListeningProgress(itemId = itemId)
    }

    private suspend fun recomputeBadges(
        state: ListeningState,
        progressMap: Map<String, ListeningProgress>,
    ): List<ListeningBadge> {
        val streak = state.currentStreak
        val longestStreak = state.longestStreak
        val masteredCount = progressMap.values.count { it.isMastered }
        val now = System.currentTimeMillis()

        return loadBadges().map { badge ->
            val progress = when (badge.id) {
                "listen_first" -> if (state.practicedWords.isNotEmpty()) 1f else 0f
                "listen_streak_3" -> (streak / 3f).coerceIn(0f, 1f)
                "listen_streak_7" -> (streak / 7f).coerceIn(0f, 1f)
                "listen_streak_30" -> (streak / 30f).coerceIn(0f, 1f)
                "listen_quick_ear" -> (longestStreak / 10f).coerceIn(0f, 1f)
                "listen_accurate" -> (state.correctCount / 20f).coerceIn(0f, 1f)
                "listen_npc_ready" -> (state.npcExerciseCount / 15f).coerceIn(0f, 1f)
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

    private suspend fun recordNewBadgeEntries(badges: List<ListeningBadge>, state: ListeningState): ListeningState {
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

    private fun updateStreak(wasCorrect: Boolean, timestamp: Long, state: ListeningState): Pair<Boolean, ListeningState> {
        if (!wasCorrect) return false to state
        val today = getStartOfDay(timestamp)
        val last = state.lastListeningDate
        val lastDay = last?.let { getStartOfDay(it) }

        return when {
            last == null -> true to state.copy(currentStreak = 1, lastListeningDate = timestamp)
            lastDay == today -> false to state.copy(lastListeningDate = timestamp)
            lastDay == today - DAY_MILLIS -> {
                val newStreak = state.currentStreak + 1
                true to state.copy(
                    currentStreak = newStreak,
                    longestStreak = maxOf(newStreak, state.longestStreak),
                    lastListeningDate = timestamp,
                )
            }
            else -> true to state.copy(currentStreak = 1, lastListeningDate = timestamp)
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

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
