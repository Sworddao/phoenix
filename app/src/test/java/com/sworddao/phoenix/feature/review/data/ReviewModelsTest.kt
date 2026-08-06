package com.sworddao.phoenix.feature.review.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewModelsTest {

    // ------------------------------------------------------------------
    // Enums
    // ------------------------------------------------------------------

    @Test
    fun `review sources has nine entries`() {
        assertEquals(9, ReviewSource.entries.size)
    }

    @Test
    fun `review types has eight entries`() {
        assertEquals(8, ReviewType.entries.size)
    }

    @Test
    fun `every review source has a name and icon`() {
        ReviewSource.entries.forEach { source ->
            assertTrue(source.displayName.isNotBlank())
            assertTrue(source.icon.isNotBlank())
        }
    }

    @Test
    fun `every review type has a name and icon`() {
        ReviewType.entries.forEach { type ->
            assertTrue(type.displayName.isNotBlank())
            assertTrue(type.icon.isNotBlank())
        }
    }

    // ------------------------------------------------------------------
    // MemoryStrength
    // ------------------------------------------------------------------

    @Test
    fun `memory strength fresh defaults to learning`() {
        assertEquals(ReviewDifficulty.LEARNING, MemoryStrength().masteryLevel)
    }

    @Test
    fun `memory strength maps to mastered at threshold`() {
        assertEquals(ReviewDifficulty.MASTERED, MemoryStrength(strength = 0.85f).masteryLevel)
    }

    @Test
    fun `memory strength maps to new when weak`() {
        assertEquals(ReviewDifficulty.NEW, MemoryStrength(strength = 0.2f).masteryLevel)
    }

    @Test
    fun `memory strength maps to familiar mid range`() {
        assertEquals(ReviewDifficulty.FAMILIAR, MemoryStrength(strength = 0.7f).masteryLevel)
    }

    @Test
    fun `memory accuracy is average of modes`() {
        val memory = MemoryStrength(speakingAccuracy = 0.9f, readingAccuracy = 0.3f)
        assertEquals(0.4f, memory.accuracy, 0.001f)
    }

    @Test
    fun `overall accuracy uses review count`() {
        val memory = MemoryStrength(correctAnswers = 3, reviewCount = 5)
        assertEquals(0.6f, memory.overallAccuracy, 0.001f)
    }

    @Test
    fun `overall accuracy zero before reviews`() {
        assertEquals(0f, MemoryStrength().overallAccuracy, 0.001f)
    }

    // ------------------------------------------------------------------
    // ReviewSchedule & ReviewItem
    // ------------------------------------------------------------------

    @Test
    fun `schedule default interval is ten minutes`() {
        assertEquals(SpacedRepetitionEngine.intervalForStage(0), ReviewSchedule(itemId = "x").intervalMillis)
    }

    @Test
    fun `schedule is due when dueAt in past`() {
        val schedule = ReviewSchedule(itemId = "x", dueAt = System.currentTimeMillis() - 1000L)
        assertTrue(schedule.isDue)
    }

    @Test
    fun `schedule not due when dueAt in future`() {
        val schedule = ReviewSchedule(itemId = "x", dueAt = System.currentTimeMillis() + 100000L)
        assertFalse(schedule.isDue)
    }

    @Test
    fun `review item carries strengthens and difficulty`() {
        val item = ReviewItem(
            id = "r1",
            source = ReviewSource.VOCABULARY,
            type = ReviewType.MIXED,
            prompt = "你好",
            detail = "ni hao · hello",
            memoryStrength = 0.65f,
        )
        assertEquals(ReviewDifficulty.FAMILIAR, item.difficulty)
        assertTrue(item.priority in 0f..1f)
    }

    @Test
    fun `review item default difficulty is new`() {
        val item = ReviewItem(id = "i", source = ReviewSource.DIALOGUE, type = ReviewType.CONVERSATION, prompt = "p", detail = "d")
        assertEquals(ReviewDifficulty.NEW, item.difficulty)
    }

    // ------------------------------------------------------------------
    // ReviewSession
    // ------------------------------------------------------------------

    @Test
    fun `session progress reflects answered count`() {
        val session = ReviewSession(
            id = "s",
            type = ReviewType.DAILY_REVIEW,
            correctCount = 2,
            incorrectCount = 1,
            totalCount = 6,
        )
        assertEquals(0.5f, session.progress, 0.001f)
    }

    @Test
    fun `session accuracy reflects correct rate`() {
        val session = ReviewSession(
            id = "s",
            type = ReviewType.MIXED,
            correctCount = 4,
            totalCount = 5,
        )
        assertEquals(0.8f, session.accuracy, 0.001f)
    }

    @Test
    fun `session not completed by default`() {
        assertFalse(ReviewSession(id = "s", type = ReviewType.MIXED).isCompleted)
    }

    @Test
    fun `session progress zero when empty`() {
        assertEquals(0f, ReviewSession(id = "s", type = ReviewType.MIXED).progress, 0.001f)
    }

    @Test
    fun `session answered count sums both`() {
        val session = ReviewSession(id = "s", type = ReviewType.READING, correctCount = 3, incorrectCount = 2)
        assertEquals(5, session.answeredCount)
    }

    // ------------------------------------------------------------------
    // Statistics & DailyReview
    // ------------------------------------------------------------------

    @Test
    fun `statistics accuracy is correct ratio`() {
        val stats = ReviewStatistics(totalReviews = 10, correctReviews = 7)
        assertEquals(0.7f, stats.accuracy, 0.001f)
    }

    @Test
    fun `statistics accuracy zero when no reviews`() {
        assertEquals(0f, ReviewStatistics().accuracy, 0.001f)
    }

    @Test
    fun `statistics average score divides total`() {
        val stats = ReviewStatistics(totalReviews = 2, totalScore = 1.5f)
        assertEquals(0.75f, stats.averageScore, 0.001f)
    }

    @Test
    fun `daily review goal completion percent`() {
        val daily = DailyReview(completedCount = 2, dailyGoal = 5)
        assertEquals(0.4f, daily.completionPercent, 0.001f)
    }

    @Test
    fun `daily review goal reached at goal`() {
        assertFalse(DailyReview(completedCount = 4).isGoalReached)
        assertTrue(DailyReview(completedCount = 5).isGoalReached)
        assertTrue(DailyReview(completedCount = 7).isGoalReached)
    }

    @Test
    fun `daily review remaining never negative`() {
        assertEquals(0, DailyReview(completedCount = 9).activitiesRemaining)
    }

    @Test
    fun `daily review default goal is five`() {
        assertEquals(5, DailyReview().dailyGoal)
    }

    @Test
    fun `daily completion caps at one`() {
        assertEquals(1f, DailyReview(completedCount = 20, dailyGoal = 5).completionPercent, 0.001f)
    }

    // ------------------------------------------------------------------
    // Recommendations & results
    // ------------------------------------------------------------------

    @Test
    fun `recommendation carries full metadata`() {
        val rec = ReviewRecommendation(
            id = "r",
            title = "t",
            description = "d",
            type = ReviewType.NPC_CHALLENGE,
            priority = 0.8f,
            icon = "icon",
        )
        assertEquals("r", rec.id)
        assertEquals(ReviewType.NPC_CHALLENGE, rec.type)
    }

    @Test
    fun `answered result carries scheduling payload`() {
        val result = ReviewResult.Answered(
            itemId = "i",
            wordId = "w",
            correct = true,
            score = 1f,
            strengthAfter = 0.6f,
            intervalMillis = SpacedRepetitionEngine.intervalForStage(1),
            nextReviewAt = 123L,
            difficulty = ReviewDifficulty.LEARNING,
        )
        assertTrue(result.correct)
        assertEquals(ReviewDifficulty.LEARNING, result.difficulty)
    }

    @Test
    fun `session completed result carries xp`() {
        val session = ReviewSession(id = "s", type = ReviewType.DAILY_REVIEW)
        val result = ReviewResult.SessionCompleted(session, 15, 0.8f)
        assertEquals(15, result.xpEarned)
        assertEquals(0.8f, result.accuracy, 0.001f)
    }

    @Test
    fun `refreshed result carries due count`() {
        assertEquals(4, (ReviewResult.Refreshed(4) as ReviewResult.Refreshed).dueCount)
    }

    @Test
    fun `difficulty from strength covers all thresholds`() {
        assertEquals(ReviewDifficulty.NEW, ReviewDifficulty.fromStrength(0.29f))
        assertEquals(ReviewDifficulty.LEARNING, ReviewDifficulty.fromStrength(0.3f))
        assertEquals(ReviewDifficulty.LEARNING, ReviewDifficulty.fromStrength(0.54f))
        assertEquals(ReviewDifficulty.FAMILIAR, ReviewDifficulty.fromStrength(0.55f))
        assertEquals(ReviewDifficulty.FAMILIAR, ReviewDifficulty.fromStrength(0.79f))
        assertEquals(ReviewDifficulty.MASTERED, ReviewDifficulty.fromStrength(0.8f))
    }
}