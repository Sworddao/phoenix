package com.sworddao.phoenix.feature.reading.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.RoomListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.RoomPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomReadingRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomReadingRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        val vocabulary = MockVocabularyRepository()
        val quest = MockQuestRepository()
        val friendship = MockFriendshipRepository()
        val game = MockGameProgressRepository()
        val passport = MockPassportRepository()
        val pronunciation = RoomPronunciationRepository(
            database.speakingDao(), vocabulary, quest, friendship, game, passport,
        )
        val listening = RoomListeningRepository(
            database.listeningDao(), vocabulary, quest, friendship, game, passport, pronunciation,
        )
        repository = RoomReadingRepository(
            database.readingDao(),
            vocabulary,
            quest,
            friendship,
            game,
            passport,
            pronunciation,
            listening,
            MockHanziRenderer(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeds initial reading exercises`() = runBlocking {
        val exercises = repository.getAllExercises().first()
        assertEquals(14, exercises.size)
        assertEquals("你好", exercises.first { it.id == "read_ex_greet_hello" }.hanzi)
    }

    @Test
    fun `getExerciseById returns null for missing`() = runBlocking {
        assertNull(repository.getExerciseById("missing").first())
    }

    @Test
    fun `filters exercises by type difficulty and word`() = runBlocking {
        val byType = repository.getExercisesByType(ReadingExerciseType.MATCH_PINYIN_TO_HANZI).first()
        assertTrue(byType.isNotEmpty())
        assertTrue(byType.all { it.type == ReadingExerciseType.MATCH_PINYIN_TO_HANZI })

        val byDifficulty = repository.getExercisesByDifficulty(ReadingDifficulty.BEGINNER).first()
        assertTrue(byDifficulty.isNotEmpty())
        assertTrue(byDifficulty.all { it.difficulty == ReadingDifficulty.BEGINNER })

        val byWord = repository.getExercisesByWord("greet_001").first()
        assertTrue(byWord.all { it.relatedWordId == "greet_001" })
    }

    @Test
    fun `getExercisesByNpc filters by npc`() = runBlocking {
        val byNpc = repository.getExercisesByNpc("grandma_mei").first()
        assertTrue(byNpc.isNotEmpty())
        assertTrue(byNpc.all { it.relatedNpcId == "grandma_mei" })
    }

    @Test
    fun `unlocked exercises and recommendations`() = runBlocking {
        val unlocked = repository.getUnlockedExercises().first()
        assertTrue(unlocked.isNotEmpty())
        assertTrue(unlocked.all { it.isUnlocked })

        val recommended = repository.getRecommendedExercises(3).first()
        assertEquals(3, recommended.size)
    }

    @Test
    fun `initial statistics are empty`() = runBlocking {
        val statistics = repository.getReadingStatistics().first()
        assertEquals(0, statistics.totalAttempts)
        assertEquals(0, statistics.correctAttempts)
        assertTrue(repository.getReadingBadges().first().isNotEmpty())
    }

    @Test
    fun `submit correct answer records progress and xp`() = runBlocking {
        val session = repository.startSession(
            ReadingSessionConfig(wordIds = listOf("greet_001"))
        )
        assertEquals(listOf("read_ex_greet_hello"), session.exerciseIds)

        val result = repository.submitAnswer(
            ReadingAttempt(
                exerciseId = "read_ex_greet_hello",
                wordId = "greet_001",
                chosenChoiceId = "choice_0",
                wasCorrect = true,
            )
        )
        assertTrue(result is ReadingResultStatus.ExerciseCompleted)
        val completed = result as ReadingResultStatus.ExerciseCompleted
        assertEquals(10, completed.result.xpEarned)
        assertEquals(1, completed.result.currentStreak)
        assertTrue(completed.result.reward.isFirstWordRead)

        val progress = repository.getReadingProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.correctAttempts)

        val statistics = repository.getReadingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.correctAttempts)
    }

    @Test
    fun `wrong answer does not award xp`() = runBlocking {
        repository.startSession(ReadingSessionConfig(wordIds = listOf("greet_001")))
        val result = repository.submitAnswer(
            ReadingAttempt(
                exerciseId = "read_ex_greet_hello",
                wordId = "greet_001",
                chosenChoiceId = "choice_1",
                wasCorrect = false,
            )
        )
        val completed = result as ReadingResultStatus.ExerciseCompleted
        assertEquals(0, completed.result.xpEarned)
        assertEquals(0, completed.result.currentStreak)
    }

    @Test
    fun `submit without active session returns error`() = runBlocking {
        val result = repository.submitAnswer(
            ReadingAttempt(exerciseId = "read_ex_greet_hello", chosenChoiceId = "choice_0", wasCorrect = true)
        )
        assertTrue(result is ReadingResultStatus.Error)
    }

    @Test
    fun `completeSession increments session counter`() = runBlocking {
        val session = repository.startSession(ReadingSessionConfig())
        val result = repository.completeSession(session)
        assertTrue(result is ReadingResultStatus.SessionCompleted)
        val completed = result as ReadingResultStatus.SessionCompleted
        assertTrue(completed.session.isCompleted)
        assertEquals(1, completed.statistics.totalSessions)
    }

    @Test
    fun `updateProgress computes mastery`() = runBlocking {
        val result = repository.updateProgress(
            ReadingProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 20,
                correctAttempts = 20,
            )
        )
        assertTrue(result is ReadingResultStatus.ProgressUpdated)
        val updated = result as ReadingResultStatus.ProgressUpdated
        assertEquals(ReadingMastery.MASTERED, updated.progress.masteryLevel)
    }

    @Test
    fun `unlockExercise succeeds for existing exercise`() = runBlocking {
        val result = repository.unlockExercise("read_ex_greet_hello")
        assertTrue(result is ReadingResultStatus.Success)
    }

    @Test
    fun `unlock missing exercise returns error`() = runBlocking {
        val result = repository.unlockExercise("missing")
        assertTrue(result is ReadingResultStatus.Error)
    }

    @Test
    fun `recordStreak updates streak`() = runBlocking {
        val result = repository.recordStreak(3)
        assertTrue(result is ReadingResultStatus.StreakUpdated)
        val streak = result as ReadingResultStatus.StreakUpdated
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
    }

    @Test
    fun `awardBadge marks badge earned once`() = runBlocking {
        val result = repository.awardBadge("read_first")
        assertTrue(result is ReadingResultStatus.BadgeEarned)
        assertTrue((result as ReadingResultStatus.BadgeEarned).badge.isEarned)

        val badges = repository.getReadingBadges().first()
        assertTrue(badges.any { it.id == "read_first" && it.isEarned })

        val duplicate = repository.awardBadge("read_first")
        assertTrue(duplicate is ReadingResultStatus.Error)
    }

    @Test
    fun `recordReveal increments reveal count`() = runBlocking {
        val result = repository.recordReveal("greet_001")
        assertTrue(result is ReadingResultStatus.RevealRecorded)
        val reveal = result as ReadingResultStatus.RevealRecorded
        assertEquals("greet_001", reveal.wordId)
        assertEquals(1, reveal.revealCount)

        val statistics = repository.getReadingStatistics().first()
        assertEquals(1, statistics.totalReveals)
    }

    @Test
    fun `addExercises adds only new exercises`() = runBlocking {
        val custom = ReadingExercise(
            id = "read_ex_custom",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "早",
            pinyin = "zǎo",
            english = "Morning",
            prompt = "选一选",
            choices = listOf(ReadingChoice("c0", "早", "zǎo", "早")),
            correctChoiceIndex = 0,
        )
        repository.addExercises(listOf(custom))
        assertNotNull(repository.getExerciseById("read_ex_custom").first())
        assertEquals(15, repository.getAllExercises().first().size)
    }
}
