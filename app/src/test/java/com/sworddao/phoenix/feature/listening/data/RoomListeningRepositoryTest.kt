package com.sworddao.phoenix.feature.listening.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
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
class RoomListeningRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomListeningRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        val pronunciation = RoomPronunciationRepository(
            database.speakingDao(),
            MockVocabularyRepository(),
            MockQuestRepository(),
            MockFriendshipRepository(),
            MockGameProgressRepository(),
            MockPassportRepository(),
        )
        repository = RoomListeningRepository(
            database.listeningDao(),
            MockVocabularyRepository(),
            MockQuestRepository(),
            MockFriendshipRepository(),
            MockGameProgressRepository(),
            MockPassportRepository(),
            pronunciation,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeds initial listening exercises`() = runBlocking {
        val exercises = repository.getAllExercises().first()
        assertEquals(14, exercises.size)
        assertNotNull(exercises.first { it.id == "listen_ex_greet_hello" }.clip)
    }

    @Test
    fun `getExerciseById returns null for missing`() = runBlocking {
        assertNull(repository.getExerciseById("missing").first())
    }

    @Test
    fun `filters exercises by type difficulty and word`() = runBlocking {
        val type = repository.getExercisesByType(ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY).first()
        assertTrue(type.isNotEmpty())
        assertTrue(type.all { it.type == ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY })

        val beginner = repository.getExercisesByDifficulty(ListeningDifficulty.BEGINNER).first()
        assertTrue(beginner.isNotEmpty())
        assertTrue(beginner.all { it.difficulty == ListeningDifficulty.BEGINNER })

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

        val recommended = repository.getRecommendedExercises(4).first()
        assertEquals(4, recommended.size)
    }

    @Test
    fun `initial statistics are empty`() = runBlocking {
        val statistics = repository.getListeningStatistics().first()
        assertEquals(0, statistics.totalAttempts)
        assertEquals(0, statistics.correctAttempts)
        assertTrue(repository.getListeningBadges().first().isNotEmpty())
    }

    @Test
    fun `submit correct answer records progress and unlocks speaking`() = runBlocking {
        val session = repository.startSession(
            ListeningSessionConfig(wordIds = listOf("greet_001"))
        )
        assertEquals(listOf("listen_ex_greet_hello"), session.exerciseIds)

        val result = repository.submitAnswer(
            ListeningAttempt(
                exerciseId = "listen_ex_greet_hello",
                wordId = "greet_001",
                chosenChoiceId = "choice_0",
                wasCorrect = true,
            )
        )
        assertTrue(result is ListeningResultStatus.ExerciseCompleted)
        val completed = result as ListeningResultStatus.ExerciseCompleted
        assertEquals(10, completed.result.xpEarned)
        assertEquals(1, completed.result.currentStreak)

        val progress = repository.getListeningProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.correctAttempts)

        val statistics = repository.getListeningStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.correctAttempts)
        assertEquals(1, statistics.wordsPracticed)
    }

    @Test
    fun `wrong answer does not award xp`() = runBlocking {
        repository.startSession(ListeningSessionConfig(wordIds = listOf("greet_001")))
        val result = repository.submitAnswer(
            ListeningAttempt(
                exerciseId = "listen_ex_greet_hello",
                wordId = "greet_001",
                chosenChoiceId = "choice_1",
                wasCorrect = false,
            )
        )
        val completed = result as ListeningResultStatus.ExerciseCompleted
        assertEquals(0, completed.result.xpEarned)
        assertEquals(0, completed.result.currentStreak)
    }

    @Test
    fun `submit without active session returns error`() = runBlocking {
        val result = repository.submitAnswer(
            ListeningAttempt(exerciseId = "listen_ex_greet_hello", chosenChoiceId = "choice_0", wasCorrect = true)
        )
        assertTrue(result is ListeningResultStatus.Error)
    }

    @Test
    fun `completeSession increments session counter`() = runBlocking {
        val session = repository.startSession(ListeningSessionConfig())
        val result = repository.completeSession(session)
        assertTrue(result is ListeningResultStatus.SessionCompleted)
        val completed = result as ListeningResultStatus.SessionCompleted
        assertTrue(completed.session.isCompleted)
        assertEquals(1, completed.statistics.totalSessions)
    }

    @Test
    fun `recordReplay increments replay count`() = runBlocking {
        val result = repository.recordReplay("listen_ex_greet_hello")
        assertTrue(result is ListeningResultStatus.ReplayRecorded)
        val replay = result as ListeningResultStatus.ReplayRecorded
        assertEquals("listen_ex_greet_hello", replay.exerciseId)
    }

    @Test
    fun `updateProgress computes mastery`() = runBlocking {
        val result = repository.updateProgress(
            ListeningProgress(
                itemId = "listen_ex_greet_hello",
                totalAttempts = 20,
                correctAttempts = 20,
            )
        )
        assertTrue(result is ListeningResultStatus.ProgressUpdated)
        val updated = result as ListeningResultStatus.ProgressUpdated
        assertEquals(ListeningMastery.MASTERED, updated.progress.masteryLevel)
    }

    @Test
    fun `unlockExercise unlocks a locked exercise`() = runBlocking {
        repository.unlockExercise("listen_ex_greet_hello")
        val result = repository.unlockExercise("listen_ex_greet_hello")
        assertTrue(result is ListeningResultStatus.Success)
    }

    @Test
    fun `recordStreak updates streak`() = runBlocking {
        val result = repository.recordStreak(3)
        assertTrue(result is ListeningResultStatus.StreakUpdated)
        val streak = result as ListeningResultStatus.StreakUpdated
        assertEquals(3, streak.currentStreak)
        assertEquals(3, streak.longestStreak)
    }

    @Test
    fun `awardBadge marks badge earned once`() = runBlocking {
        val result = repository.awardBadge("listen_first")
        assertTrue(result is ListeningResultStatus.BadgeEarned)
        assertTrue((result as ListeningResultStatus.BadgeEarned).badge.isEarned)

        val badges = repository.getListeningBadges().first()
        assertTrue(badges.any { it.id == "listen_first" && it.isEarned })

        val duplicate = repository.awardBadge("listen_first")
        assertTrue(duplicate is ListeningResultStatus.Error)
    }

    @Test
    fun `addExercises adds only new exercises`() = runBlocking {
        val custom = ListeningExercise(
            id = "listen_ex_custom",
            type = ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY,
            difficulty = ListeningDifficulty.BEGINNER,
            clip = AudioClip(id = "clip_custom", text = "zǎo", hanzi = "早", english = "Morning"),
            prompt = "选一选",
            choices = listOf(ListeningChoice("c0", "zǎo", "早")),
            correctChoiceIndex = 0,
        )
        repository.addExercises(listOf(custom))
        assertNotNull(repository.getExerciseById("listen_ex_custom").first())
        assertEquals(15, repository.getAllExercises().first().size)
    }
}
