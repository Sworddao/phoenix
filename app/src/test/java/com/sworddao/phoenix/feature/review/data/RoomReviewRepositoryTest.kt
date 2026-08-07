package com.sworddao.phoenix.feature.review.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomReviewRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomReviewRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        val game = MockGameProgressRepository()
        val world = MockWorldRepository()
        val quest = MockQuestRepository()
        val passport = MockPassportRepository()
        val vocabulary = MockVocabularyRepository()
        val friendship = MockFriendshipRepository()
        val discovery = MockDiscoveryRepository(vocabulary)
        val pronunciation = MockPronunciationRepository(vocabulary, quest, friendship, game, passport)
        val listening = MockListeningRepository(vocabulary, quest, friendship, game, passport, pronunciation)
        val reading = MockReadingRepository(vocabulary, quest, friendship, game, passport, pronunciation, listening, MockHanziRenderer())
        val progression = MockProgressionRepository(game, world, quest, passport, vocabulary, friendship, discovery, pronunciation, listening, reading)

        repository = RoomReviewRepository(
            database.reviewDao(),
            vocabulary,
            game,
            quest,
            friendship,
            world,
            pronunciation,
            listening,
            reading,
            progression,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initial review state is empty`() = runBlocking {
        assertTrue(repository.getTodayReviews().first().isEmpty())
        assertTrue(repository.getUpcomingReviews().first().isEmpty())
        assertTrue(repository.getReviewHistory().first().isEmpty())
        assertEquals(0, repository.getReviewStatistics().first().totalReviews)
    }

    @Test
    fun `refresh seeds review items from discovered vocabulary`() = runBlocking {
        val result = repository.refresh()
        assertTrue(result is ReviewResult.Refreshed)

        val daily = repository.getDailyReview().first()
        assertTrue(daily.dueCount > 0)

        val today = repository.getTodayReviews().first()
        assertTrue(today.isNotEmpty())
    }

    @Test
    fun `refresh publishes recommendations and memory strengths`() = runBlocking {
        repository.refresh()
        assertTrue(repository.getRecommendations().first().isNotEmpty())
        assertTrue(repository.getMemoryStrengths().first().isNotEmpty())
    }

    @Test
    fun `start session returns due items`() = runBlocking {
        repository.refresh()
        val result = repository.startSession(ReviewType.MIXED)
        assertTrue(result is ReviewResult.SessionStarted)
        val session = (result as ReviewResult.SessionStarted).session
        assertTrue(session.items.isNotEmpty())
        assertEquals(session.items.size, session.totalCount)
    }

    @Test
    fun `start session without due items returns error`() = runBlocking {
        val result = repository.startSession(ReviewType.MIXED)
        assertTrue(result is ReviewResult.Error)
    }

    @Test
    fun `submit correct answer records history and strengthens memory`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()

        val result = repository.submitAnswer(item.id, correct = true, score = 0.9f)
        assertTrue(result is ReviewResult.Answered)
        val answered = result as ReviewResult.Answered
        assertTrue(answered.correct)
        assertTrue(answered.strengthAfter > 0f)

        val history = repository.getReviewHistory().first()
        assertEquals(1, history.size)
        assertEquals(item.wordId, history.first().wordId)
        assertTrue(history.first().correct)
    }

    @Test
    fun `submit wrong answer lowers strength`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()

        val result = repository.submitAnswer(item.id, correct = false, score = 0.2f)
        assertTrue(result is ReviewResult.Answered)
        val answered = result as ReviewResult.Answered
        assertTrue(answered.strengthAfter < (answered.strengthAfter + 1f))
        assertTrue(repository.getReviewHistory().first().any { !it.correct })
    }

    @Test
    fun `submit unknown item returns error`() = runBlocking {
        repository.refresh()
        val result = repository.submitAnswer("missing_item", correct = true, score = 0.9f)
        assertTrue(result is ReviewResult.Error)
    }

    @Test
    fun `complete session grants xp and updates statistics`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session

        val result = repository.completeSession(session.id)
        assertTrue(result is ReviewResult.SessionCompleted)
        val completed = result as ReviewResult.SessionCompleted
        assertEquals(15, completed.xpEarned)

        val stats = repository.getReviewStatistics().first()
        assertEquals(1, stats.totalSessions)
        assertEquals(1, stats.completedSessions)
        assertEquals(15, stats.xpEarned)
    }

    @Test
    fun `complete session twice returns error`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        repository.completeSession(session.id)
        val result = repository.completeSession(session.id)
        assertTrue(result is ReviewResult.Error)
    }

    @Test
    fun `resetReviewSystem clears everything`() = runBlocking {
        repository.refresh()
        val result = repository.resetReviewSystem()
        assertTrue(result is ReviewResult.Success)

        assertTrue(repository.getTodayReviews().first().isEmpty())
        assertTrue(repository.getReviewHistory().first().isEmpty())
        assertEquals(0, repository.getReviewStatistics().first().totalReviews)
    }
}
