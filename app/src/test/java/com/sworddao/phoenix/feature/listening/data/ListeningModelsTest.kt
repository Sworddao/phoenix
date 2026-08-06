package com.sworddao.phoenix.feature.listening.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningModelsTest {

    @Test
    fun `ListeningExercise correctChoice returns the indexed choice`() {
        val exercise = ListeningExercise(
            id = "ex_1",
            type = ListeningExerciseType.HEAR_AND_CHOOSE_MEANING,
            difficulty = ListeningDifficulty.BEGINNER,
            clip = AudioClip(id = "clip_1", text = "nǐ hǎo", english = "Hello"),
            prompt = "p",
            choices = listOf(
                ListeningChoice("choice_0", "Hello"),
                ListeningChoice("choice_1", "Goodbye"),
                ListeningChoice("choice_2", "Thanks"),
            ),
            correctChoiceIndex = 1,
        )

        assertEquals("choice_1", exercise.correctChoice?.id)
    }

    @Test
    fun `ListeningExercise correctChoice is null for out of range index`() {
        val exercise = ListeningExercise(
            id = "ex_1",
            type = ListeningExerciseType.HEAR_AND_CHOOSE_MEANING,
            difficulty = ListeningDifficulty.BEGINNER,
            clip = AudioClip(id = "clip_1", text = "nǐ hǎo", english = "Hello"),
            prompt = "p",
            choices = listOf(ListeningChoice("choice_0", "Hello")),
            correctChoiceIndex = 5,
        )

        assertNull(exercise.correctChoice)
    }

    @Test
    fun `AudioClip displayText prefers hanzi`() {
        val clip = AudioClip(
            id = "clip_1",
            text = "nǐ hǎo",
            hanzi = "你好",
            english = "Hello",
        )
        assertEquals("你好", clip.displayText)

        val plain = AudioClip(id = "clip_2", text = "yī", english = "one")
        assertEquals("yī", plain.displayText)
    }

    @Test
    fun `ListeningSession progress and counts`() {
        val session = ListeningSession(
            exerciseIds = listOf("a", "b", "c", "d"),
            currentExerciseIndex = 1,
            attempts = listOf(
                ListeningAttempt(exerciseId = "a", chosenChoiceId = "x", wasCorrect = true, replayCount = 2),
                ListeningAttempt(exerciseId = "b", chosenChoiceId = "x", wasCorrect = false, replayCount = 1),
                ListeningAttempt(exerciseId = "c", chosenChoiceId = "x", wasCorrect = true, replayCount = 0),
            ),
        )

        assertEquals("b", session.currentExerciseId)
        assertEquals(0.25f, session.progress, 0.001f)
        assertEquals(2, session.correctAttempts)
        assertEquals(3, session.totalReplayCount)
        assertFalse(session.isCompleted)
    }

    @Test
    fun `ListeningResult celebration and feedback`() {
        val exercise = ListeningExercise(
            id = "ex_1",
            type = ListeningExerciseType.HEAR_AND_CHOOSE_MEANING,
            difficulty = ListeningDifficulty.BEGINNER,
            clip = AudioClip(id = "clip_1", text = "nǐ hǎo", english = "Hello"),
            prompt = "p",
            choices = listOf(ListeningChoice("choice_0", "Hello")),
        )

        val correct = ListeningResult(
            attempt = ListeningAttempt(exerciseId = "ex_1", chosenChoiceId = "choice_0", wasCorrect = true),
            exercise = exercise,
            isNewPersonalBest = true,
            streakContinued = false,
        )
        assertTrue(correct.shouldCelebrate)
        assertTrue(correct.feedbackMessage.isNotEmpty())

        val plainCorrect = correct.copy(isNewPersonalBest = false, streakContinued = false)
        assertFalse(plainCorrect.shouldCelebrate)

        val failed = ListeningResult(
            attempt = ListeningAttempt(exerciseId = "ex_1", chosenChoiceId = "choice_0", wasCorrect = false),
            exercise = exercise,
            isNewPersonalBest = true,
        )
        assertFalse(failed.shouldCelebrate)
    }

    @Test
    fun `ListeningStatistics overallAccuracy`() {
        assertEquals(0f, ListeningStatistics().overallAccuracy, 0.001f)

        val stats = ListeningStatistics(totalAttempts = 10, correctAttempts = 7)
        assertEquals(0.7f, stats.overallAccuracy, 0.001f)
    }

    @Test
    fun `ListeningMastery thresholds progress in order`() {
        val levels = ListeningMastery.entries.map { it.level }
        assertEquals(levels.sorted(), levels)
        assertEquals(ListeningMastery.NEW.level, 0)
        assertEquals(ListeningMastery.MASTERED.level, 4)
        assertEquals(0.85f, ListeningMastery.MASTERED.requiredSuccessRate, 0.001f)
    }

    @Test
    fun `ListeningDifficulty has four levels in order`() {
        assertEquals(4, ListeningDifficulty.entries.size)
        assertEquals(ListeningDifficulty.BEGINNER.level, 1)
        assertEquals(ListeningDifficulty.ADVANCED.level, 4)
    }

    @Test
    fun `ListeningExerciseType has eight exercise types`() {
        assertEquals(8, ListeningExerciseType.entries.size)
        assertTrue(ListeningExerciseType.entries.containsAll(
            listOf(
                ListeningExerciseType.HEAR_AND_CHOOSE_MEANING,
                ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY,
                ListeningExerciseType.HEAR_AND_MATCH_IMAGE,
                ListeningExerciseType.HEAR_AND_CHOOSE_NPC_RESPONSE,
                ListeningExerciseType.HEAR_NUMBERS,
                ListeningExerciseType.HEAR_GREETINGS,
                ListeningExerciseType.HEAR_DIRECTIONS,
                ListeningExerciseType.HEAR_FOOD_ORDERS,
            )
        ))
    }

    @Test
    fun `ListeningBadge catalog has eight badges`() {
        assertEquals(8, ListeningBadge.ALL_BADGES.size)
        assertNotNull(ListeningBadge.getBadge("listen_first"))
        assertNotNull(ListeningBadge.getBadge("listen_streak_30"))
        assertNull(ListeningBadge.getBadge("missing_badge"))
    }

    @Test
    fun `ListeningProgress success rate and mastery`() {
        val progress = ListeningProgress(
            itemId = "greet_001",
            wordId = "greet_001",
            totalAttempts = 4,
            correctAttempts = 3,
            masteryLevel = ListeningMastery.LEARNING,
        )
        assertEquals(0.75f, progress.successRate, 0.001f)
        assertFalse(progress.isMastered)

        val mastered = progress.copy(
            totalAttempts = 20,
            correctAttempts = 18,
            masteryLevel = ListeningMastery.MASTERED,
        )
        assertTrue(mastered.isMastered)
    }
}