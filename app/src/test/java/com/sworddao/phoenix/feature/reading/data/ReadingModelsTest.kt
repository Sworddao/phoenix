package com.sworddao.phoenix.feature.reading.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingModelsTest {

    @Test
    fun `ReadingExercise correctChoice returns the indexed choice`() {
        val exercise = ReadingExercise(
            id = "ex_1",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好",
            pinyin = "nǐ hǎo",
            english = "Hello",
            prompt = "p",
            choices = listOf(
                ReadingChoice("choice_0", "你好", "nǐ hǎo", "你好"),
                ReadingChoice("choice_1", "谢谢", "xiè xie", "谢谢"),
            ),
            correctChoiceIndex = 1,
        )

        assertEquals("choice_1", exercise.correctChoice?.id)
    }

    @Test
    fun `ReadingExercise correctChoice is null for out of range index`() {
        val exercise = ReadingExercise(
            id = "ex_1",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好",
            pinyin = "nǐ hǎo",
            english = "Hello",
            prompt = "p",
            choices = listOf(ReadingChoice("choice_0", "你好", "nǐ hǎo", "你好")),
            correctChoiceIndex = 5,
        )

        assertNull(exercise.correctChoice)
    }

    @Test
    fun `ReadingExercise card carries hanzi and tone info`() {
        val exercise = ReadingExercise(
            id = "ex_1",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好",
            pinyin = "nǐ hǎo",
            english = "Hello",
            syllableTones = listOf(3, 3),
            prompt = "p",
            choices = listOf(ReadingChoice("choice_0", "你好")),
            relatedWordId = "greet_001",
        )

        val card = exercise.card
        assertEquals("card_ex_1", card.id)
        assertEquals("你好", card.hanzi)
        assertEquals("nǐ hǎo", card.pinyin)
        assertEquals("greet_001", card.wordId)
        assertEquals(listOf(3, 3), card.syllableTones)
    }

    @Test
    fun `ReadingSession progress and counts`() {
        val session = ReadingSession(
            exerciseIds = listOf("a", "b", "c", "d"),
            currentExerciseIndex = 1,
            attempts = listOf(
                ReadingAttempt(exerciseId = "a", chosenChoiceId = "x", wasCorrect = true, revealedHanziBeforeAnswer = true),
                ReadingAttempt(exerciseId = "b", chosenChoiceId = "x", wasCorrect = false, revealedHanziBeforeAnswer = false),
                ReadingAttempt(exerciseId = "c", chosenChoiceId = "x", wasCorrect = true, revealedHanziBeforeAnswer = false),
            ),
        )

        assertEquals("b", session.currentExerciseId)
        assertEquals(0.25f, session.progress, 0.001f)
        assertEquals(2, session.correctAttempts)
        assertEquals(1, session.revealCount)
        assertFalse(session.isCompleted)
        assertEquals(0, session.totalXpEarned)
    }

    @Test
    fun `ReadingResult celebration and feedback`() {
        val exercise = ReadingExercise(
            id = "ex_1",
            type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
            difficulty = ReadingDifficulty.BEGINNER,
            hanzi = "你好",
            pinyin = "nǐ hǎo",
            english = "Hello",
            prompt = "p",
            choices = listOf(ReadingChoice("choice_0", "你好")),
        )

        val correct = ReadingResult(
            attempt = ReadingAttempt(exerciseId = "ex_1", chosenChoiceId = "choice_0", wasCorrect = true),
            exercise = exercise,
            isNewPersonalBest = true,
            streakContinued = false,
        )
        assertTrue(correct.shouldCelebrate)
        assertTrue(correct.feedbackMessage.isNotEmpty())

        val plainCorrect = correct.copy(isNewPersonalBest = false, streakContinued = false)
        assertFalse(plainCorrect.shouldCelebrate)

        val firstWordRead = correct.copy(
            isNewPersonalBest = false,
            reward = ReadingReward(isFirstWordRead = true),
        )
        assertTrue(firstWordRead.shouldCelebrate)

        val failed = ReadingResult(
            attempt = ReadingAttempt(exerciseId = "ex_1", chosenChoiceId = "choice_0", wasCorrect = false),
            exercise = exercise,
            isNewPersonalBest = true,
        )
        assertFalse(failed.shouldCelebrate)
    }

    @Test
    fun `ReadingStatistics overallAccuracy`() {
        assertEquals(0f, ReadingStatistics().overallAccuracy, 0.001f)

        val stats = ReadingStatistics(totalAttempts = 10, correctAttempts = 7)
        assertEquals(0.7f, stats.overallAccuracy, 0.001f)
    }

    @Test
    fun `ReadingMastery thresholds progress in order`() {
        val levels = ReadingMastery.entries.map { it.level }
        assertEquals(levels.sorted(), levels)
        assertEquals(ReadingMastery.NEW.level, 0)
        assertEquals(ReadingMastery.MASTERED.level, 5)
        assertEquals(0.85f, ReadingMastery.MASTERED.requiredSuccessRate, 0.001f)
        assertEquals(15, ReadingMastery.MASTERED.minAttempts)
    }

    @Test
    fun `ReadingDifficulty has four levels in order`() {
        assertEquals(4, ReadingDifficulty.entries.size)
        assertEquals(ReadingDifficulty.BEGINNER.level, 1)
        assertEquals(ReadingDifficulty.ADVANCED.level, 4)
    }

    @Test
    fun `ReadingExerciseType has eight exercise types`() {
        assertEquals(8, ReadingExerciseType.entries.size)
        assertTrue(ReadingExerciseType.entries.containsAll(
            listOf(
                ReadingExerciseType.MATCH_SPOKEN_TO_WRITTEN,
                ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
                ReadingExerciseType.MATCH_HANZI_TO_MEANING,
                ReadingExerciseType.SENTENCE_READING,
                ReadingExerciseType.PHRASE_RECOGNITION,
                ReadingExerciseType.CHARACTER_RECOGNITION,
                ReadingExerciseType.CONTEXT_READING,
                ReadingExerciseType.NPC_DIALOGUE_READING,
            )
        ))
    }

    @Test
    fun `CharacterRevealState has seven states`() {
        assertEquals(7, CharacterRevealState.entries.size)
        assertTrue(CharacterRevealState.entries.containsAll(
            listOf(
                CharacterRevealState.HIDDEN,
                CharacterRevealState.PINYIN_ONLY,
                CharacterRevealState.HANZI_ONLY,
                CharacterRevealState.HANZI_AND_PINYIN,
                CharacterRevealState.TONE_COLORED_PINYIN,
                CharacterRevealState.TAP_TO_REVEAL,
                CharacterRevealState.AUTO_REVEAL,
            )
        ))
    }

    @Test
    fun `ReadingBadge catalog has eight badges`() {
        assertEquals(8, ReadingBadge.ALL_BADGES.size)
        assertNotNull(ReadingBadge.getBadge("read_first"))
        assertNotNull(ReadingBadge.getBadge("read_streak_30"))
        assertNotNull(ReadingBadge.getBadge("read_char_collector"))
        assertNull(ReadingBadge.getBadge("missing_badge"))
    }

    @Test
    fun `ReadingProgress success rate and mastery`() {
        val progress = ReadingProgress(
            itemId = "greet_001",
            wordId = "greet_001",
            totalAttempts = 4,
            correctAttempts = 3,
            masteryLevel = ReadingMastery.LEARNING,
        )
        assertEquals(0.75f, progress.successRate, 0.001f)
        assertFalse(progress.isMastered)

        val mastered = progress.copy(
            totalAttempts = 20,
            correctAttempts = 18,
            masteryLevel = ReadingMastery.MASTERED,
        )
        assertTrue(mastered.isMastered)
    }

    @Test
    fun `ToneColor has five colors with neutral default`() {
        assertEquals(5, ToneColor.entries.size)
        assertEquals(ToneColor.NEUTRAL, ToneColor.NEUTRAL)
        assertTrue(ToneColor.entries.containsAll(
            listOf(
                ToneColor.NEUTRAL,
                ToneColor.TONE1,
                ToneColor.TONE2,
                ToneColor.TONE3,
                ToneColor.TONE4,
            )
        ))
    }
}
