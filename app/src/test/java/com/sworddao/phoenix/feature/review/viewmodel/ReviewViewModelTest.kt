package com.sworddao.phoenix.feature.review.viewmodel

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
import com.sworddao.phoenix.feature.review.data.MockReviewRepository
import com.sworddao.phoenix.feature.review.data.ReviewType
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewViewModelTest {

    private lateinit var repository: MockReviewRepository
    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vocabularyRepository = MockVocabularyRepository()
        gameProgressRepository = MockGameProgressRepository()
        val questRepository = MockQuestRepository()
        val friendshipRepository = MockFriendshipRepository()
        val passportRepository = MockPassportRepository()
        val worldRepository = MockWorldRepository()
        val discoveryRepository = MockDiscoveryRepository(vocabularyRepository = vocabularyRepository)
        val pronunciationRepository = MockPronunciationRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        val listeningRepository = MockListeningRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
        )
        val readingRepository = MockReadingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            hanziRenderer = MockHanziRenderer(),
        )
        val writingRepository = MockWritingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        val progressionRepository = MockProgressionRepository(
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
            writingRepository = writingRepository,
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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun seedWords(vararg ids: String) {
        ids.forEach { id ->
            vocabularyRepository.discoverWord(id)
            gameProgressRepository.recordWordDiscovered(id)
        }
    }

    private fun createViewModel(): ReviewViewModel = ReviewViewModel(reviewRepository = repository)

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    @Test
    fun `init loads review dashboard`() = runTest {
        seedWords("greet_001", "greet_003")
        repository.refresh()

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(2, state.todayReviews.size)
        assertEquals(2, state.memoryStrengths.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.recommendations)
    }

    @Test
    fun `init with no reviews yields empty dashboard`() = runTest {
        repository.refresh()

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.todayReviews.isEmpty())
        assertFalse(state.isLoading)
    }

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    @Test
    fun `start session populates active session`() = runTest {
        seedWords("greet_001", "food_001", "greet_003")
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)

        val state = viewModel.uiState.value
        assertNotNull(state.activeSession)
        assertEquals(3, state.activeSession!!.totalCount)
        assertEquals(0f, state.sessionProgress, 0.001f)
        assertFalse(state.showCompletion)
    }

    @Test
    fun `start session reports error when nothing due`() = runTest {
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)

        assertNotNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.activeSession)
    }

    @Test
    fun `answering advances session progress`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)
        viewModel.answerCurrent(true)
        viewModel.answerCurrent(false)

        val state = viewModel.uiState.value
        assertEquals(2, state.sessionAnswers)
        assertEquals(1f, state.sessionProgress, 0.001f)
        assertEquals(false, state.lastAnswerCorrect)
        assertTrue(state.showCompletion)
    }

    @Test
    fun `completing session reports xp and accuracy`() = runTest {
        seedWords("greet_001", "food_001")
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)
        viewModel.answerCurrent(true)
        viewModel.answerCurrent(true)
        viewModel.completeActiveSession()

        val state = viewModel.uiState.value
        assertNull(state.activeSession)
        assertTrue(state.showCompletion)
        assertEquals(15, state.completedXp)
        assertEquals(1f, state.completedAccuracy, 0.001f)
    }

    @Test
    fun `dismiss completion resets session state`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)
        viewModel.answerCurrent(true)
        viewModel.dismissCompletion()

        val state = viewModel.uiState.value
        assertNull(state.activeSession)
        assertEquals(0, state.sessionAnswers)
        assertEquals(0f, state.sessionProgress, 0.001f)
        assertFalse(state.showCompletion)
    }

    @Test
    fun `answer without active session is a no-op`() = runTest {
        seedWords("greet_001")
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.answerCurrent(true)

        assertNull(viewModel.uiState.value.activeSession)
        assertEquals(0, viewModel.uiState.value.sessionAnswers)
    }

    @Test
    fun `dismiss error clears error message`() = runTest {
        repository.refresh()

        val viewModel = createViewModel()
        viewModel.startSession(ReviewType.DAILY_REVIEW)
        assertNotNull(viewModel.uiState.value.error)

        viewModel.dismissError()

        assertNull(viewModel.uiState.value.error)
    }
}