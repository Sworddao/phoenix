package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.data.seed.WritingSeedData
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
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
class RoomWritingRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var repository: RoomWritingRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        repository = createRepository()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRepository(): RoomWritingRepository = RoomWritingRepository(
        database.writingDao(),
        vocabularyRepository,
        questRepository,
        friendshipRepository,
        gameProgressRepository,
        passportRepository,
    )

    private fun correctAttempt(exercise: WritingExercise): WritingAttempt = WritingAttempt(
        exerciseId = exercise.id,
        wordId = exercise.character.wordId,
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

    @Test
    fun `first access seeds exercises from seed data`() = runBlocking {
        val exercises = repository.getAllExercises().first()

        assertEquals(WritingSeedData.createInitialExercises().size, exercises.size)
        assertTrue(exercises.all { it.isUnlocked })
        assertTrue(exercises.any { it.character.id == "write_char_ni" })
        assertTrue(exercises.any { it.character.strokes.isNotEmpty() })
    }

    @Test
    fun `getExercisesByType and byDifficulty filter seeded exercises`() = runBlocking {
        val traces = repository.getExercisesByType(WritingExerciseType.TRACE_STROKES).first()
        val beginners = repository.getExercisesByDifficulty(WritingDifficulty.BEGINNER).first()

        assertTrue(traces.isNotEmpty())
        assertTrue(traces.all { it.type == WritingExerciseType.TRACE_STROKES })
        assertTrue(beginners.all { it.difficulty == WritingDifficulty.BEGINNER })
    }

    @Test
    fun `getExercisesByWord returns exercises for the word`() = runBlocking {
        val exercises = repository.getExercisesByWord("greet_001").first()

        assertTrue(exercises.isNotEmpty())
        assertTrue(exercises.all { it.character.wordId == "greet_001" })
    }

    @Test
    fun `getRecommendedExercises respects limit and order`() = runBlocking {
        val recommended = repository.getRecommendedExercises(5).first()

        assertEquals(5, recommended.size)
        assertEquals(recommended.sortedBy { it.order }, recommended)
    }

    @Test
    fun `startSession with characterIds selects matching exercises`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first()
        val session = repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        assertTrue(session.exerciseIds.isNotEmpty())
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertEquals(character.id, exercise?.character?.id)
    }

    @Test
    fun `startSession with unknown characterId creates dynamic exercise from vocabulary`() = runBlocking {
        val session = repository.startSession(
            WritingSessionConfig(characterIds = listOf("greet_001"))
        )

        assertTrue(session.exerciseIds.isNotEmpty())
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertNotNull(exercise)
        assertEquals("write_dynamic_greet_001", exercise?.id)
    }

    @Test
    fun `submitAnswer correct attempt awards xp and updates statistics`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getAllExercises().first().first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val result = repository.submitAnswer(correctAttempt(exercise))

        assertTrue(result is WritingResultStatus.ExerciseCompleted)
        val completed = (result as WritingResultStatus.ExerciseCompleted).result
        assertEquals(exercise.xpReward, completed.xpEarned)
        assertTrue(completed.isNewPersonalBest)
        assertTrue(completed.attempt.wasCorrect)

        val statistics = repository.getWritingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.correctAttempts)
        assertEquals(1, statistics.totalExercises)
    }

    @Test
    fun `submitAnswer correct attempt updates vocabulary and game progress`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first { it.wordId != null }
        val exercise = repository.getAllExercises().first().first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        repository.submitAnswer(correctAttempt(exercise))

        val word = vocabularyRepository.getWordById(character.wordId!!).first()
        assertEquals(1, word?.timesWritten)
        assertEquals(1, gameProgressRepository.getGameProgress().first().totalWritingPractices)
    }

    @Test
    fun `submitAnswer wrong attempt awards no xp`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getAllExercises().first().first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        val wrong = correctAttempt(exercise).copy(
            strokeAnswers = exercise.character.strokes.mapIndexed { index, stroke ->
                WritingStrokeAnswer(
                    strokeIndex = index,
                    expectedType = stroke.type,
                    expectedDirection = stroke.direction,
                    wasCorrect = false,
                    attempts = 2,
                )
            }
        )

        val result = repository.submitAnswer(wrong)

        assertTrue(result is WritingResultStatus.ExerciseCompleted)
        val completed = (result as WritingResultStatus.ExerciseCompleted).result
        assertEquals(0, completed.xpEarned)
        assertFalse(completed.attempt.wasCorrect)

        val statistics = repository.getWritingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(0, statistics.correctAttempts)
    }

    @Test
    fun `submitAnswer without active session returns error`() = runBlocking {
        val exercise = repository.getAllExercises().first().first()
        val result = repository.submitAnswer(correctAttempt(exercise))

        assertTrue(result is WritingResultStatus.Error)
    }

    @Test
    fun `submitAnswer with unknown exercise returns error`() = runBlocking {
        repository.startSession(WritingSessionConfig(exerciseCount = 3))
        val result = repository.submitAnswer(correctAttempt(exercise = WritingSeedData.createInitialExercises().first()).copy(exerciseId = "write_ex_unknown"))

        assertTrue(result is WritingResultStatus.Error)
    }

    @Test
    fun `submitAnswer correct attempt records passport entry`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getAllExercises().first().first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))

        repository.submitAnswer(correctAttempt(exercise))

        val entries = passportRepository.getRecentEntries(10).first()
        assertTrue(entries.any { it.type == EntryType.WRITING_PRACTICE })
    }

    @Test
    fun `completeSession marks session complete and increments totalSessions`() = runBlocking {
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
    fun `updateProgress computes mastery level`() = runBlocking {
        val result = repository.updateProgress(
            WritingProgress(itemId = "greet_001", wordId = "greet_001", totalAttempts = 1, correctAttempts = 1)
        )

        assertTrue(result is WritingResultStatus.ProgressUpdated)
        val updated = (result as WritingResultStatus.ProgressUpdated).progress
        assertEquals(WritingMastery.SEEN, updated.masteryLevel)

        val progress = repository.getWritingProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(WritingMastery.SEEN, progress?.masteryLevel)
    }

    @Test
    fun `unlockExercise unlocks an added locked exercise`() = runBlocking {
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

        assertFalse(repository.getExerciseById("write_ex_trace_locked").first()?.isUnlocked ?: true)

        val unlocked = repository.unlockExercise("write_ex_trace_locked")
        assertTrue(unlocked is WritingResultStatus.Success)
        assertTrue(repository.getExerciseById("write_ex_trace_locked").first()?.isUnlocked ?: false)
    }

    @Test
    fun `unlockExercise on seeded unlocked exercise reports already unlocked`() = runBlocking {
        val exercise = repository.getAllExercises().first().first()

        val result = repository.unlockExercise(exercise.id)

        assertTrue(result is WritingResultStatus.Success)
        assertEquals("Exercise already unlocked", (result as WritingResultStatus.Success).message)
    }

    @Test
    fun `unlockExercise with unknown id returns error`() = runBlocking {
        val result = repository.unlockExercise("write_ex_nonexistent")

        assertTrue(result is WritingResultStatus.Error)
    }

    @Test
    fun `recordStreak updates streak state and statistics`() = runBlocking {
        val result = repository.recordStreak(5)

        assertTrue(result is WritingResultStatus.StreakUpdated)
        val streak = result as WritingResultStatus.StreakUpdated
        assertEquals(5, streak.currentStreak)
        assertEquals(5, streak.longestStreak)

        val statistics = repository.getWritingStatistics().first()
        assertEquals(5, statistics.currentStreak)
        assertEquals(5, statistics.longestStreak)
    }

    @Test
    fun `awardBadge earns a badge once`() = runBlocking {
        val first = repository.awardBadge("write_first")
        assertTrue(first is WritingResultStatus.BadgeEarned)

        val second = repository.awardBadge("write_first")
        assertTrue(second is WritingResultStatus.Error)

        val badges = repository.getWritingBadges().first()
        val earned = badges.find { it.id == "write_first" }
        assertTrue(earned?.isEarned == true)
    }

    @Test
    fun `state persists across repository instances`() = runBlocking {
        val character = WritingSeedData.createInitialCharacters().first()
        val exercise = repository.getAllExercises().first().first { it.character.id == character.id }
        repository.startSession(WritingSessionConfig(characterIds = listOf(character.id)))
        repository.submitAnswer(correctAttempt(exercise))

        val fresh = createRepository()
        val statistics = fresh.getWritingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.correctAttempts)

        val exercises = fresh.getAllExercises().first()
        assertEquals(WritingSeedData.createInitialExercises().size, exercises.size)
    }
}
