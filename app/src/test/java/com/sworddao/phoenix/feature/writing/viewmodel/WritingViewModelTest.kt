package com.sworddao.phoenix.feature.writing.viewmodel

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.writing.data.MockWritingEngine
import com.sworddao.phoenix.feature.writing.data.MockWritingRepository
import com.sworddao.phoenix.feature.writing.data.StrokeDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WritingViewModelTest {

    private lateinit var repository: MockWritingRepository

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = MockWritingRepository(
            MockVocabularyRepository(),
            MockQuestRepository(),
            MockFriendshipRepository(),
            MockGameProgressRepository(),
            MockPassportRepository(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): WritingViewModel =
        WritingViewModel(repository, MockWritingEngine())

    @Test
    fun `startPractice with wordId selects seeded exercise with stroke data`() = runTest {
        val viewModel = createViewModel()
        viewModel.startPractice("greet_001")

        val exercise = requireNotNull(viewModel.uiState.value.currentExercise)
        assertEquals("write_ex_trace_ni", exercise.id)
        assertTrue(exercise.character.strokes.isNotEmpty())
    }

    @Test
    fun `completing an exercise records real stroke attempts`() = runTest {
        val viewModel = createViewModel()
        viewModel.startPractice("greet_001")

        val exercise = requireNotNull(viewModel.uiState.value.currentExercise)
        val strokes = exercise.character.strokes
        assertTrue(strokes.isNotEmpty())

        val firstStroke = strokes.first()
        val wrongDirection = StrokeDirection.entries.first { it != firstStroke.direction }
        viewModel.recordStroke(wrongDirection)
        assertFalse(viewModel.uiState.value.lastStrokeFeedback?.wasCorrect ?: true)
        assertEquals(0, viewModel.uiState.value.expectedStrokeIndex)

        viewModel.recordStroke(firstStroke.direction)
        assertEquals(1, viewModel.uiState.value.expectedStrokeIndex)

        strokes.drop(1).forEach { stroke ->
            viewModel.recordStroke(stroke.direction)
        }

        val state = viewModel.uiState.value
        assertTrue(state.isExerciseComplete)
        val result = requireNotNull(state.lastResult)
        assertTrue(result.attempt.wasCorrect)
        assertEquals(strokes.size, result.attempt.strokeAnswers.size)
        assertEquals(strokes.size, result.attempt.correctStrokeCount)
        assertEquals(2, result.attempt.strokeAnswers.first().attempts)
        assertTrue(result.attempt.strokeAnswers.drop(1).all { it.attempts == 1 })
        assertEquals(1, state.statistics.totalAttempts)
        assertEquals(1, state.statistics.correctAttempts)
    }

    @Test
    fun `wrong direction feedback does not advance the pending stroke`() = runTest {
        val viewModel = createViewModel()
        viewModel.startPractice("greet_001")

        val exercise = requireNotNull(viewModel.uiState.value.currentExercise)
        val firstStroke = exercise.character.strokes.first()
        val wrongDirection = StrokeDirection.entries.first { it != firstStroke.direction }

        viewModel.recordStroke(wrongDirection)

        val state = viewModel.uiState.value
        assertFalse(state.isExerciseComplete)
        assertEquals(0, state.expectedStrokeIndex)
        assertTrue(state.strokesCompleted.isEmpty())
        assertEquals(1, state.pendingStrokeAttempts)
        assertTrue(state.strokeAnswers.isEmpty())
    }

    @Test
    fun `nextExercise advances through the session exercises`() = runTest {
        val viewModel = createViewModel()
        viewModel.startPractice("greet_001")

        val first = requireNotNull(viewModel.uiState.value.currentExercise)
        val firstId = first.id
        val strokes = first.character.strokes
        strokes.forEach { stroke -> viewModel.recordStroke(stroke.direction) }

        viewModel.nextExercise()

        val next = requireNotNull(viewModel.uiState.value.currentExercise)
        assertNotNull(next)
        assertTrue(next.id != firstId)
        assertEquals(0, viewModel.uiState.value.expectedStrokeIndex)
        assertTrue(viewModel.uiState.value.strokeAnswers.isEmpty())
    }
}
