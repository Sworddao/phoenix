package com.sworddao.phoenix.feature.review.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import com.sworddao.phoenix.feature.progression.data.XpSource
import com.sworddao.phoenix.feature.progression.domain.ProgressionRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.data.QuestStats
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.review.domain.ReviewRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import com.sworddao.phoenix.feature.world.domain.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReviewRepository @Inject constructor(
    private val dao: ReviewDao,
    private val vocabularyRepository: VocabularyRepository,
    private val gameProgressRepository: GameProgressRepository,
    private val questRepository: QuestRepository,
    private val friendshipRepository: FriendshipRepository,
    private val worldRepository: WorldRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val listeningRepository: ListeningRepository,
    private val readingRepository: ReadingRepository,
    private val progressionRepository: ProgressionRepository,
) : ReviewRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.getItemsDocOnce() == null) {
                dao.upsertItemsDoc(ReviewItemsEntity("all", "[]"))
            }
            if (dao.getMemoryDocOnce() == null) {
                dao.upsertMemoryDoc(ReviewMemoryEntity("all", RoomJson.toJson<Map<String, MemoryStrength>>(emptyMap())))
            }
            if (dao.getSchedulesDocOnce() == null) {
                dao.upsertSchedulesDoc(ReviewSchedulesEntity("all", RoomJson.toJson<Map<String, ReviewSchedule>>(emptyMap())))
            }
            if (dao.getSessionsDocOnce() == null) {
                dao.upsertSessionsDoc(ReviewSessionsEntity("all", RoomJson.toJson<Map<String, ReviewSession>>(emptyMap())))
            }
            if (dao.getHistoryDocOnce() == null) {
                dao.upsertHistoryDoc(ReviewHistoryEntity("all", "[]"))
            }
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(ReviewState(todayDate = today()).toEntity())
            }
            if (dao.getSnapshotDocOnce() == null) {
                dao.upsertSnapshotDoc(ReviewSnapshotEntity("all", null))
            }
            if (dao.getStatsDocOnce() == null) {
                dao.upsertStatsDoc(ReviewStatsEntity("all", RoomJson.toJson(ReviewStatistics())))
            }
            if (dao.getPublishedDocOnce() == null) {
                dao.upsertPublishedDoc(ReviewPublishedEntity("all", "[]", "[]", "[]", RoomJson.toJson(DailyReview()), "[]"))
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    private suspend fun loadItems(): MutableList<ReviewItem> =
        RoomJson.fromJsonList<ReviewItem>(dao.getItemsDocOnce()?.itemsJson).toMutableList()

    private suspend fun saveItems(items: List<ReviewItem>) {
        dao.upsertItemsDoc(ReviewItemsEntity("all", RoomJson.toJsonList(items)))
    }

    private suspend fun loadMemory(): MutableMap<String, MemoryStrength> =
        RoomJson.fromJsonOrNull<Map<String, MemoryStrength>>(dao.getMemoryDocOnce()?.memoryJson)?.toMutableMap()
            ?: mutableMapOf()

    private suspend fun saveMemory(memory: Map<String, MemoryStrength>) {
        dao.upsertMemoryDoc(ReviewMemoryEntity("all", RoomJson.toJson(memory)))
    }

    private suspend fun loadSchedules(): MutableMap<String, ReviewSchedule> =
        RoomJson.fromJsonOrNull<Map<String, ReviewSchedule>>(dao.getSchedulesDocOnce()?.schedulesJson)?.toMutableMap()
            ?: mutableMapOf()

    private suspend fun saveSchedules(schedules: Map<String, ReviewSchedule>) {
        dao.upsertSchedulesDoc(ReviewSchedulesEntity("all", RoomJson.toJson(schedules)))
    }

    private suspend fun loadSessions(): MutableMap<String, ReviewSession> =
        RoomJson.fromJsonOrNull<Map<String, ReviewSession>>(dao.getSessionsDocOnce()?.sessionsJson)?.toMutableMap()
            ?: mutableMapOf()

    private suspend fun saveSessions(sessions: Map<String, ReviewSession>) {
        dao.upsertSessionsDoc(ReviewSessionsEntity("all", RoomJson.toJson(sessions)))
    }

    private suspend fun loadHistory(): MutableList<ReviewHistoryEntry> =
        RoomJson.fromJsonList<ReviewHistoryEntry>(dao.getHistoryDocOnce()?.historyJson).toMutableList()

    private suspend fun saveHistory(history: List<ReviewHistoryEntry>) {
        dao.upsertHistoryDoc(ReviewHistoryEntity("all", RoomJson.toJsonList(history)))
    }

    private suspend fun loadState(): ReviewState =
        dao.getStateDocOnce()?.toDomain() ?: ReviewState(todayDate = today())

    private suspend fun saveState(state: ReviewState) {
        dao.upsertStateDoc(state.toEntity())
    }

    private suspend fun loadSnapshot(): ReviewSourceSnapshot? =
        RoomJson.fromJsonOrNull(dao.getSnapshotDocOnce()?.snapshotJson)

    private suspend fun saveSnapshot(snapshot: ReviewSourceSnapshot?) {
        dao.upsertSnapshotDoc(ReviewSnapshotEntity("all", snapshot?.let { RoomJson.toJson(it) }))
    }

    private suspend fun loadStats(): ReviewStatistics =
        RoomJson.fromJsonOrNull(dao.getStatsDocOnce()?.statsJson) ?: ReviewStatistics()

    private suspend fun saveStats(stats: ReviewStatistics) {
        dao.upsertStatsDoc(ReviewStatsEntity("all", RoomJson.toJson(stats)))
    }

    override fun getTodayReviews(): Flow<List<ReviewItem>> =
        seededFlow { dao.getPublishedDoc().map { doc -> RoomJson.fromJsonList(doc?.todayJson) } }

    override fun getUpcomingReviews(): Flow<List<ReviewItem>> =
        seededFlow { dao.getPublishedDoc().map { doc -> RoomJson.fromJsonList(doc?.upcomingJson) } }

    override fun getReviewHistory(): Flow<List<ReviewHistoryEntry>> =
        seededFlow { dao.getHistoryDoc().map { doc -> RoomJson.fromJsonList(doc?.historyJson) } }

    override fun getReviewStatistics(): Flow<ReviewStatistics> =
        seededFlow { dao.getStatsDoc().map { doc -> RoomJson.fromJsonOrNull<ReviewStatistics>(doc?.statsJson) ?: ReviewStatistics() } }

    override fun getRecommendations(): Flow<List<ReviewRecommendation>> =
        seededFlow { dao.getPublishedDoc().map { doc -> RoomJson.fromJsonList(doc?.recommendationsJson) } }

    override fun getDailyReview(): Flow<DailyReview> =
        seededFlow { dao.getPublishedDoc().map { doc -> RoomJson.fromJsonOrNull<DailyReview>(doc?.dailyJson) ?: DailyReview() } }

    override fun getMemoryStrengths(): Flow<List<MemoryStrength>> =
        seededFlow { dao.getPublishedDoc().map { doc -> RoomJson.fromJsonList(doc?.memoryStrengthsJson) } }

    // ------------------------------------------------------------------
    // Refresh: seed words + schedule from source deltas
    // ------------------------------------------------------------------

    override suspend fun refresh(): ReviewResult {
        ensureSeeded()
        val gameProgress = gameProgressRepository.getGameProgress().first()
        val discoveredWords = vocabularyRepository.getDiscoveredWords().first()
        val questStats = questRepository.getQuestStats().first()
        val friendshipStates = friendshipRepository.getAllFriendshipStates().first()
        val regions = worldRepository.getAllRegions().first()
        val speakingStats = pronunciationRepository.getSpeakingStatistics().first()
        val listeningStats = listeningRepository.getListeningStatistics().first()
        val readingStats = readingRepository.getReadingStatistics().first()

        seedWordEntries(discoveredWords)

        val snapshot = buildSnapshot(
            gameProgress = gameProgress,
            questStats = questStats,
            friendshipStates = friendshipStates,
            regions = regions,
            speakingStats = speakingStats,
            listeningStats = listeningStats,
            readingStats = readingStats,
        )

        val previous = loadSnapshot()
        if (previous != null) {
            scheduleFromDeltas(snapshot, previous, discoveredWords)
        }
        saveSnapshot(snapshot)

        publishAll()
        return ReviewResult.Refreshed(getTodayReviews().first().size)
    }

    private suspend fun seedWordEntries(words: List<VocabularyWord>) {
        val existing = loadMemory()
        val newEntries = words.filter { it.id !in existing }
        if (newEntries.isEmpty()) return

        val now = System.currentTimeMillis()
        val seeded = mutableMapOf<String, MemoryStrength>()
        val seededItems = mutableListOf<ReviewItem>()
        newEntries.forEach { word ->
            val entry = initialMemory(word)
            seeded[word.id] = entry
            if (needsInitialReview(word)) {
                seededItems += buildWordItem(word, entry, ReviewType.MIXED, ReviewSource.VOCABULARY, now)
            }
        }
        val memory = loadMemory() + seeded
        saveMemory(memory)
        val schedules = loadSchedules()
        val items = loadItems()
        seededItems.forEach { item ->
            schedules[item.wordId!!] = item.schedule
            items += item
        }
        saveSchedules(schedules)
        saveItems(items)
    }

    private fun initialMemory(word: VocabularyWord): MemoryStrength {
        val baseStrength = when (word.mastery) {
            VocabularyMastery.MASTERED -> 0.85f
            VocabularyMastery.FAMILIAR -> 0.65f
            VocabularyMastery.LEARNING -> 0.5f
            else -> 0.4f
        }
        return MemoryStrength(
            strength = baseStrength,
            confidence = baseStrength,
            correctAnswers = (word.timesReviewed * 0.8f).toInt(),
            averageScore = if (word.timesReviewed > 0) 0.8f else 0f,
            reviewCount = word.timesReviewed,
        )
    }

    private fun needsInitialReview(word: VocabularyWord): Boolean =
        word.mastery == VocabularyMastery.UNKNOWN ||
            word.mastery == VocabularyMastery.SEEN ||
            word.mastery == VocabularyMastery.LEARNING

    private suspend fun scheduleFromDeltas(
        snapshot: ReviewSourceSnapshot,
        previous: ReviewSourceSnapshot,
        discoveredWords: List<VocabularyWord>,
    ) {
        val now = System.currentTimeMillis()
        val items = loadItems()

        if (snapshot.wordsDiscovered > previous.wordsDiscovered) {
            val delta = snapshot.wordsDiscovered - previous.wordsDiscovered
            discoveredWords.takeLast(delta).forEach { word ->
                val entry = loadMemory()[word.id]
                if (entry != null) {
                    items += buildWordItem(word, entry, ReviewType.MIXED, ReviewSource.VOCABULARY, now)
                }
            }
        }

        val dialogueDelta = snapshot.dialogues - previous.dialogues
        for (i in 0 until dialogueDelta) {
            addSourceItem(ReviewType.CONVERSATION, ReviewSource.DIALOGUE, now)
        }

        val speakingDelta = snapshot.speakingPractices - previous.speakingPractices
        for (i in 0 until speakingDelta) {
            addSourceItem(ReviewType.SPEAKING, ReviewSource.SPEAKING, now)
        }

        val listeningDelta = snapshot.listeningPractices - previous.listeningPractices
        for (i in 0 until listeningDelta) {
            addSourceItem(ReviewType.LISTENING, ReviewSource.LISTENING, now)
        }

        val readingDelta = snapshot.readingPractices - previous.readingPractices
        for (i in 0 until readingDelta) {
            addSourceItem(ReviewType.READING, ReviewSource.READING, now)
        }

        val writingDelta = snapshot.writingPractices - previous.writingPractices
        for (i in 0 until writingDelta) {
            addSourceItem(ReviewType.WRITING, ReviewSource.WRITING, now)
        }

        val questDelta = snapshot.questsCompleted - previous.questsCompleted
        if (questDelta > 0) {
            addQuestReviewItem(now)
        }

        val friendshipDelta = snapshot.friendshipLevels - previous.friendshipLevels
        if (friendshipDelta > 0) {
            addNpcChallengeItem(now)
        }

        val stampDelta = snapshot.passportStamps - previous.passportStamps
        if (stampDelta > 0) {
            addSourceItem(ReviewType.MIXED, ReviewSource.EXPLORATION, now)
        }

        val regionDelta = snapshot.regionsUnlocked - previous.regionsUnlocked
        if (regionDelta > 0) {
            addSourceItem(ReviewType.MIXED, ReviewSource.EXPLORATION, now)
        }
    }

    private suspend fun buildWordItem(
        word: VocabularyWord,
        memory: MemoryStrength,
        type: ReviewType,
        source: ReviewSource,
        now: Long,
    ): ReviewItem {
        val id = nextItemId()
        val schedule = ReviewSchedule(
            itemId = id,
            wordId = word.id,
            stage = 0,
            intervalMillis = SpacedRepetitionEngine.intervalForStage(0),
            dueAt = now,
        )
        return ReviewItem(
            id = id,
            source = source,
            type = type,
            prompt = word.displayHanzi,
            detail = "${word.pinyin} · ${word.english}",
            wordId = word.id,
            hanzi = word.displayHanzi,
            pinyin = word.pinyin,
            relatedNpcId = word.relatedNpcId,
            relatedQuestId = word.relatedQuestId,
            memoryStrength = memory.strength,
            schedule = schedule,
        )
    }

    private suspend fun addSourceItem(type: ReviewType, source: ReviewSource, now: Long) {
        val wordId = weakestWordId()
        val word = wordId?.let { id ->
            vocabularyRepository.getWordById(id).first()
        }
        if (word == null) return
        val memory = loadMemory()[wordId] ?: MemoryStrength()
        val item = buildWordItem(word, memory, type, source, now).copy(
            prompt = word.pinyin,
            detail = word.english,
            schedule = itemSchedule(wordId, now),
        )
        saveItems(loadItems() + item)
    }

    private suspend fun addQuestReviewItem(now: Long) {
        val completed = questRepository.getCompletedQuests().first()
        val quest = completed.lastOrNull() ?: return
        val wordId = weakestWordId()
        if (wordId == null) {
            addGenericItem(ReviewType.QUEST_REVIEW, ReviewSource.QUEST, quest.title, now)
            return
        }
        val memory = loadMemory()[wordId] ?: MemoryStrength()
        val word = vocabularyRepository.getWordById(wordId).first() ?: return
        val item = buildWordItem(
            word = word,
            memory = memory,
            type = ReviewType.QUEST_REVIEW,
            source = ReviewSource.QUEST,
            now = now,
        ).copy(
            detail = "任务：「${quest.title}」",
            relatedQuestId = quest.id,
            schedule = itemSchedule(wordId, now),
        )
        saveItems(loadItems() + item)
    }

    private suspend fun addNpcChallengeItem(now: Long) {
        val bestNpc = friendshipRepository.getAllFriendshipStates().first()
            .maxByOrNull { it.friendshipLevel.level }
        if (bestNpc == null) return
        val wordId = weakestWordId()
        if (wordId == null) {
            addGenericItem(ReviewType.NPC_CHALLENGE, ReviewSource.FRIENDSHIP, bestNpc.npcId, now)
            return
        }
        val memory = loadMemory()[wordId] ?: MemoryStrength()
        val word = vocabularyRepository.getWordById(wordId).first() ?: return
        val item = buildWordItem(
            word = word,
            memory = memory,
            type = ReviewType.NPC_CHALLENGE,
            source = ReviewSource.NPC_CONVERSATION,
            now = now,
        ).copy(
            detail = "来自 ${bestNpc.npcId} 的挑战",
            relatedNpcId = bestNpc.npcId,
            schedule = itemSchedule(wordId, now),
        )
        saveItems(loadItems() + item)
    }

    private suspend fun addGenericItem(type: ReviewType, source: ReviewSource, title: String, now: Long) {
        val id = nextItemId()
        saveItems(loadItems() + ReviewItem(
            id = id,
            source = source,
            type = type,
            prompt = title,
            detail = "点击复习",
            memoryStrength = 0.4f,
            schedule = ReviewSchedule(itemId = id, stage = 0, dueAt = now),
        ))
    }

    private suspend fun itemSchedule(wordId: String, now: Long): ReviewSchedule =
        loadSchedules()[wordId] ?: ReviewSchedule(
            itemId = nextItemId(),
            wordId = wordId,
            stage = 0,
            intervalMillis = SpacedRepetitionEngine.intervalForStage(0),
            dueAt = now,
        )

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    override suspend fun startSession(type: ReviewType): ReviewResult {
        ensureSeeded()
        val due = getTodayReviews().first()
        val items = when (type) {
            ReviewType.DAILY_REVIEW, ReviewType.MIXED -> due.take(MAX_SESSION_ITEMS)
            else -> due.filter { it.type == type }
                .ifEmpty { due.take(MAX_SESSION_ITEMS) }
        }
        if (items.isEmpty()) {
            return ReviewResult.Error("今日暂无待复习内容")
        }
        var state = loadState()
        val session = ReviewSession(
            id = "session_${state.sessionIdCounter}",
            type = type,
            items = items,
            totalCount = items.size,
        )
        state = state.copy(sessionIdCounter = state.sessionIdCounter + 1)
        saveState(state)
        val sessions = loadSessions()
        sessions[session.id] = session
        saveSessions(sessions)
        var stats = loadStats()
        stats = stats.copy(totalSessions = stats.totalSessions + 1)
        saveStats(stats)
        return ReviewResult.SessionStarted(session)
    }

    override suspend fun completeSession(sessionId: String): ReviewResult {
        ensureSeeded()
        val sessions = loadSessions()
        val session = sessions[sessionId] ?: return ReviewResult.Error("Session not found")
        if (session.isCompleted) return ReviewResult.Error("Session already completed")

        val completed = session.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )
        sessions[sessionId] = completed
        saveSessions(sessions)

        var stats = loadStats()
        stats = stats.copy(
            completedSessions = stats.completedSessions + 1,
            xpEarned = stats.xpEarned + REVIEW_XP,
        )
        saveStats(stats)
        var state = loadState()
        state = state.copy(xpEarnedTotal = state.xpEarnedTotal + REVIEW_XP)
        saveState(state)
        progressionRepository.awardXp(XpSource.REVIEW, 1)

        publishAll()
        return ReviewResult.SessionCompleted(completed, REVIEW_XP, completed.accuracy)
    }

    // ------------------------------------------------------------------
    // Answering
    // ------------------------------------------------------------------

    override suspend fun submitAnswer(itemId: String, correct: Boolean, score: Float): ReviewResult {
        ensureSeeded()
        val items = loadItems()
        val item = items.find { it.id == itemId }
            ?: return ReviewResult.Error("Review item not found")

        val wordId = item.wordId
        val memory = wordId?.let { loadMemory()[it] }
        val schedule = wordId?.let { loadSchedules()[it] } ?: item.schedule

        val now = System.currentTimeMillis()
        var strengthAfter = memory?.strength ?: 0f
        var nextReviewAt = now + SpacedRepetitionEngine.intervalForStage(0)
        var interval = SpacedRepetitionEngine.intervalForStage(0)
        var difficulty = SpacedRepetitionEngine.difficultyFor(strengthAfter)
        var newStage = schedule.stage

        if (memory != null) {
            val adjusted = SpacedRepetitionEngine.adjustMemory(memory, correct, score)
            val practiceApplied = SpacedRepetitionEngine.withPractice(
                current = adjusted,
                mode = item.type,
                accuracy = if (correct) score else 0f,
            )
            newStage = SpacedRepetitionEngine.nextStage(
                correct = correct,
                score = score,
                currentStage = schedule.stage,
                consecutiveFailures = memory.consecutiveFailures,
            )
            interval = SpacedRepetitionEngine.intervalForStage(newStage)
            nextReviewAt = now + interval
            strengthAfter = practiceApplied.strength
            difficulty = SpacedRepetitionEngine.difficultyFor(strengthAfter)

            val finalMemory = practiceApplied.copy(
                lastReviewAt = now,
                nextReviewAt = nextReviewAt,
            )
            val updatedMemory = loadMemory() + (wordId to finalMemory)
            saveMemory(updatedMemory)
            val updatedSchedules = loadSchedules() + (wordId to schedule.copy(
                stage = newStage,
                intervalMillis = interval,
                dueAt = nextReviewAt,
                lastReviewedAt = now,
            ))
            saveSchedules(updatedSchedules)
            vocabularyRepository.incrementReview(wordId)
        }

        var state = loadState()
        val historyEntry = ReviewHistoryEntry(
            id = "history_${state.historyIdCounter}",
            itemId = itemId,
            wordId = wordId,
            reviewedAt = now,
            correct = correct,
            score = score.coerceIn(0f, 1f),
            intervalMillis = interval,
            type = item.type,
            strengthAfter = strengthAfter,
        )
        state = state.copy(historyIdCounter = state.historyIdCounter + 1)
        saveHistory((listOf(historyEntry) + loadHistory()).take(MAX_HISTORY))

        saveItems(loadItems().map { existing ->
            if (existing.id == itemId) {
                existing.copy(
                    memoryStrength = if (wordId == null) existing.memoryStrength else strengthAfter,
                    schedule = existing.schedule.copy(
                        stage = newStage,
                        intervalMillis = interval,
                        dueAt = nextReviewAt,
                        lastReviewedAt = now,
                    ),
                )
            } else {
                existing
            }
        })

        state = if (wordId != null) {
            state.copy(reviewedWordIds = state.reviewedWordIds + wordId)
        } else state
        saveState(state)

        updateStatistics(item, correct, score)
        recordDailyActivity(correct)

        val sessions = loadSessions()
        val updatedSessions = sessions.mapValues { (_, session) ->
            if (!session.isCompleted && session.items.any { it.id == itemId }) {
                session.copy(
                    correctCount = session.correctCount + if (correct) 1 else 0,
                    incorrectCount = session.incorrectCount + if (correct) 0 else 1,
                )
            } else {
                session
            }
        }
        saveSessions(updatedSessions)

        publishAll()
        return ReviewResult.Answered(
            itemId = itemId,
            wordId = wordId,
            correct = correct,
            score = score.coerceIn(0f, 1f),
            strengthAfter = strengthAfter,
            intervalMillis = interval,
            nextReviewAt = nextReviewAt,
            difficulty = difficulty,
        )
    }

    // ------------------------------------------------------------------
    // Statistics & daily tracking
    // ------------------------------------------------------------------

    private suspend fun updateStatistics(item: ReviewItem, correct: Boolean, score: Float) {
        val stats = loadStats()
        val byType = stats.byType.toMutableMap()
        byType[item.type] = (byType[item.type] ?: 0) + 1
        val state = loadState()
        val wordsMastered = loadMemory().values.count { it.strength >= SpacedRepetitionEngine.MASTERY_THRESHOLD }
        saveStats(stats.copy(
            totalReviews = stats.totalReviews + 1,
            correctReviews = stats.correctReviews + if (correct) 1 else 0,
            incorrectReviews = stats.incorrectReviews + if (correct) 0 else 1,
            totalScore = stats.totalScore + score.coerceIn(0f, 1f),
            wordsReviewed = state.reviewedWordIds.size,
            wordsMastered = wordsMastered,
            byType = byType,
        ))
    }

    private suspend fun recordDailyActivity(correct: Boolean) {
        val now = LocalDate.now()
        var state = loadState()
        if (now.toString() != state.todayDate) {
            var streak = state.currentStreakDays
            streak = if (state.reviewsToday >= DailyReview.DAILY_GOAL) streak + 1 else 0
            val longest = maxOf(streak, state.longestStreakDays)
            state = state.copy(
                todayDate = now.toString(),
                reviewsToday = 0,
                currentStreakDays = streak,
                longestStreakDays = longest,
            )
        }
        state = state.copy(reviewsToday = state.reviewsToday + 1)
        saveState(state)
    }

    // ------------------------------------------------------------------
    // Publishing derived flows
    // ------------------------------------------------------------------

    private suspend fun publishAll() {
        val now = System.currentTimeMillis()
        val allItems = loadItems()
        val due = allItems
            .filter { it.schedule.dueAt <= now }
            .sortedWith(compareByDescending<ReviewItem> { it.priority }.thenBy { it.schedule.dueAt })
            .take(MAX_TODAY_ITEMS)
        val upcoming = allItems
            .filter { it.schedule.dueAt > now }
            .sortedBy { it.schedule.dueAt }
            .take(MAX_UPCOMING)

        val allMemory = loadMemory().values
        val state = loadState()
        val stats = loadStats()

        val dailyReview = DailyReview(
            date = state.todayDate,
            dueCount = due.size,
            completedCount = state.reviewsToday,
            dailyGoal = DailyReview.DAILY_GOAL,
            weakestWords = allMemory.sortedBy { it.strength }.take(3),
            bestWords = allMemory.sortedByDescending { it.strength }.take(3),
        )

        saveStats(stats.copy(
            currentStreakDays = state.currentStreakDays,
            longestStreakDays = state.longestStreakDays,
            xpEarned = state.xpEarnedTotal,
            wordsMastered = allMemory.count { it.strength >= SpacedRepetitionEngine.MASTERY_THRESHOLD },
        ))

        val recommendations = buildRecommendations(due, allMemory)

        dao.upsertPublishedDoc(ReviewPublishedEntity(
            id = "all",
            todayJson = RoomJson.toJsonList(due),
            upcomingJson = RoomJson.toJsonList(upcoming),
            recommendationsJson = RoomJson.toJsonList(recommendations),
            dailyJson = RoomJson.toJson(dailyReview),
            memoryStrengthsJson = RoomJson.toJsonList(allMemory.sortedBy { it.strength }),
        ))
    }

    private fun buildRecommendations(
        due: List<ReviewItem>,
        allMemory: Collection<MemoryStrength>,
    ): List<ReviewRecommendation> {
        val recommendations = mutableListOf<ReviewRecommendation>()
        if (due.isNotEmpty()) {
            recommendations += ReviewRecommendation(
                id = "rec_daily",
                title = "今日有 ${due.size} 个复习",
                description = "完成今日复习，保持记忆稳固",
                type = ReviewType.DAILY_REVIEW,
                priority = 0.9f,
                icon = "📅",
            )
        }
        val maxFailures = allMemory.maxOfOrNull { it.consecutiveFailures } ?: 0
        if (maxFailures >= 2) {
            recommendations += ReviewRecommendation(
                id = "rec_failures",
                title = "有词汇连续答错",
                description = "尽快复习容易遗忘的词汇",
                type = ReviewType.MIXED,
                priority = 0.85f,
                icon = "⚠️",
            )
        }
        allMemory.sortedBy { it.strength }.take(3).forEachIndexed { index, memory ->
            if (memory.strength < SpacedRepetitionEngine.MASTERY_THRESHOLD) {
                recommendations += ReviewRecommendation(
                    id = "rec_weak_$index",
                    title = "巩固薄弱词汇",
                    description = "记忆强度 ${(memory.strength * 100).toInt()}%，建议优先复习",
                    type = ReviewType.MIXED,
                    priority = 0.6f - index * 0.1f,
                    icon = "🎯",
                )
            }
        }
        val mastered = allMemory.count { it.strength >= SpacedRepetitionEngine.MASTERY_THRESHOLD }
        if (mastered > 0) {
            recommendations += ReviewRecommendation(
                id = "rec_mastered",
                title = "已掌握 $mastered 个词汇",
                description = "继续保持这个节奏！",
                type = ReviewType.DAILY_REVIEW,
                priority = 0.4f,
                icon = "🏆",
            )
        }
        recommendations += ReviewRecommendation(
            id = "rec_keep",
            title = "保持节奏",
            description = "每天复习一点点，进步看得见",
            type = ReviewType.MIXED,
            priority = 0.2f,
            icon = "🌟",
        )
        return recommendations.sortedByDescending { it.priority }
    }

    override suspend fun resetReviewSystem(): ReviewResult {
        ensureSeeded()
        saveItems(emptyList())
        saveMemory(emptyMap())
        saveSchedules(emptyMap())
        saveHistory(emptyList())
        saveSessions(emptyMap())
        saveStats(ReviewStatistics())
        saveSnapshot(null)
        dao.upsertPublishedDoc(ReviewPublishedEntity("all", "[]", "[]", "[]", RoomJson.toJson(DailyReview()), "[]"))
        saveState(ReviewState(todayDate = today()))
        return ReviewResult.Success("Review system reset")
    }

    // ------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------

    private suspend fun buildSnapshot(
        gameProgress: com.sworddao.phoenix.feature.gameplay.data.GameProgress,
        questStats: QuestStats,
        friendshipStates: List<com.sworddao.phoenix.feature.friendship.data.FriendshipState>,
        regions: List<com.sworddao.phoenix.feature.world.data.WorldRegion>,
        speakingStats: com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics,
        listeningStats: com.sworddao.phoenix.feature.listening.data.ListeningStatistics,
        readingStats: com.sworddao.phoenix.feature.reading.data.ReadingStatistics,
    ): ReviewSourceSnapshot {
        return ReviewSourceSnapshot(
            wordsDiscovered = gameProgress.totalWordsDiscovered,
            dialogues = gameProgress.totalDialoguesCompleted,
            questsCompleted = questStats.completedQuests,
            friendshipLevels = friendshipStates.sumOf { it.friendshipLevel.level - 1 },
            passportStamps = gameProgress.totalPassportStamps,
            speakingPractices = gameProgress.totalSpeakingPractices,
            listeningPractices = gameProgress.totalListeningPractices,
            readingPractices = gameProgress.totalReadingPractices,
            writingPractices = gameProgress.totalWritingPractices,
            regionsUnlocked = regions.count { it.isUnlocked },
        )
    }

    private suspend fun weakestWordId(): String? =
        loadMemory().entries.minByOrNull { it.value.strength }?.key

    private suspend fun nextItemId(): String {
        var state = loadState()
        val id = "review_${state.itemIdCounter}"
        state = state.copy(itemIdCounter = state.itemIdCounter + 1)
        saveState(state)
        return id
    }

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private companion object {
        const val MAX_TODAY_ITEMS = 10
        const val MAX_UPCOMING = 10
        const val MAX_HISTORY = 100
        const val MAX_SESSION_ITEMS = 5
        const val REVIEW_XP = 15
    }
}
