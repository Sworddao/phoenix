package com.sworddao.phoenix.feature.review.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.sworddao.phoenix.feature.writing.data.MockWritingRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomReviewRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomReviewRepository
    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var progressionRepository: MockProgressionRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = buildRepository(database)
    }

    private fun buildRepository(database: PhoenixDatabase): RoomReviewRepository {
        val game = MockGameProgressRepository()
        val world = MockWorldRepository()
        val quest = MockQuestRepository()
        val passport = MockPassportRepository()
        vocabularyRepository = MockVocabularyRepository()
        val friendship = MockFriendshipRepository()
        val discovery = MockDiscoveryRepository(vocabularyRepository)
        val pronunciation = MockPronunciationRepository(vocabularyRepository, quest, friendship, game, passport)
        val listening = MockListeningRepository(vocabularyRepository, quest, friendship, game, passport, pronunciation)
        val reading = MockReadingRepository(vocabularyRepository, quest, friendship, game, passport, pronunciation, listening, MockHanziRenderer())
        val writing = MockWritingRepository(vocabularyRepository, quest, friendship, game, passport)
        progressionRepository = MockProgressionRepository(game, world, quest, passport, vocabularyRepository, friendship, discovery, pronunciation, listening, reading, writing)

        return RoomReviewRepository(
            database.reviewDao(),
            vocabularyRepository,
            game,
            quest,
            friendship,
            world,
            pronunciation,
            listening,
            reading,
            progressionRepository,
        )
    }

    private fun createFileDatabase(dbName: String, deleteExisting: Boolean = false): PhoenixDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (deleteExisting) context.deleteDatabase(dbName)
        return Room.databaseBuilder(context, PhoenixDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
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
    fun `submitting an answer advances the persisted item stage`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()
        assertEquals(0, item.schedule.stage)

        repository.submitAnswer(item.id, correct = true, score = 0.9f)

        val refreshed = repository.getTodayReviews().first() + repository.getUpcomingReviews().first()
        val updated = refreshed.firstOrNull { it.id == item.id }
        assertNotNull(updated)
        assertEquals(1, updated!!.schedule.stage)
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
    fun `submit correct answer increments vocabulary timesReviewed`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()
        val wordId = requireNotNull(item.wordId)
        val before = vocabularyRepository.getWordById(wordId).first()?.timesReviewed ?: 0

        repository.submitAnswer(item.id, correct = true, score = 0.9f)

        val word = vocabularyRepository.getWordById(wordId).first()
        assertEquals(before + 1, word?.timesReviewed)
    }

    @Test
    fun `submit correct answer moves item from today into upcoming`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()
        assertTrue(repository.getTodayReviews().first().any { it.id == item.id })

        repository.submitAnswer(item.id, correct = true, score = 0.9f)

        assertFalse(repository.getTodayReviews().first().any { it.id == item.id })
        val upcoming = repository.getUpcomingReviews().first().firstOrNull { it.id == item.id }
        assertNotNull(upcoming)
        assertEquals(1, upcoming!!.schedule.stage)
    }

    @Test
    fun `repeated wrong answers surface a recovery recommendation`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()

        repository.submitAnswer(item.id, correct = false, score = 0.2f)
        repository.submitAnswer(item.id, correct = false, score = 0.2f)

        val recommendations = repository.getRecommendations().first()
        assertTrue(recommendations.any { it.id == "rec_failures" })
    }

    @Test
    fun `complete session awards progression xp`() = runBlocking {
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session

        repository.completeSession(session.id)

        val player = progressionRepository.getPlayerProgress().first()
        assertEquals(15, player.totalXp)
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

    @Test
    fun `schedules memory history and statistics survive database restart`() = runBlocking {
        database = createFileDatabase("restart_review_test.db", deleteExisting = true)
        repository = buildRepository(database)
        repository.refresh()
        val session = (repository.startSession(ReviewType.MIXED) as ReviewResult.SessionStarted).session
        val item = session.items.first()
        assertEquals(0, item.schedule.stage)
        repository.submitAnswer(item.id, correct = true, score = 0.9f)

        val memoryBefore = repository.getMemoryStrengths().first().size
        val historyBefore = repository.getReviewHistory().first().size
        val statsBefore = repository.getReviewStatistics().first()
        assertTrue(memoryBefore > 0)
        assertTrue(historyBefore > 0)
        database.close()

        database = createFileDatabase("restart_review_test.db")
        repository = buildRepository(database)
        val refreshResult = repository.refresh()
        assertTrue(refreshResult is ReviewResult.Refreshed)

        val refreshed = repository.getTodayReviews().first() + repository.getUpcomingReviews().first()
        val surviving = refreshed.firstOrNull { it.id == item.id }
        assertNotNull(surviving)
        assertEquals(1, surviving!!.schedule.stage)
        assertEquals(memoryBefore, repository.getMemoryStrengths().first().size)
        assertEquals(historyBefore, repository.getReviewHistory().first().size)
        assertEquals(statsBefore.totalReviews, repository.getReviewStatistics().first().totalReviews)
    }
}
