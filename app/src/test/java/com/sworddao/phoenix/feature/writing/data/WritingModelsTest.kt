package com.sworddao.phoenix.feature.writing.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.data.seed.WritingSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingModelsTest {

    private val seededExercise: WritingExercise
        get() = WritingSeedData.createInitialExercises().first()

    private fun correctStrokeAnswer(index: Int = 0, stroke: HanziStroke = seededExercise.character.strokes[0]) =
        WritingStrokeAnswer(
            strokeIndex = index,
            expectedType = stroke.type,
            expectedDirection = stroke.direction,
            wasCorrect = true,
            attempts = 1,
        )

    @Test
    fun `writing attempt is correct only when all strokes are correct`() {
        val strokes = seededExercise.character.strokes

        val allCorrect = WritingAttempt(
            exerciseId = seededExercise.id,
            strokeAnswers = strokes.mapIndexed { index, stroke -> correctStrokeAnswer(index, stroke) },
        )
        assertTrue(allCorrect.wasCorrect)
        assertEquals(strokes.size, allCorrect.correctStrokeCount)
        assertEquals(strokes.size, allCorrect.totalStrokeCount)
        assertEquals(1f, allCorrect.accuracy, 0.001f)

        val partial = allCorrect.copy(
            strokeAnswers = allCorrect.strokeAnswers.dropLast(1) + allCorrect.strokeAnswers.last().copy(wasCorrect = false),
        )
        assertFalse(partial.wasCorrect)
        assertEquals(strokes.size - 1, partial.correctStrokeCount)
        assertEquals(strokes.size, partial.totalStrokeCount)
        assertTrue(partial.accuracy < 1f)
        assertTrue(partial.accuracy > 0f)

        assertFalse(WritingAttempt(exerciseId = seededExercise.id, strokeAnswers = emptyList()).wasCorrect)
    }

    @Test
    fun `writing session tracks progress and correct attempts`() {
        val session = WritingSession(
            exerciseIds = listOf("a", "b", "c", "d"),
            attempts = listOf(
                WritingAttempt(exerciseId = "a", strokeAnswers = listOf(correctStrokeAnswer())),
                WritingAttempt(exerciseId = "b", strokeAnswers = emptyList()),
                WritingAttempt(exerciseId = "c", strokeAnswers = listOf(correctStrokeAnswer())),
            ),
        )

        assertEquals("a", session.currentExerciseId)
        assertEquals(0f, session.progress, 0.001f)
        assertEquals(2, session.correctAttempts)

        val advanced = session.copy(currentExerciseIndex = 2)
        assertEquals("c", advanced.currentExerciseId)
        assertEquals(0.5f, advanced.progress, 0.001f)

        val done = session.copy(currentExerciseIndex = 4)
        assertNull(done.currentExerciseId)
        assertEquals(1f, done.progress, 0.001f)
    }

    @Test
    fun `writing result celebrates personal best and streak`() {
        val attempt = WritingAttempt(
            exerciseId = seededExercise.id,
            strokeAnswers = seededExercise.character.strokes.mapIndexed { index, stroke -> correctStrokeAnswer(index, stroke) },
        )

        val personalBest = WritingResult(attempt = attempt, exercise = seededExercise, isNewPersonalBest = true)
        assertTrue(personalBest.shouldCelebrate)
        assertEquals("写对了！", personalBest.feedbackMessage)

        val plainCorrect = WritingResult(attempt = attempt, exercise = seededExercise)
        assertFalse(plainCorrect.shouldCelebrate)

        val wrong = attempt.copy(strokeAnswers = emptyList())
        val wrongResult = WritingResult(attempt = wrong, exercise = seededExercise, streakContinued = true)
        assertFalse(wrongResult.shouldCelebrate)
        assertTrue(wrongResult.feedbackMessage.contains("继续加油"))
    }

    @Test
    fun `writing progress computes success rate stroke accuracy and mastery`() {
        val mastered = WritingProgress(
            itemId = "greet_001",
            wordId = "greet_001",
            totalAttempts = 15,
            correctAttempts = 14,
            totalStrokes = 30,
            correctStrokes = 28,
            masteryLevel = WritingMastery.MASTERED,
        )

        assertEquals(14f / 15f, mastered.successRate, 0.001f)
        assertEquals(28f / 30f, mastered.strokeAccuracy, 0.001f)
        assertTrue(mastered.isMastered)

        assertFalse(WritingProgress(itemId = "x").isMastered)
        assertEquals(0f, WritingProgress(itemId = "x").successRate, 0.001f)
        assertEquals(0f, WritingProgress(itemId = "x").strokeAccuracy, 0.001f)
    }

    @Test
    fun `writing mastery entries are ordered by level`() {
        val levels = WritingMastery.entries.map { it.level }

        assertEquals(levels.sorted(), levels)
        assertEquals(WritingMastery.NEW, WritingMastery.entries.first())
        assertEquals(WritingMastery.MASTERED, WritingMastery.entries.last())
    }

    @Test
    fun `writing statistics overall accuracy divides correct by total`() {
        assertEquals(0.5f, WritingStatistics(totalAttempts = 4, correctAttempts = 2).overallAccuracy, 0.001f)
        assertEquals(0f, WritingStatistics().overallAccuracy, 0.001f)
    }

    @Test
    fun `hanzi character stroke count derives from strokes`() {
        val character = seededExercise.character

        assertEquals(character.strokes.size, character.strokeCount)
        assertTrue(character.isSeeded)

        val bare = character.copy(strokes = emptyList())
        assertEquals(0, bare.strokeCount)
        assertFalse(bare.isSeeded)
    }

    @Test
    fun `writing badges expose known catalog`() {
        val ids = WritingBadge.ALL_BADGES.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(ids.containsAll(listOf("write_first", "write_streak_3", "write_char_collector")))
        assertNotNull(WritingBadge.getBadge("write_first"))
        assertNull(WritingBadge.getBadge("write_nonexistent"))
    }

    @Test
    fun `writing exercise json round trips through RoomJson`() {
        val exercise = seededExercise

        val restored = RoomJson.fromJsonOrNull<WritingExercise>(RoomJson.toJson(exercise))

        assertNotNull(restored)
        assertEquals(exercise, restored)
        assertEquals(exercise.character.strokes.size, restored?.strokeCount)
    }

    @Test
    fun `writing session json round trips through RoomJson`() {
        val session = WritingSession(
            exerciseIds = listOf("e1", "e2"),
            attempts = listOf(WritingAttempt(exerciseId = "e1", strokeAnswers = listOf(correctStrokeAnswer()))),
            totalXpEarned = 20,
            isCompleted = true,
        )

        val restored = RoomJson.fromJsonOrNull<WritingSession>(RoomJson.toJson(session))

        assertNotNull(restored)
        assertEquals(session, restored)
    }

    @Test
    fun `writing statistics json round trips through RoomJson`() {
        val statistics = WritingStatistics(
            totalSessions = 2,
            totalAttempts = 5,
            correctAttempts = 4,
            charactersWritten = 3,
            exercisesByType = mapOf(WritingExerciseType.TRACE_STROKES to 5),
            exercisesByDifficulty = mapOf(WritingDifficulty.BEGINNER to 5),
        )

        val restored = RoomJson.fromJsonOrNull<WritingStatistics>(RoomJson.toJson(statistics))

        assertNotNull(restored)
        assertEquals(statistics, restored)
    }

    @Test
    fun `writing progress map json round trips through RoomJson`() {
        val progress = mapOf(
            "greet_001" to WritingProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 3,
                correctAttempts = 2,
                timesWritten = 2,
            )
        )

        val restored = RoomJson.fromJsonOrNull<Map<String, WritingProgress>>(RoomJson.toJson(progress))

        assertNotNull(restored)
        assertEquals(progress, restored)
    }
}
