package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomPronunciationRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomPronunciationRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomPronunciationRepository(
            database.speakingDao(),
            MockVocabularyRepository(),
            MockQuestRepository(),
            MockFriendshipRepository(),
            MockGameProgressRepository(),
            MockPassportRepository(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeds initial speaking exercises`() = runBlocking {
        val exercises = repository.getAllExercises().first()
        assertEquals(11, exercises.size)
        assertEquals("nǐ hǎo", exercises.first { it.id == "pron_ex_greet_hello" }.expectedText)
    }

    @Test
    fun `getExerciseById returns null for missing exercise`() = runBlocking {
        assertNull(repository.getExerciseById("missing").first())
    }

    @Test
    fun `filters exercises by type and difficulty`() = runBlocking {
        val tone = repository.getExercisesByType(SpeakingExerciseType.TONE_PRACTICE).first()
        assertEquals(1, tone.size)
        assertEquals("pron_ex_tone_hello", tone.first().id)

        val beginner = repository.getExercisesByDifficulty(SpeakingDifficulty.BEGINNER).first()
        assertEquals(8, beginner.size)
    }

    @Test
    fun `getExercisesByWord filters by word`() = runBlocking {
        val byWord = repository.getExercisesByWord("greet_001").first()
        assertTrue(byWord.all { it.wordId == "greet_001" })
        assertEquals(listOf("pron_ex_greet_hello"), byWord.map { it.id })
    }

    @Test
    fun `getExercisesByPhrase filters by phrase`() = runBlocking {
        val byPhrase = repository.getExercisesByPhrase("grandma_mei_hao_chi").first()
        assertEquals(listOf("pron_ex_dlg_hao_chi"), byPhrase.map { it.id })
    }

    @Test
    fun `getExercisesByNpc filters by npc`() = runBlocking {
        val byNpc = repository.getExercisesByNpc("grandma_mei").first()
        assertTrue(byNpc.isNotEmpty())
        assertTrue(byNpc.all { it.relatedNpcId == "grandma_mei" })
    }

    @Test
    fun `unlocked exercises exclude locked seeds`() = runBlocking {
        val unlocked = repository.getUnlockedExercises().first()
        assertEquals(7, unlocked.size)
        assertTrue(unlocked.all { it.isUnlocked })
    }

    @Test
    fun `recommended exercises respects limit`() = runBlocking {
        val recommended = repository.getRecommendedExercises(3).first()
        assertEquals(3, recommended.size)
    }

    @Test
    fun `initial progress and statistics are empty`() = runBlocking {
        assertNull(repository.getPronunciationProgress("greet_001").first())
        assertTrue(repository.getAllPronunciationProgress().first().isEmpty())
        val statistics = repository.getSpeakingStatistics().first()
        assertEquals(0, statistics.totalAttempts)
        assertEquals(0, statistics.wordsPracticed)
        assertTrue(statistics.pronunciationBadges.isNotEmpty())
    }

    @Test
    fun `start session selects exercises and submits success`() = runBlocking {
        val session = repository.startSession(
            PronunciationSessionConfig(wordIds = listOf("greet_001", "greet_003"))
        )
        assertEquals(listOf("pron_ex_greet_hello", "pron_ex_greet_thanks"), session.exerciseIds)

        val result = repository.submitAttempt(
            PronunciationAttempt(
                exerciseId = "pron_ex_greet_hello",
                wordId = "greet_001",
                phraseId = null,
                expectedText = "nǐ hǎo",
                expectedPinyin = "nǐ hǎo",
                spokenText = "nǐ hǎo",
                confidence = 0.9f,
                toneAccuracy = 0.95f,
                fluencyScore = 0.9f,
                wasSuccessful = true,
            )
        )
        assertTrue(result is PronunciationResultStatus.ExerciseCompleted)
        val completed = result as PronunciationResultStatus.ExerciseCompleted
        assertEquals(10, completed.result.xpEarned)
        assertEquals(1, completed.result.currentStreak)

        val progress = repository.getPronunciationProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.successfulAttempts)
        assertEquals(0.9f, progress?.bestConfidence ?: 0f, 0.001f)

        val statistics = repository.getSpeakingStatistics().first()
        assertEquals(1, statistics.totalAttempts)
        assertEquals(1, statistics.successfulAttempts)
        assertEquals(1, statistics.wordsPracticed)
        assertEquals(SpeakingExerciseType.VOCABULARY_WORD to 1, statistics.exercisesByType.entries.first().toPair())
    }

    @Test
    fun `failed attempt does not award xp`() = runBlocking {
        repository.startSession(PronunciationSessionConfig(wordIds = listOf("greet_001")))
        val result = repository.submitAttempt(
            PronunciationAttempt(
                exerciseId = "pron_ex_greet_hello",
                wordId = "greet_001",
                phraseId = null,
                expectedText = "nǐ hǎo",
                expectedPinyin = "nǐ hǎo",
                spokenText = "x",
                confidence = 0.2f,
                wasSuccessful = false,
            )
        )
        val completed = result as PronunciationResultStatus.ExerciseCompleted
        assertEquals(0, completed.result.xpEarned)
        assertEquals(0, completed.result.currentStreak)
    }

    @Test
    fun `submit without active session returns error`() = runBlocking {
        val result = repository.submitAttempt(
            PronunciationAttempt(
                exerciseId = "pron_ex_greet_hello",
                wordId = "greet_001",
                phraseId = null,
                expectedText = "nǐ hǎo",
                expectedPinyin = "nǐ hǎo",
                wasSuccessful = true,
            )
        )
        assertTrue(result is PronunciationResultStatus.Error)
    }

    @Test
    fun `completeSession marks session complete and increments counter`() = runBlocking {
        val session = repository.startSession(PronunciationSessionConfig())
        val result = repository.completeSession(session)
        assertTrue(result is PronunciationResultStatus.SessionCompleted)
        val completed = result as PronunciationResultStatus.SessionCompleted
        assertTrue(completed.session.isCompleted)
        assertEquals(1, completed.statistics.totalSessions)
    }

    @Test
    fun `unlockExercise unlocks locked seed`() = runBlocking {
        val result = repository.unlockExercise("pron_ex_dlg_meet")
        assertTrue(result is PronunciationResultStatus.Success)
        assertTrue(repository.getUnlockedExercises().first().any { it.id == "pron_ex_dlg_meet" })
    }

    @Test
    fun `unlock missing exercise returns error`() = runBlocking {
        val result = repository.unlockExercise("missing")
        assertTrue(result is PronunciationResultStatus.Error)
    }

    @Test
    fun `updateProgress computes mastery`() = runBlocking {
        val result = repository.updateProgress(
            PronunciationProgress(
                wordId = "greet_001",
                totalAttempts = 20,
                successfulAttempts = 20,
            )
        )
        assertTrue(result is PronunciationResultStatus.ProgressUpdated)
        val updated = result as PronunciationResultStatus.ProgressUpdated
        assertEquals(SpeakingMastery.MASTERED, updated.progress.masteryLevel)
    }

    @Test
    fun `recordStreak updates current and longest streak`() = runBlocking {
        val result = repository.recordStreak(5)
        assertTrue(result is PronunciationResultStatus.StreakUpdated)
        val streak = result as PronunciationResultStatus.StreakUpdated
        assertEquals(5, streak.currentStreak)
        assertEquals(5, streak.longestStreak)
    }

    @Test
    fun `awardBadge marks badge earned`() = runBlocking {
        val result = repository.awardBadge("first_word")
        assertTrue(result is PronunciationResultStatus.BadgeEarned)
        val badge = (result as PronunciationResultStatus.BadgeEarned).badge
        assertTrue(badge.isEarned)
        assertNotNull(badge.earnedAt)

        val badges = repository.getPronunciationBadges().first()
        assertTrue(badges.any { it.id == "first_word" && it.isEarned })
    }

    @Test
    fun `awardBadge twice returns error`() = runBlocking {
        repository.awardBadge("first_word")
        val result = repository.awardBadge("first_word")
        assertTrue(result is PronunciationResultStatus.Error)
    }

    @Test
    fun `addExercises inserts new exercises only`() = runBlocking {
        val newExercise = SpeakingExercise(
            id = "pron_ex_custom",
            type = SpeakingExerciseType.FREESTYLE,
            difficulty = SpeakingDifficulty.INTERMEDIATE,
            expectedText = "nǐ chī le ma",
            expectedPinyin = "nǐ chī le ma",
            expectedHanzi = "你吃了吗",
        )
        val result = repository.addExercises(listOf(newExercise))
        assertTrue(result is PronunciationResultStatus.Success)
        assertNotNull(repository.getExerciseById("pron_ex_custom").first())

        repository.addExercises(listOf(newExercise))
        assertEquals(12, repository.getAllExercises().first().size)
    }

    @Test
    fun `evaluatePronunciationOffline builds attempt with similarity`() = runBlocking {
        val attempt = repository.evaluatePronunciationOffline("nǐ hǎo", "nǐ hǎo", "nǐ hǎo")
        assertEquals(1.0f, attempt.confidence, 0.001f)
        assertTrue(attempt.wasSuccessful)
    }

    @Test
    fun `evaluatePronunciation matches best similarity`() = runBlocking {
        val attempt = repository.evaluatePronunciation("nǐ hǎo", "nǐ hǎo", "audio")
        assertEquals(1.0f, attempt.confidence, 0.001f)
    }
}
