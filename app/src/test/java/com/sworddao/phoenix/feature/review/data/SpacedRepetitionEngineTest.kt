package com.sworddao.phoenix.feature.review.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionEngineTest {

    // ------------------------------------------------------------------
    // Intervals
    // ------------------------------------------------------------------

    private val minute = 60 * 1000L
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `interval stage zero is ten minutes`() {
        assertEquals(10 * minute, SpacedRepetitionEngine.intervalForStage(0))
    }

    @Test
    fun `interval stage one is one day`() {
        assertEquals(day, SpacedRepetitionEngine.intervalForStage(1))
    }

    @Test
    fun `interval stage two is three days`() {
        assertEquals(3 * day, SpacedRepetitionEngine.intervalForStage(2))
    }

    @Test
    fun `interval stage three is seven days`() {
        assertEquals(7 * day, SpacedRepetitionEngine.intervalForStage(3))
    }

    @Test
    fun `interval stage four is fourteen days`() {
        assertEquals(14 * day, SpacedRepetitionEngine.intervalForStage(4))
    }

    @Test
    fun `interval stage five is thirty days`() {
        assertEquals(30 * day, SpacedRepetitionEngine.intervalForStage(5))
    }

    @Test
    fun `interval stage six is ninety days`() {
        assertEquals(90 * day, SpacedRepetitionEngine.intervalForStage(6))
    }

    @Test
    fun `interval stage clamps above max to ninety days`() {
        assertEquals(90 * day, SpacedRepetitionEngine.intervalForStage(99))
    }

    @Test
    fun `interval stage clamps below zero to ten minutes`() {
        assertEquals(10 * minute, SpacedRepetitionEngine.intervalForStage(-3))
    }

    @Test
    fun `intervals are strictly increasing`() {
        for (stage in 0 until SpacedRepetitionEngine.MAX_STAGE) {
            assertTrue(
                SpacedRepetitionEngine.intervalForStage(stage + 1) >
                    SpacedRepetitionEngine.intervalForStage(stage)
            )
        }
    }

    // ------------------------------------------------------------------
    // Stage transitions
    // ------------------------------------------------------------------

    @Test
    fun `correct answer advances one stage`() {
        assertEquals(1, SpacedRepetitionEngine.nextStage(true, 0.8f, 0, 0))
        assertEquals(2, SpacedRepetitionEngine.nextStage(true, 0.8f, 1, 0))
    }

    @Test
    fun `perfect score advances two stages`() {
        assertEquals(2, SpacedRepetitionEngine.nextStage(true, 0.95f, 0, 0))
        assertEquals(3, SpacedRepetitionEngine.nextStage(true, 1f, 1, 0))
    }

    @Test
    fun `perfect score after failures only advances one`() {
        assertEquals(1, SpacedRepetitionEngine.nextStage(true, 1f, 0, 2))
    }

    @Test
    fun `incorrect answer drops two stages`() {
        assertEquals(1, SpacedRepetitionEngine.nextStage(false, 0f, 3, 0))
    }

    @Test
    fun `incorrect answer never drops below zero`() {
        assertEquals(0, SpacedRepetitionEngine.nextStage(false, 0f, 1, 0))
        assertEquals(0, SpacedRepetitionEngine.nextStage(false, 0f, 0, 0))
    }

    @Test
    fun `stage caps at max`() {
        assertEquals(SpacedRepetitionEngine.MAX_STAGE, SpacedRepetitionEngine.nextStage(true, 0.8f, SpacedRepetitionEngine.MAX_STAGE, 0))
        assertEquals(SpacedRepetitionEngine.MAX_STAGE, SpacedRepetitionEngine.nextStage(true, 1f, SpacedRepetitionEngine.MAX_STAGE, 0))
    }

    // ------------------------------------------------------------------
    // Memory adjustments
    // ------------------------------------------------------------------

    @Test
    fun `correct answer increases strength`() {
        val initial = MemoryStrength()
        val updated = SpacedRepetitionEngine.adjustMemory(initial, true, 1f)
        assertTrue(updated.strength > initial.strength)
        assertEquals(1, updated.correctAnswers)
        assertEquals(0, updated.consecutiveFailures)
        assertEquals(1, updated.streak)
    }

    @Test
    fun `incorrect answer decreases strength`() {
        val initial = MemoryStrength(strength = 0.6f, confidence = 0.7f, correctAnswers = 2, reviewCount = 2)
        val updated = SpacedRepetitionEngine.adjustMemory(initial, false, 0f)
        assertTrue(updated.strength < initial.strength)
        assertEquals(1, updated.incorrectAnswers)
        assertEquals(1, updated.consecutiveFailures)
        assertEquals(0, updated.streak)
    }

    @Test
    fun `incorrect answer lowers confidence floor`() {
        val updated = SpacedRepetitionEngine.adjustMemory(MemoryStrength(), false, 0f)
        assertTrue(updated.confidence < MemoryStrength().confidence)
        assertTrue(updated.confidence >= 0.05f)
    }

    @Test
    fun `repeated failure accumulates consecutive failures`() {
        val once = SpacedRepetitionEngine.adjustMemory(MemoryStrength(), false, 0f)
        val twice = SpacedRepetitionEngine.adjustMemory(once, false, 0f)
        assertEquals(2, twice.consecutiveFailures)
    }

    @Test
    fun `correct answer resets consecutive failures`() {
        val failed = SpacedRepetitionEngine.adjustMemory(MemoryStrength(), false, 0f)
        val corrected = SpacedRepetitionEngine.adjustMemory(failed, true, 1f)
        assertEquals(0, corrected.consecutiveFailures)
    }

    @Test
    fun `several correct answers lead to mastery`() {
        var memory = MemoryStrength()
        repeat(4) {
            memory = SpacedRepetitionEngine.adjustMemory(memory, true, 1f)
        }
        assertTrue(SpacedRepetitionEngine.isMastered(memory.strength))
    }

    @Test
    fun `mixed answers keep strength moderate`() {
        var memory = MemoryStrength()
        memory = SpacedRepetitionEngine.adjustMemory(memory, true, 1f)
        memory = SpacedRepetitionEngine.adjustMemory(memory, true, 0.8f)
        memory = SpacedRepetitionEngine.adjustMemory(memory, false, 0f)
        assertTrue(memory.strength in 0f..1f)
    }

    @Test
    fun `average score weights history`() {
        var memory = MemoryStrength()
        memory = SpacedRepetitionEngine.adjustMemory(memory, true, 1f)
        memory = SpacedRepetitionEngine.adjustMemory(memory, true, 0.5f)
        val expected = (1f + 0.5f) / 2f
        assertEquals(expected, memory.averageScore, 0.001f)
    }

    @Test
    fun `new memory defaults to fresh strength`() {
        assertEquals(0.4f, MemoryStrength().strength, 0.001f)
    }

    // ------------------------------------------------------------------
    // Practice accuracy
    // ------------------------------------------------------------------

    @Test
    fun `speaking practice updates speaking accuracy`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.SPEAKING, 0.9f)
        assertEquals(0.9f, updated.speakingAccuracy, 0.001f)
    }

    @Test
    fun `listening practice updates listening accuracy`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.LISTENING, 0.7f)
        assertEquals(0.7f, updated.listeningAccuracy, 0.001f)
    }

    @Test
    fun `reading practice updates reading accuracy`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.READING, 0.6f)
        assertEquals(0.6f, updated.readingAccuracy, 0.001f)
    }

    @Test
    fun `conversation practice updates conversation success`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.CONVERSATION, 1f)
        assertEquals(1f, updated.conversationSuccess, 0.001f)
    }

    @Test
    fun `mixed practice does not alter accuracies`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.MIXED, 0.8f)
        assertEquals(0f, updated.speakingAccuracy, 0.001f)
        assertEquals(0f, updated.listeningAccuracy, 0.001f)
    }

    @Test
    fun `accuracy clamps into range`() {
        val updated = SpacedRepetitionEngine.withPractice(MemoryStrength(), ReviewType.SPEAKING, 2f)
        assertEquals(1f, updated.speakingAccuracy, 0.001f)
    }

    @Test
    fun `memory overall accuracy averages the four modes`() {
        val memory = MemoryStrength(speakingAccuracy = 1f, listeningAccuracy = 0.5f, readingAccuracy = 0f)
        assertEquals(0.375f, memory.accuracy, 0.001f)
    }

    // ------------------------------------------------------------------
    // Difficulty & priority
    // ------------------------------------------------------------------

    @Test
    fun `low strength is new difficulty`() {
        assertEquals(ReviewDifficulty.NEW, SpacedRepetitionEngine.difficultyFor(0.1f))
    }

    @Test
    fun `mid strength is learning`() {
        assertEquals(ReviewDifficulty.LEARNING, SpacedRepetitionEngine.difficultyFor(0.4f))
    }

    @Test
    fun `high strength is familiar`() {
        assertEquals(ReviewDifficulty.FAMILIAR, SpacedRepetitionEngine.difficultyFor(0.7f))
    }

    @Test
    fun `mastered strength maps to mastered`() {
        assertEquals(ReviewDifficulty.MASTERED, SpacedRepetitionEngine.difficultyFor(0.9f))
    }

    @Test
    fun `priority higher for weaker words`() {
        assertTrue(
            SpacedRepetitionEngine.priorityFor(0.2f, 0) >
                SpacedRepetitionEngine.priorityFor(0.9f, 0)
        )
    }

    @Test
    fun `priority bounded between zero and one`() {
        val priority = SpacedRepetitionEngine.priorityFor(0.5f, 3)
        assertTrue(priority in 0f..1f)
    }

    @Test
    fun `recall decays over time`() {
        val now = 0f
        val soon = SpacedRepetitionEngine.recallProbability(0.8f, 0)
        val later = SpacedRepetitionEngine.recallProbability(0.8f, 14 * 24 * 60 * 60 * 1000L)
        assertTrue(soon > now)
        assertTrue(later < soon)
    }

    @Test
    fun `recall at half life is half over seven days`() {
        val expected = 0.8f * 0.5f
        val actual = SpacedRepetitionEngine.recallProbability(0.8f, 7 * 24 * 60 * 60 * 1000L)
        assertEquals(expected, actual, 0.05f)
    }

    @Test
    fun `recall stays within bounds`() {
        assertTrue(SpacedRepetitionEngine.recallProbability(1f, 0) in 0f..1f)
        assertTrue(SpacedRepetitionEngine.recallProbability(0f, 100000L) in 0f..1f)
    }
}