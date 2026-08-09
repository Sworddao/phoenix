package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.seed.WritingSeedData
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WritingRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var repository: MockWritingRepository

    @Before
    fun setup() {
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        repository = MockWritingRepository(
            vocabularyRepository,
            questRepository,
            friendshipRepository,
            gameProgressRepository,
            passportRepository,
        )
    }

    @Test
    fun `getAllExercises returns seeded exercises with stroke data`() = runTest {
        val exercises = repository.getAllExercises().first()

        assertTrue(exercises.isNotEmpty())
        val trace = exercises.first { it.type == WritingExerciseType.TRACE_STROKES }
        assertTrue(trace.isUnlocked)
        assertEquals(trace.character.strokeCount, trace.character.strokes.size)
        assertEquals(trace.character.hanzi, trace.hanzi)
    }

    @Test
    fun `getExercisesByType filters by exercise type`() = runTest {
        val traces = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
        val orders = repository.getExercisesByType(WritingExerciseType.STROKE_ORDER).first()

        assertTrue(traces.isNotEmpty())
        assertTrue(traces.all { it.type == WritingExerciseType.TRACE_STROKES })
        assertTrue(orders.all { it.type == WritingExerciseType.STROKE_ORDER })
    }

    @Test
    fun `startSession with characterIds returns matching exercises`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first()
        val session = repository.startSession(
            WritingSessionConfig(characterIds = listOf(character.id))
        )

        assertTrue(session.exerciseIds.isNotEmpty())
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertEquals(character.id, exercise?.character?.id)
    }

    @Test
    fun `submitAnswer correct attempt awards xp and updates statistics`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
            .first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val attempt = WritingAttempt(
            exerciseId = exercise.id,
            wordId = character.wordId,
            hanzi = exercise.hanzi,
            strokeAnswers = exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = true,
                    attempts = 1,
                )
            },
            timeTakenMs = 1500,
        )

        val result = repository.submitAnswer(attempt)

        assertTrue(result is WritingResultStatus.ExerciseCompleted)
        val completed = (result as WritingResultStatus.ExerciseCompleted).result
        assertEquals(exercise.xpReward, completed.xpEarned)
        assertTrue(completed.attempt.wasCorrect)
        assertEquals(character.wordId, completed.attempt.wordId)

        val statistics = repository.getWritingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.correctAttempts)
        assertEquals(1, statistics.totalExercises)
    }

    @Test
    fun `submitAnswer correct attempt updates vocabulary written count`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first { it.wordId != null }
        val exercise = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
            .first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val attempt = WritingAttempt(
            exerciseId = exercise.id,
            wordId = character.wordId,
            hanzi = exercise.hanzi,
            strokeAnswers = exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = true,
                    attempts = 1,
                )
            },
            timeTakenMs = 1500,
        )

        repository.submitAnswer(attempt)

        val word = vocabularyRepository.getWordById(character.wordId!!).first()
        assertEquals(1, word?.timesWritten)
        assertEquals(1, gameProgressRepository.getGameProgress().first().totalWritingPractices)
    }

    @Test
    fun `submitAnswer wrong attempt awards no xp`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
            .first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val attempt = WritingAttempt(
            exerciseId = exercise.id,
            wordId = character.wordId,
            hanzi = exercise.hanzi,
            strokeAnswers = exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = false,
                    attempts = 2,
                )
            },
            timeTakenMs = 3000,
        )

        val result = repository.submitAnswer(attempt)

        assertTrue(result is WritingResultStatus.ExerciseCompleted)
        val completed = (result as WritingResultStatus.ExerciseCompleted).result
        assertEquals(0, completed.xpEarned)
        assertFalse(completed.attempt.wasCorrect)

        val statistics = repository.getWritingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(0, statistics.correctAttempts)
    }

    @Test
    fun `submitAnswer without active session returns error`() = runTest {
        val attempt = WritingAttempt(
            exerciseId = "write_ex_trace_ni",
            wordId = "greet_001",
            hanzi = "你",
            strokeAnswers = emptyList(),
        )

        val result = repository.submitAnswer(attempt)

        assertTrue(result is WritingResultStatus.Error)
    }

    @Test
    fun `completeSession marks session complete and increments totalSessions`() = runTest {
        val session = repository.startSession(WritingSessionConfig(exerciseCount = 3))

        val result = repository.completeSession(session)

        assertTrue(result is WritingResultStatus.SessionCompleted)
        val completed = (result as WritingResultStatus.SessionCompleted).session
        assertTrue(completed.isCompleted)
        assertEquals(1, repository.getWritingStatistics().first().totalSessions)

        val second = repository.completeSession(session)
        assertTrue(second is WritingResultStatus.Error)
    }

    @Test
    fun `recordStreak updates streak statistics`() = runTest {
        val result = repository.recordStreak(5)

        assertTrue(result is WritingResultStatus.StreakUpdated)
        val stats = repository.getWritingStatistics().first()
        assertEquals(5, stats.currentStreak)
        assertEquals(5, stats.longestStreak)
    }

    @Test
    fun `awardBadge earns a badge once`() = runTest {
        val first = repository.awardBadge("write_first")
        assertTrue(first is WritingResultStatus.BadgeEarned)

        val second = repository.awardBadge("write_first")
        assertTrue(second is WritingResultStatus.Error)

        val badges = repository.getWritingBadges().first()
        val earned = badges.find { it.id == "write_first" }
        assertTrue(earned?.isEarned == true)
    }

    @Test
    fun `correct writing records passport entry`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
            .first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val attempt = WritingAttempt(
            exerciseId = exercise.id,
            wordId = character.wordId,
            hanzi = exercise.hanzi,
            strokeAnswers = exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = true,
                    attempts = 1,
                )
            },
            timeTakenMs = 1500,
        )

        repository.submitAnswer(attempt)

        val entries = passportRepository.getRecentEntries(10).first()
        assertTrue(entries.any { it.type == EntryType.WRITING_PRACTICE })
    }

    @Test
    fun `addExercises then unlockExercise makes it available`() = runTest {
        val character = WritingSeedData.createInitialCharacters().first()
        val locked = WritingExercise(
            id = "write_ex_trace_locked",
            type = WritingExerciseType.TRACE_STROKES,
            difficulty = character.difficulty,
            character = character,
            prompt = "locked exercise",
            isUnlocked = false,
        )

        val added = repository.addExercises(listOf(locked))
        assertTrue(added is WritingResultStatus.Success)

        val before = repository.getExerciseById("write_ex_trace_locked").first()
        assertFalse(before?.isUnlocked ?: true)

        val unlocked = repository.unlockExercise("write_ex_trace_locked")
        assertTrue(unlocked is WritingResultStatus.Success)

        val after = repository.getExerciseById("write_ex_trace_locked").first()
        assertTrue(after?.isUnlocked ?: false)
    }
}
