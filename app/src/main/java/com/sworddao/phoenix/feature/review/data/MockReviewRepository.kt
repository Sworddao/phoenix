package com.sworddao.phoenix.feature.review.data

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameProgress
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.ListeningStatistics
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.progression.data.MockProgressionRepository
import com.sworddao.phoenix.feature.progression.data.XpSource
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.QuestStats
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.reading.data.ReadingStatistics
import com.sworddao.phoenix.feature.review.domain.ReviewRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import com.sworddao.phoenix.feature.world.data.WorldRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockReviewRepository @Inject constructor(
    private val vocabularyRepository: MockVocabularyRepository,
    private val gameProgressRepository: MockGameProgressRepository,
    private val questRepository: MockQuestRepository,
    private val friendshipRepository: MockFriendshipRepository,
    private val worldRepository: MockWorldRepository,
    private val passportRepository: MockPassportRepository,
    private val pronunciationRepository: MockPronunciationRepository,
    private val listeningRepository: MockListeningRepository,
    private val readingRepository: MockReadingRepository,
    private val progressionRepository: MockProgressionRepository,
) : ReviewRepository {

    private val _todayReviews = MutableStateFlow<List<ReviewItem>>(emptyList())
    private val _upcomingReviews = MutableStateFlow<List<ReviewItem>>(emptyList())
    private val _reviewHistory = MutableStateFlow<List<ReviewHistoryEntry>>(emptyList())
    private val _statistics = MutableStateFlow(ReviewStatistics())
    private val _recommendations = MutableStateFlow<List<ReviewRecommendation>>(emptyList())
    private val _dailyReview = MutableStateFlow(DailyReview())
    private val _memoryStrengths = MutableStateFlow<List<MemoryStrength>>(emptyList())

    private val _items = MutableStateFlow<List<ReviewItem>>(emptyList())
    private val _memory = MutableStateFlow<Map<String, MemoryStrength>>(emptyMap())
    private val _schedules = MutableStateFlow<Map<String, ReviewSchedule>>(emptyMap())
    private val _sessions = MutableStateFlow<Map<String, ReviewSession>>(emptyMap())

    private var lastSnapshot: SourceSnapshot? = null
    private var itemIdCounter: Int = 0
    private var historyIdCounter: Int = 0
    private var sessionIdCounter: Int = 0
    private var todayDate: String = today()
    private var reviewsToday: Int = 0
    private var currentStreakDays: Int = 0
    private var longestStreakDays: Int = 0
    private var xpEarnedTotal: Int = 0
    private val reviewedWordIds = mutableSetOf<String>()

    override fun getTodayReviews(): Flow<List<ReviewItem>> = _todayReviews.asStateFlow()

    override fun getUpcomingReviews(): Flow<List<ReviewItem>> = _upcomingReviews.asStateFlow()

    override fun getReviewHistory(): Flow<List<ReviewHistoryEntry>> = _reviewHistory.asStateFlow()

    override fun getReviewStatistics(): Flow<ReviewStatistics> = _statistics.asStateFlow()

    override fun getRecommendations(): Flow<List<ReviewRecommendation>> =
        _recommendations.asStateFlow()

    override fun getDailyReview(): Flow<DailyReview> = _dailyReview.asStateFlow()

    override fun getMemoryStrengths(): Flow<List<MemoryStrength>> =
        _memoryStrengths.asStateFlow()

    // ------------------------------------------------------------------
    // Refresh: seed words + schedule from source deltas
    // ------------------------------------------------------------------

    override suspend fun refresh(): ReviewResult {
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

        val previous = lastSnapshot
        if (previous != null) {
            scheduleFromDeltas(snapshot, previous, discoveredWords)
        }
        lastSnapshot = snapshot

        publishAll()
        return ReviewResult.Refreshed(_todayReviews.value.size)
    }

    private fun seedWordEntries(words: List<VocabularyWord>) {
        val existing = _memory.value
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
        _memory.value = existing + seeded
        seededItems.forEach { item ->
            _schedules.value = _schedules.value + (item.wordId!! to item.schedule)
            _items.value = _items.value + item
        }
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
        snapshot: SourceSnapshot,
        previous: SourceSnapshot,
        discoveredWords: List<VocabularyWord>,
    ) {
        val now = System.currentTimeMillis()

        if (snapshot.wordsDiscovered > previous.wordsDiscovered) {
            val delta = snapshot.wordsDiscovered - previous.wordsDiscovered
            discoveredWords.takeLast(delta).forEach { word ->
                val entry = _memory.value[word.id]
                if (entry != null) {
                    val item = buildWordItem(word, entry, ReviewType.MIXED, ReviewSource.VOCABULARY, now)
                    _items.value = _items.value + item
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

    private fun buildWordItem(
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
        val memory = _memory.value[wordId] ?: MemoryStrength()
        val item = buildWordItem(word, memory, type, source, now).copy(
            prompt = word.pinyin,
            detail = word.english,
            schedule = itemSchedule(wordId, now),
        )
        _items.value = _items.value + item
    }

    private suspend fun addQuestReviewItem(now: Long) {
        val completed = questRepository.getCompletedQuests().first()
        val quest = completed.lastOrNull() ?: return
        val wordId = weakestWordId()
        if (wordId == null) {
            addGenericItem(ReviewType.QUEST_REVIEW, ReviewSource.QUEST, quest.title, now)
            return
        }
        val memory = _memory.value[wordId] ?: MemoryStrength()
        val item = buildWordItem(
            word = vocabularyRepository.getWordById(wordId).first() ?: return,
            memory = memory,
            type = ReviewType.QUEST_REVIEW,
            source = ReviewSource.QUEST,
            now = now,
        ).copy(
            detail = "任务：「${quest.title}」",
            relatedQuestId = quest.id,
            schedule = itemSchedule(wordId, now),
        )
        _items.value = _items.value + item
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
        val memory = _memory.value[wordId] ?: MemoryStrength()
        val item = buildWordItem(
            word = vocabularyRepository.getWordById(wordId).first() ?: return,
            memory = memory,
            type = ReviewType.NPC_CHALLENGE,
            source = ReviewSource.NPC_CONVERSATION,
            now = now,
        ).copy(
            detail = "来自 ${bestNpc.npcId} 的挑战",
            relatedNpcId = bestNpc.npcId,
            schedule = itemSchedule(wordId, now),
        )
        _items.value = _items.value + item
    }

    private fun addGenericItem(type: ReviewType, source: ReviewSource, title: String, now: Long) {
        val id = nextItemId()
        _items.value = _items.value + ReviewItem(
            id = id,
            source = source,
            type = type,
            prompt = title,
            detail = "点击复习",
            memoryStrength = 0.4f,
            schedule = ReviewSchedule(itemId = id, stage = 0, dueAt = now),
        )
    }

    private fun itemSchedule(wordId: String, now: Long): ReviewSchedule =
        _schedules.value[wordId] ?: ReviewSchedule(
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
        val due = _todayReviews.value
        val items = when (type) {
            ReviewType.DAILY_REVIEW, ReviewType.MIXED -> due.take(MAX_SESSION_ITEMS)
            else -> due.filter { it.type == type }
                .ifEmpty { due.take(MAX_SESSION_ITEMS) }
        }
        if (items.isEmpty()) {
            return ReviewResult.Error("今日暂无待复习内容")
        }
        val session = ReviewSession(
            id = "session_${sessionIdCounter++}",
            type = type,
            items = items,
            totalCount = items.size,
        )
        _sessions.value = _sessions.value + (session.id to session)
        _statistics.value = _statistics.value.copy(totalSessions = _statistics.value.totalSessions + 1)
        return ReviewResult.SessionStarted(session)
    }

    override suspend fun completeSession(sessionId: String): ReviewResult {
        val session = _sessions.value[sessionId] ?: return ReviewResult.Error("Session not found")
        if (session.isCompleted) return ReviewResult.Error("Session already completed")

        val completed = session.copy(
            completedAt = System.currentTimeMillis(),
            isCompleted = true,
        )
        _sessions.value = _sessions.value + (sessionId to completed)

        val stats = _statistics.value
        _statistics.value = stats.copy(
            completedSessions = stats.completedSessions + 1,
            xpEarned = stats.xpEarned + REVIEW_XP,
        )
        xpEarnedTotal += REVIEW_XP
        progressionRepository.awardXp(XpSource.REVIEW, 1)

        publishAll()
        return ReviewResult.SessionCompleted(completed, REVIEW_XP, completed.accuracy)
    }

    // ------------------------------------------------------------------
    // Answering
    // ------------------------------------------------------------------

    override suspend fun submitAnswer(itemId: String, correct: Boolean, score: Float): ReviewResult {
        val item = _items.value.find { it.id == itemId }
            ?: return ReviewResult.Error("Review item not found")

        val wordId = item.wordId
        val memory = wordId?.let { _memory.value[it] }
        val schedule = wordId?.let { _schedules.value[it] } ?: item.schedule

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
            _memory.value = _memory.value + (wordId to finalMemory)
            _schedules.value = _schedules.value + (wordId to schedule.copy(
                stage = newStage,
                intervalMillis = interval,
                dueAt = nextReviewAt,
                lastReviewedAt = now,
            ))
            vocabularyRepository.incrementReview(wordId)
        }

        val historyEntry = ReviewHistoryEntry(
            id = "history_${historyIdCounter++}",
            itemId = itemId,
            wordId = wordId,
            reviewedAt = now,
            correct = correct,
            score = score.coerceIn(0f, 1f),
            intervalMillis = interval,
            type = item.type,
            strengthAfter = strengthAfter,
        )
        _reviewHistory.value = (listOf(historyEntry) + _reviewHistory.value).take(MAX_HISTORY)

        _items.value = _items.value.map { existing ->
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
        }

        wordId?.let { reviewedWordIds.add(it) }
        updateStatistics(item, correct, score)
        recordDailyActivity(correct)

        _sessions.value = _sessions.value.mapValues { (_, session) ->
            if (!session.isCompleted && session.items.any { it.id == itemId }) {
                session.copy(
                    correctCount = session.correctCount + if (correct) 1 else 0,
                    incorrectCount = session.incorrectCount + if (correct) 0 else 1,
                )
            } else {
                session
            }
        }

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

    private fun updateStatistics(item: ReviewItem, correct: Boolean, score: Float) {
        val stats = _statistics.value
        val byType = stats.byType.toMutableMap()
        byType[item.type] = (byType[item.type] ?: 0) + 1
        _statistics.value = stats.copy(
            totalReviews = stats.totalReviews + 1,
            correctReviews = stats.correctReviews + if (correct) 1 else 0,
            incorrectReviews = stats.incorrectReviews + if (correct) 0 else 1,
            totalScore = stats.totalScore + score.coerceIn(0f, 1f),
            wordsReviewed = reviewedWordIds.size,
            wordsMastered = _memory.value.values.count { it.strength >= SpacedRepetitionEngine.MASTERY_THRESHOLD },
            byType = byType,
        )
    }

    private fun recordDailyActivity(correct: Boolean) {
        val now = LocalDate.now()
        if (now.toString() != todayDate) {
            if (reviewsToday >= DailyReview.DAILY_GOAL) currentStreakDays++ else currentStreakDays = 0
            if (currentStreakDays > longestStreakDays) longestStreakDays = currentStreakDays
            todayDate = now.toString()
            reviewsToday = 0
        }
        reviewsToday++
    }

    // ------------------------------------------------------------------
    // Publishing derived flows
    // ------------------------------------------------------------------

    private fun publishAll() {
        val now = System.currentTimeMillis()
        val due = _items.value
            .filter { it.schedule.dueAt <= now }
            .sortedWith(compareByDescending<ReviewItem> { it.priority }.thenBy { it.schedule.dueAt })
            .take(MAX_TODAY_ITEMS)
        val upcoming = _items.value
            .filter { it.schedule.dueAt > now }
            .sortedBy { it.schedule.dueAt }
            .take(MAX_UPCOMING)

        _todayReviews.value = due
        _upcomingReviews.value = upcoming

        val allMemory = _memory.value.values
        _memoryStrengths.value = allMemory.sortedBy { it.strength }

        _dailyReview.value = DailyReview(
            date = todayDate,
            dueCount = due.size,
            completedCount = reviewsToday,
            dailyGoal = DailyReview.DAILY_GOAL,
            weakestWords = allMemory.sortedBy { it.strength }.take(3),
            bestWords = allMemory.sortedByDescending { it.strength }.take(3),
        )

        _statistics.value = _statistics.value.copy(
            currentStreakDays = currentStreakDays,
            longestStreakDays = longestStreakDays,
            xpEarned = xpEarnedTotal,
            wordsMastered = allMemory.count { it.strength >= SpacedRepetitionEngine.MASTERY_THRESHOLD },
        )

        _recommendations.value = buildRecommendations(due, allMemory)
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
        _items.value = emptyList()
        _memory.value = emptyMap()
        _schedules.value = emptyMap()
        _reviewHistory.value = emptyList()
        _sessions.value = emptyMap()
        _statistics.value = ReviewStatistics()
        _recommendations.value = emptyList()
        _dailyReview.value = DailyReview()
        _memoryStrengths.value = emptyList()
        lastSnapshot = null
        itemIdCounter = 0
        historyIdCounter = 0
        sessionIdCounter = 0
        todayDate = today()
        reviewsToday = 0
        currentStreakDays = 0
        longestStreakDays = 0
        xpEarnedTotal = 0
        reviewedWordIds.clear()
        return ReviewResult.Success("Review system reset")
    }

    // ------------------------------------------------------------------
    // Snapshot
    // ------------------------------------------------------------------

    private fun buildSnapshot(
        gameProgress: GameProgress,
        questStats: QuestStats,
        friendshipStates: List<com.sworddao.phoenix.feature.friendship.data.FriendshipState>,
        regions: List<WorldRegion>,
        speakingStats: SpeakingStatistics,
        listeningStats: ListeningStatistics,
        readingStats: ReadingStatistics,
    ): SourceSnapshot {
        return SourceSnapshot(
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

    private data class SourceSnapshot(
        val wordsDiscovered: Int,
        val dialogues: Int,
        val questsCompleted: Int,
        val friendshipLevels: Int,
        val passportStamps: Int,
        val speakingPractices: Int,
        val listeningPractices: Int,
        val readingPractices: Int,
        val writingPractices: Int,
        val regionsUnlocked: Int,
    )

    private fun weakestWordId(): String? =
        _memory.value.entries.minByOrNull { it.value.strength }?.key

    private fun nextItemId(): String = "review_${itemIdCounter++}"

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    companion object {
        private const val MAX_TODAY_ITEMS = 10
        private const val MAX_UPCOMING = 10
        private const val MAX_HISTORY = 100
        private const val MAX_SESSION_ITEMS = 5
        private const val REVIEW_XP = 15
    }
}
