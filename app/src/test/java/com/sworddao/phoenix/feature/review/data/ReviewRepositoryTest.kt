package com.sworddao.phoenix.feature.review.data

import com.sworddao.phoenix.feature.discovery.data.MockDiscoveryRepository
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.progression.data.MockProgressionRepository
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.reading.data.MockHanziRenderer
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var worldRepository: MockWorldRepository
    private lateinit var pronunciationRepository: MockPronunciationRepository
    private lateinit var listeningRepository: MockListeningRepository
    private lateinit var readingRepository: MockReadingRepository
    private lateinit var progressionRepository: MockProgressionRepository
    private lateinit var repository: MockReviewRepository

    @Before
    fun setup() {
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        worldRepository = MockWorldRepository()
        val discoveryRepository = MockDiscoveryRepository(vocabularyRepository = vocabularyRepository)
        pronunciationRepository = MockPronunciationRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        listeningRepository = MockListeningRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
        )
        readingRepository = MockReadingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            hanziRenderer = MockHanziRenderer(),
        )
        progressionRepository = MockProgressionRepository(
            gameProgressRepository = gameProgressRepository,
            worldRepository = worldRepository,
            questRepository = questRepository,
            passportRepository = passportRepository,
            vocabularyRepository = vocabularyRepository,
            friendshipRepository = friendshipRepository,
            discoveryRepository = discoveryRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            readingRepository = readingRepository,
        )
        repository = MockReviewRepository(
            vocabularyRepository = vocabularyRepository,
            gameProgressRepository = gameProgressRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            worldRepository = worldRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            readingRepository = readingRepository,
            progressionRepository = progressionRepository,
        )
        vocabularyRepository.resetVocabularySystem()
    }

    private suspend fun seedWords(vararg ids: String) {
        ids.forEach { id ->
            vocabularyRepository.discoverWord(id)
            gameProgressRepository.recordWordDiscovered(id)
        }
    }

    private suspend fun completeFirstQuest() {
        questRepository.startQuest("quest_help_grandma_mei")
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        questRepository.completeQuest("quest_help_grandma_mei")
    }

    // ------------------------------------------------------------------
    // Initial refresh & seeding
    // ------------------------------------------------------------------

    @Test
    fun `refresh with no discovered words yields no due reviews`() = runTest {
        val result = repository.refresh()
        assertTrue(result is ReviewResult.Refreshed)
        assertEquals(0, repository.getTodayReviews().first().size)
        assertTrue(repository.getMemoryStrengths().first().isEmpty())
    }

    @Test
    fun `refresh seeds discovered words as due reviews`() = runTest {
        seedWords("greet_001", "greet_003")
        repository.refresh()

        val today = repository.getTodayReviews().first()
        assertEquals(2, today.size)
        assertEquals(2, repository.getMemoryStrengths().first().size)
    }

    @Test
    fun `seeded reviews are due immediately`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val item = repository.getTodayReviews().first().first()
        assertTrue(item.isDue)
        assertTrue(item.schedule.dueAt <= System.currentTimeMillis())
    }

    @Test
    fun `seeded review references the discovered word`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val item = repository.getTodayReviews().first().first()
        assertEquals("greet_001", item.wordId)
        assertEquals(ReviewSource.VOCABULARY, item.source)
    }

    // ------------------------------------------------------------------
    // Source deltas
    // ------------------------------------------------------------------

    @Test
    fun `dialogue completion schedules a conversation review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        val before = repository.getTodayReviews().first().size

        gameProgressRepository.recordDialogueCompleted("grandma_mei")
        repository.refresh()

        val after = repository.getTodayReviews().first()
        assertEquals(before + 1, after.size)
        assertTrue(after.any { it.type == ReviewType.CONVERSATION })
    }

    @Test
    fun `speaking practice schedules a speaking review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        gameProgressRepository.recordSpeakingPractice()
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.type == ReviewType.SPEAKING })
    }

    @Test
    fun `listening practice schedules a listening review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        gameProgressRepository.recordListeningPractice()
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.type == ReviewType.LISTENING })
    }

    @Test
    fun `reading practice schedules a reading review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        gameProgressRepository.recordReadingPractice()
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.type == ReviewType.READING })
    }

    @Test
    fun `word discovery delta schedules a new word item`() = runTest {
        repository.refresh()
        vocabularyRepository.discoverWord("food_001")
        gameProgressRepository.recordWordDiscovered("food_001")
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.wordId == "food_001" })
    }

    @Test
    fun `quest completion schedules a quest review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        completeFirstQuest()
        repository.refresh()

        val item = repository.getTodayReviews().first().first { it.type == ReviewType.QUEST_REVIEW }
        assertEquals(ReviewSource.QUEST, item.source)
    }

    @Test
    fun `friendship level up schedules an npc challenge`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        friendshipRepository.initializeFriendship("grandma_mei")
        friendshipRepository.addFriendshipXp("grandma_mei", 200)
        repository.refresh()

        val item = repository.getTodayReviews().first().first { it.type == ReviewType.NPC_CHALLENGE }
        assertEquals("grandma_mei", item.relatedNpcId)
    }

    @Test
    fun `passport stamp schedules an exploration review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        gameProgressRepository.recordPassportStampEarned("qingyuan_village")
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.source == ReviewSource.EXPLORATION })
    }

    @Test
    fun `region completion schedules an exploration review`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        worldRepository.completeRegion("high_speed_rail")
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().any { it.source == ReviewSource.EXPLORATION })
    }

    // ------------------------------------------------------------------
    // Today & upcoming
    // ------------------------------------------------------------------

    @Test
    fun `today reviews ranked by descending priority`() = runTest {
        seedWords("greet_001", "food_001", "greet_003")
        repository.refresh()

        val itemId = repository.getTodayReviews().first().first().id
        repository.submitAnswer(itemId, true, 1f)
        repository.refresh()

        val today = repository.getTodayReviews().first()
        assertTrue(today.size in 2..3)
        assertEquals(today.sortedByDescending { it.priority }, today)
    }

    @Test
    fun `today reviews capped at ten`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        repeat(12) { gameProgressRepository.recordDialogueCompleted("grandma_mei") }
        repository.refresh()

        assertTrue(repository.getTodayReviews().first().size <= 10)
    }

    @Test
    fun `answered word appears in upcoming reviews`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val itemId = repository.getTodayReviews().first().first().id
        repository.submitAnswer(itemId, true, 1f)
        repository.refresh()

        val upcoming = repository.getUpcomingReviews().first()
        assertTrue(upcoming.any { it.wordId == "greet_001" })
        assertTrue(upcoming.first().schedule.dueAt > System.currentTimeMillis())
    }

    // ------------------------------------------------------------------
    // Answering
    // ------------------------------------------------------------------

    @Test
    fun `correct answer strengthens memory and advances stage`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        val item = repository.getTodayReviews().first().first()

        val result = repository.submitAnswer(item.id, true, 1f)

        assertTrue(result is ReviewResult.Answered)
        val answered = result as ReviewResult.Answered
        assertTrue(answered.correct)
        assertTrue(answered.strengthAfter > item.memoryStrength)
        assertEquals(SpacedRepetitionEngine.intervalForStage(2), answered.intervalMillis)
        assertEquals(ReviewDifficulty.FAMILIAR, answered.difficulty)
    }

    @Test
    fun `incorrect answer weakens memory and stays at base interval`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        val item = repository.getTodayReviews().first().first()

        val result = repository.submitAnswer(item.id, false, 0f)

        val answered = result as ReviewResult.Answered
        assertFalse(answered.correct)
        assertTrue(answered.strengthAfter < item.memoryStrength)
        assertEquals(SpacedRepetitionEngine.intervalForStage(0), answered.intervalMillis)
        assertEquals(ReviewDifficulty.NEW, answered.difficulty)
    }

    @Test
    fun `answered item drops out of today`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        val before = repository.getTodayReviews().first()
        repository.submitAnswer(before.first().id, true, 1f)

        val after = repository.getTodayReviews().first()
        assertFalse(after.any { it.id == before.first().id })
    }

    @Test
    fun `submit answer records history`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        val item = repository.getTodayReviews().first().first()
        repository.submitAnswer(item.id, true, 1f)

        val history = repository.getReviewHistory().first()
        assertEquals(1, history.size)
        assertEquals("greet_001", history.first().wordId)
        assertTrue(history.first().correct)
    }

    @Test
    fun `submit answer increments vocabulary review count`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        val item = repository.getTodayReviews().first().first()
        val before = vocabularyRepository.getWordById("greet_001").first()!!.timesReviewed

        repository.submitAnswer(item.id, true, 1f)

        val after = vocabularyRepository.getWordById("greet_001").first()!!.timesReviewed
        assertEquals(before + 1, after)
    }

    @Test
    fun `correct answers strengthen every reviewed word`() = runTest {
        seedWords("greet_001", "food_001", "greet_003", "food_002")
        repository.refresh()

        repeat(3) {
            val item = repository.getTodayReviews().first().first()
            val answered = repository.submitAnswer(item.id, true, 1f) as ReviewResult.Answered
            assertEquals(ReviewDifficulty.FAMILIAR, answered.difficulty)
            repository.refresh()
        }

        val strengths = repository.getMemoryStrengths().first()
        assertTrue(strengths.isNotEmpty())
        assertTrue(strengths.any { it.strength > 0.6f })
        assertEquals(3, repository.getReviewStatistics().first().wordsReviewed)
        assertEquals(0, repository.getReviewStatistics().first().wordsMastered)
    }

    @Test
    fun `answer errors for unknown item`() = runTest {
        val result = repository.submitAnswer("nope", true, 1f)
        assertTrue(result is ReviewResult.Error)
    }

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    @Test
    fun `start session returns due items`() = runTest {
        seedWords("greet_001", "food_001", "greet_003")
        repository.refresh()

        val result = repository.startSession(ReviewType.DAILY_REVIEW)
        assertTrue(result is ReviewResult.SessionStarted)
        val session = (result as ReviewResult.SessionStarted).session
        assertTrue(session.items.isNotEmpty())
        assertEquals(session.items.size, session.totalCount)
    }

    @Test
    fun `start session errors when nothing due`() = runTest {
        repository.refresh()
        val result = repository.startSession(ReviewType.DAILY_REVIEW)
        assertTrue(result is ReviewResult.Error)
    }

    @Test
    fun `answers update session counts`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        val result = repository.startSession(ReviewType.DAILY_REVIEW)
        val session = (result as ReviewResult.SessionStarted).session

        repository.submitAnswer(session.items[0].id, true, 1f)
        repository.submitAnswer(session.items[1].id, false, 0f)
        repository.refresh()

        val stats = repository.getReviewStatistics().first()
        assertEquals(1, stats.correctReviews)
        assertEquals(1, stats.incorrectReviews)
    }

    @Test
    fun `complete session awards xp to progression`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        val session = (repository.startSession(ReviewType.DAILY_REVIEW) as ReviewResult.SessionStarted).session
        session.items.forEach { repository.submitAnswer(it.id, true, 1f) }

        val completed = repository.completeSession(session.id)

        assertTrue(completed is ReviewResult.SessionCompleted)
        val resultCompleted = completed as ReviewResult.SessionCompleted
        assertEquals(15, resultCompleted.xpEarned)
        assertEquals(15, progressionRepository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `complete unknown session errors`() = runTest {
        val result = repository.completeSession("missing")
        assertTrue(result is ReviewResult.Error)
    }

    // ------------------------------------------------------------------
    // Statistics & daily
    // ------------------------------------------------------------------

    @Test
    fun `statistics track correct and incorrect reviews`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        val today = repository.getTodayReviews().first()
        repository.submitAnswer(today[0].id, true, 1f)
        repository.submitAnswer(today[1].id, false, 0f)

        val stats = repository.getReviewStatistics().first()
        assertEquals(2, stats.totalReviews)
        assertEquals(1, stats.correctReviews)
        assertEquals(1, stats.incorrectReviews)
    }

    @Test
    fun `daily review counts today answers`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        repository.getTodayReviews().first()
            .take(2)
            .forEach { repository.submitAnswer(it.id, true, 1f) }

        val daily = repository.getDailyReview().first()
        assertEquals(2, daily.completedCount)
        assertEquals(0, daily.dueCount)
    }

    @Test
    fun `daily goal reached after five answers`() = runTest {
        seedWords("greet_001", "food_001", "greet_003", "greet_005", "food_002")
        repository.refresh()
        repository.getTodayReviews().first()
            .take(5)
            .forEach { repository.submitAnswer(it.id, true, 1f) }

        val daily = repository.getDailyReview().first()
        assertTrue(daily.isGoalReached)
    }

    @Test
    fun `daily review reports weakest words`() = runTest {
        seedWords("greet_001", "food_001", "greet_003")
        repository.refresh()

        val daily = repository.getDailyReview().first()
        assertEquals(3, daily.weakestWords.size)
    }

    @Test
    fun `recommendations include daily prompt when due`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val recommendations = repository.getRecommendations().first()
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.id == "rec_daily" })
    }

    @Test
    fun `statistics accuracy reflects answers`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()
        val today = repository.getTodayReviews().first()
        repository.submitAnswer(today[0].id, true, 1f)
        repository.submitAnswer(today[1].id, true, 0.8f)

        val stats = repository.getReviewStatistics().first()
        assertEquals(1f, stats.accuracy, 0.001f)
        assertEquals(0.9f, stats.averageScore, 0.001f)
    }

    @Test
    fun `memory strengths sorted ascending`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()

        val strengths = repository.getMemoryStrengths().first()
        assertEquals(strengths.sortedBy { it.strength }, strengths)
    }

    // ------------------------------------------------------------------
    // Reset
    // ------------------------------------------------------------------

    @Test
    fun `reset clears review state`() = runTest {
        seedWords("greet_001")
        repository.refresh()
        repository.getTodayReviews().first().forEach { repository.submitAnswer(it.id, true, 1f) }

        val result = repository.resetReviewSystem()

        assertTrue(result is ReviewResult.Success)
        assertTrue(repository.getTodayReviews().first().isEmpty())
        assertTrue(repository.getMemoryStrengths().first().isEmpty())
        assertTrue(repository.getReviewHistory().first().isEmpty())
        assertEquals(0, repository.getReviewStatistics().first().totalReviews)
    }
}