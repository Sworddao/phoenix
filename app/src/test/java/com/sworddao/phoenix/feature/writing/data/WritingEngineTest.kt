package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.seed.WritingSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingEngineTest {

    private val engine = MockWritingEngine()

    @Test
    fun `startSession initializes state for a character`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)

        assertEquals(character.id, state.characterId)
        assertEquals(character.hanzi, state.hanzi)
        assertEquals(character.strokeCount, state.expectedStrokeCount)
        assertEquals(0, state.nextStrokeIndex)
        assertTrue(state.strokesCompleted.isEmpty())
        assertFalse(state.isComplete)
    }

    @Test
    fun `expectedStroke returns the stroke at the next index`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)

        val expected = engine.expectedStroke(state.sessionId)
        assertNotNull(expected)
        assertEquals(character.strokes.first(), expected)
    }

    @Test
    fun `recordStroke with correct order and direction completes a single stroke`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)

        val feedback = engine.recordStroke(state.sessionId, 0, StrokeDirection.LEFT_TO_RIGHT)

        assertTrue(feedback.wasOrderCorrect)
        assertTrue(feedback.wasDirectionCorrect)
        assertTrue(feedback.wasCorrect)
        assertTrue(engine.isComplete(state.sessionId))
        assertEquals(1f, engine.progress(state.sessionId), 0.0001f)
        assertEquals(1, engine.correctOrderCount(state.sessionId))
        assertEquals(1, engine.correctDirectionCount(state.sessionId))
    }

    @Test
    fun `recordStroke with correct order but wrong direction is marked incorrect`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)

        val feedback = engine.recordStroke(state.sessionId, 0, StrokeDirection.TOP_TO_BOTTOM)

        assertTrue(feedback.wasOrderCorrect)
        assertFalse(feedback.wasDirectionCorrect)
        assertFalse(feedback.wasCorrect)
        assertEquals(0, engine.correctDirectionCount(state.sessionId))
    }

    @Test
    fun `recordStroke with wrong direction does not advance to the next stroke`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "好" }
        val state = engine.startSession(character)

        val feedback = engine.recordStroke(state.sessionId, 0, StrokeDirection.TOP_TO_BOTTOM)

        assertTrue(feedback.wasOrderCorrect)
        assertFalse(feedback.wasCorrect)
        assertEquals(0, engine.correctDirectionCount(state.sessionId))
        assertEquals(0f, engine.progress(state.sessionId), 0.0001f)
        assertFalse(engine.isComplete(state.sessionId))
        assertEquals(character.strokes.first(), engine.expectedStroke(state.sessionId))
    }

    @Test
    fun `wrong direction then retry with correct direction completes the stroke`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "好" }
        val state = engine.startSession(character)

        engine.recordStroke(state.sessionId, 0, StrokeDirection.TOP_TO_BOTTOM)
        val retry = engine.recordStroke(state.sessionId, 0, character.strokes.first().direction)

        assertTrue(retry.wasCorrect)
        assertEquals(2, engine.correctOrderCount(state.sessionId))
        assertEquals(1, engine.correctDirectionCount(state.sessionId))
        assertEquals(1f / character.strokeCount, engine.progress(state.sessionId), 0.0001f)
        assertEquals(character.strokes[1], engine.expectedStroke(state.sessionId))
    }

    @Test
    fun `recordStroke with wrong index does not advance the session`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "好" }
        val state = engine.startSession(character)

        val feedback = engine.recordStroke(state.sessionId, 3, StrokeDirection.LEFT_TO_RIGHT)

        assertFalse(feedback.wasOrderCorrect)
        assertFalse(feedback.wasCorrect)
        assertEquals(0, engine.correctOrderCount(state.sessionId))
        assertFalse(engine.isComplete(state.sessionId))
    }

    @Test
    fun `all strokes in correct order complete the session`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "人" }
        val state = engine.startSession(character)

        var complete = false
        character.strokes.forEachIndexed { index, stroke ->
            val feedback = engine.recordStroke(state.sessionId, index, stroke.direction)
            assertTrue("stroke $index should be correct", feedback.wasCorrect)
            complete = engine.isComplete(state.sessionId)
        }

        assertTrue(complete)
        assertEquals(character.strokeCount, engine.correctOrderCount(state.sessionId))
        assertEquals(1f, engine.progress(state.sessionId), 0.0001f)
    }

    @Test
    fun `reset restores the session to its initial state`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)
        engine.recordStroke(state.sessionId, 0, StrokeDirection.LEFT_TO_RIGHT)
        assertTrue(engine.isComplete(state.sessionId))

        engine.reset(state.sessionId)

        assertEquals(0f, engine.progress(state.sessionId), 0.0001f)
        assertEquals(0, engine.correctOrderCount(state.sessionId))
        assertFalse(engine.isComplete(state.sessionId))
    }

    @Test
    fun `endSession removes the session`() {
        val character = WritingSeedData.createInitialCharacters().first { it.hanzi == "一" }
        val state = engine.startSession(character)

        engine.endSession(state.sessionId)

        assertFalse(engine.isComplete(state.sessionId))
        assertEquals(0f, engine.progress(state.sessionId), 0.0001f)
        assertEquals(0, engine.correctOrderCount(state.sessionId))
    }

    @Test
    fun `getEngineInfo reports supported exercise types`() {
        val info = engine.getEngineInfo()

        assertEquals("MockWritingEngine", info.name)
        assertTrue(info.supportedTypes.containsAll(WritingExerciseType.entries))
        assertTrue(info.supportsStrokeAnimation)
    }
}
