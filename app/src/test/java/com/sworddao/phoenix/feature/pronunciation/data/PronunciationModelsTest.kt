package com.sworddao.phoenix.feature.pronunciation.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationModelsTest {

    @Test
    fun `PronunciationAttempt overallScore averages confidence tone and fluency`() {
        val attempt = PronunciationAttempt(
            exerciseId = "ex_1",
            wordId = "w_1",
            phraseId = null,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
            confidence = 0.9f,
            toneAccuracy = 0.8f,
            fluencyScore = 0.7f,
        )

        assertEquals(0.8f, attempt.overallScore, 0.001f)
    }

    @Test
    fun `PronunciationAttempt isHighConfidence requires confidence and tone`() {
        val highConfidence = PronunciationAttempt(
            exerciseId = "ex_1",
            wordId = "w_1",
            phraseId = null,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
            confidence = 0.8f,
            toneAccuracy = 0.7f,
        )
        assertTrue(highConfidence.isHighConfidence)

        val lowTone = highConfidence.copy(toneAccuracy = 0.5f)
        assertFalse(lowTone.isHighConfidence)

        val lowConfidence = highConfidence.copy(confidence = 0.6f)
        assertFalse(lowConfidence.isHighConfidence)
    }

    @Test
    fun `PronunciationSession progress follows exercise index`() {
        val session = PronunciationSession(
            exerciseIds = listOf("a", "b", "c", "d"),
            currentExerciseIndex = 2,
        )

        assertEquals("c", session.currentExerciseId)
        assertEquals(0.5f, session.progress, 0.001f)
    }

    @Test
    fun `PronunciationSession averageConfidence averages attempts`() {
        val session = PronunciationSession(
            exerciseIds = listOf("a"),
            attempts = listOf(
                PronunciationAttempt("", "a", null, null, "", "", confidence = 0.8f),
                PronunciationAttempt("", "a", null, null, "", "", confidence = 1.0f),
            ),
        )

        assertEquals(0.9f, session.averageConfidence, 0.001f)
        assertEquals(0f, PronunciationSession(exerciseIds = emptyList()).averageConfidence, 0.001f)
    }

    @Test
    fun `PronunciationSession successfulAttempts counts wins`() {
        val session = PronunciationSession(
            exerciseIds = listOf("a"),
            attempts = listOf(
                PronunciationAttempt("", "a", null, null, "", "", wasSuccessful = true),
                PronunciationAttempt("", "a", null, null, "", "", wasSuccessful = false),
                PronunciationAttempt("", "a", null, null, "", "", wasSuccessful = true),
            ),
        )

        assertEquals(2, session.successfulAttempts)
    }

    @Test
    fun `PronunciationResult shouldCelebrate on personal best or streak`() {
        val attempt = PronunciationAttempt(
            exerciseId = "ex_1",
            wordId = "w_1",
            phraseId = null,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
            wasSuccessful = true,
        )
        val exercise = SpeakingExercise(
            id = "ex_1",
            type = SpeakingExerciseType.VOCABULARY_WORD,
            difficulty = SpeakingDifficulty.BEGINNER,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
        )

        assertTrue(
            PronunciationResult(attempt, exercise, isNewPersonalBest = true).shouldCelebrate
        )
        assertTrue(
            PronunciationResult(attempt, exercise, streakContinued = true).shouldCelebrate
        )
        assertFalse(
            PronunciationResult(attempt, exercise).shouldCelebrate
        )
    }

    @Test
    fun `PronunciationResult feedbackMessage comes from attempt`() {
        val attempt = PronunciationAttempt(
            exerciseId = "ex_1",
            wordId = "w_1",
            phraseId = null,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
            feedbackType = PronunciationFeedbackType.GREAT_START,
        )
        val exercise = SpeakingExercise(
            id = "ex_1",
            type = SpeakingExerciseType.VOCABULARY_WORD,
            difficulty = SpeakingDifficulty.BEGINNER,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
        )

        assertEquals("Great start!", PronunciationResult(attempt, exercise).feedbackMessage)
    }

    @Test
    fun `SpeakingExercise displayText prefers hanzi`() {
        val withHanzi = SpeakingExercise(
            id = "ex_1",
            type = SpeakingExerciseType.VOCABULARY_WORD,
            difficulty = SpeakingDifficulty.BEGINNER,
            expectedText = "nǐ hǎo",
            expectedPinyin = "nǐ hǎo",
            expectedHanzi = "你好",
        )
        assertEquals("你好", withHanzi.displayText)

        val withoutHanzi = withHanzi.copy(expectedHanzi = null)
        assertEquals("nǐ hǎo", withoutHanzi.displayText)
    }

    @Test
    fun `SpeakingMastery progression reflects success rate and attempt minimums`() {
        assertTrue(SpeakingMastery.MASTERED.level > SpeakingMastery.CONFIDENT.level)
        assertEquals(0.85f, SpeakingMastery.MASTERED.requiredSuccessRate, 0.001f)
        assertEquals(20, SpeakingMastery.MASTERED.minAttempts)
        assertEquals(10, SpeakingMastery.CONFIDENT.minAttempts)
    }

    @Test
    fun `PronunciationProgress successRate and isMastered`() {
        val progress = PronunciationProgress(
            wordId = "w_1",
            totalAttempts = 10,
            successfulAttempts = 9,
            masteryLevel = SpeakingMastery.CONFIDENT,
        )

        assertEquals(0.9f, progress.successRate, 0.001f)
        assertFalse(progress.isMastered)

        val mastered = progress.copy(
            totalAttempts = 20,
            successfulAttempts = 20,
            masteryLevel = SpeakingMastery.MASTERED,
        )
        assertTrue(mastered.isMastered)
    }

    @Test
    fun `PronunciationProgress defaults to NEW mastery`() {
        val progress = PronunciationProgress(wordId = "w_1")
        assertEquals(SpeakingMastery.NEW, progress.masteryLevel)
        assertEquals(0f, progress.successRate, 0.001f)
    }

    @Test
    fun `SpeakingStatistics overallSuccessRate`() {
        val stats = SpeakingStatistics(totalAttempts = 10, successfulAttempts = 6)
        assertEquals(0.6f, stats.overallSuccessRate, 0.001f)
        assertEquals(0f, SpeakingStatistics().overallSuccessRate, 0.001f)
    }

    @Test
    fun `PronunciationBadge catalog contains all badge ids`() {
        val ids = PronunciationBadge.ALL_BADGES.map { it.id }

        assertEquals(8, ids.size)
        assertTrue(ids.containsAll(
            listOf("first_word", "streak_3", "streak_7", "streak_30", "confident_speaker", "tone_master", "conversation_ready", "pronunciation_pro")
        ))
        assertNotNull(PronunciationBadge.getBadge("streak_7"))
        assertNull(PronunciationBadge.getBadge("unknown_badge"))
    }

    @Test
    fun `SpeakingQuestObjective tracks progress and completion`() {
        val objective = SpeakingQuestObjective(
            exerciseType = SpeakingExerciseType.DIALOGUE_PHRASE,
            difficulty = SpeakingDifficulty.BEGINNER,
            targetCount = 3,
            currentCount = 2,
        )

        assertFalse(objective.isComplete)
        assertEquals(2f / 3f, objective.progress, 0.001f)

        assertTrue(objective.copy(currentCount = 3).isComplete)
    }

    @Test
    fun `PronunciationSessionConfig defaults are sensible`() {
        val config = PronunciationSessionConfig()
        assertEquals(SpeakingExerciseType.REPEAT_AFTER_NPC, config.exerciseType)
        assertEquals(SpeakingDifficulty.BEGINNER, config.difficulty)
        assertEquals(5, config.exerciseCount)
        assertTrue(config.enableTonePractice)
    }
}