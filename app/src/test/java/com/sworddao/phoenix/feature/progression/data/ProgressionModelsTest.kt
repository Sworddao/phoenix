package com.sworddao.phoenix.feature.progression.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionModelsTest {

    // ------------------------------------------------------------------
    // XpCalculator
    // ------------------------------------------------------------------

    @Test
    fun `xpRequiredForLevel one is one hundred`() {
        assertEquals(100, XpCalculator.xpRequiredForLevel(1))
    }

    @Test
    fun `xpRequiredForLevel two is one hundred twenty five`() {
        assertEquals(125, XpCalculator.xpRequiredForLevel(2))
    }

    @Test
    fun `xpRequiredForLevel grows with level`() {
        assertTrue(XpCalculator.xpRequiredForLevel(10) > XpCalculator.xpRequiredForLevel(5))
    }

    @Test
    fun `xpRequiredForLevel clamps to max level`() {
        val maxLevelRequirement = XpCalculator.xpRequiredForLevel(XpCalculator.MAX_LEVEL)
        assertEquals(maxLevelRequirement, XpCalculator.xpRequiredForLevel(999))
    }

    @Test
    fun `totalXpForLevel one is zero`() {
        assertEquals(0, XpCalculator.totalXpForLevel(1))
    }

    @Test
    fun `totalXpForLevel two is one hundred`() {
        assertEquals(100, XpCalculator.totalXpForLevel(2))
    }

    @Test
    fun `totalXpForLevel three is two hundred twenty five`() {
        assertEquals(225, XpCalculator.totalXpForLevel(3))
    }

    @Test
    fun `levelForTotalXp zero is one`() {
        assertEquals(1, XpCalculator.levelForTotalXp(0))
    }

    @Test
    fun `levelForTotalXp below first threshold is one`() {
        assertEquals(1, XpCalculator.levelForTotalXp(99))
    }

    @Test
    fun `levelForTotalXp exactly one hundred is two`() {
        assertEquals(2, XpCalculator.levelForTotalXp(100))
    }

    @Test
    fun `levelForTotalXp negative is one`() {
        assertEquals(1, XpCalculator.levelForTotalXp(-50))
    }

    @Test
    fun `xpIntoLevel at boundary is zero`() {
        assertEquals(0, XpCalculator.xpIntoLevel(100))
    }

    @Test
    fun `xpIntoLevel computes remainder into level`() {
        assertEquals(25, XpCalculator.xpIntoLevel(250))
    }

    @Test
    fun `xpRemainingToNextLevel computes gap`() {
        assertEquals(125, XpCalculator.xpRemainingToNextLevel(100))
    }

    @Test
    fun `progressInLevel at boundary is zero`() {
        assertEquals(0f, XpCalculator.progressInLevel(100), 0.001f)
    }

    @Test
    fun `progressInLevel is capped at one`() {
        assertEquals(1f, XpCalculator.progressInLevel(200_000), 0.001f)
    }

    @Test
    fun `isMaxLevel false for moderate xp`() {
        assertFalse(XpCalculator.isMaxLevel(1000))
    }

    @Test
    fun `isMaxLevel true for enormous xp`() {
        assertTrue(XpCalculator.isMaxLevel(100_000_000))
    }

    @Test
    fun `max level is one hundred`() {
        assertEquals(100, XpCalculator.MAX_LEVEL)
    }

    // ------------------------------------------------------------------
    // XpSource
    // ------------------------------------------------------------------

    @Test
    fun `xp sources has twelve entries`() {
        assertEquals(12, XpSource.entries.size)
    }

    @Test
    fun `every xp source grants positive xp`() {
        XpSource.entries.forEach { source ->
            assertTrue("${source.name} must grant xp", source.baseXp > 0)
        }
    }

    @Test
    fun `dialogue source base xp is twenty`() {
        assertEquals(20, XpSource.DIALOGUE.baseXp)
    }

    @Test
    fun `quest completion source base xp is fifty`() {
        assertEquals(50, XpSource.QUEST_COMPLETION.baseXp)
    }

    // ------------------------------------------------------------------
    // FeatureUnlock
    // ------------------------------------------------------------------

    @Test
    fun `feature unlocks has eight entries`() {
        assertEquals(8, FeatureUnlock.entries.size)
    }

    @Test
    fun `feature unlock required levels are ascending`() {
        val levels = FeatureUnlock.entries.map { it.requiredLevel }
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `speaking unlocks at level two`() {
        assertEquals(2, FeatureUnlock.SPEAKING.requiredLevel)
    }

    @Test
    fun `regions unlock at level ten`() {
        assertEquals(10, FeatureUnlock.REGIONS.requiredLevel)
    }

    @Test
    fun `every feature unlock has display names`() {
        FeatureUnlock.entries.forEach { feature ->
            assertTrue(feature.displayName.isNotBlank())
            assertTrue(feature.displayNameCn.isNotBlank())
        }
    }

    @Test
    fun `feature unlock entry defaults to locked`() {
        val entry = FeatureUnlockEntry(feature = FeatureUnlock.SPEAKING)
        assertFalse(entry.isUnlocked)
        assertNull(entry.unlockedAt)
    }

    // ------------------------------------------------------------------
    // ChapterUnlockRequirement
    // ------------------------------------------------------------------

    @Test
    fun `chapter unlock requirement default has no requirements`() {
        assertFalse(ChapterUnlockRequirement().hasRequirements)
    }

    @Test
    fun `chapter unlock requirement with region has requirements`() {
        assertTrue(ChapterUnlockRequirement(requiredRegionId = "region").hasRequirements)
    }

    // ------------------------------------------------------------------
    // ChapterInfo
    // ------------------------------------------------------------------

    @Test
    fun `chapter info default is locked and incomplete`() {
        val chapter = ChapterInfo(id = "chapter_1", title = "T", titleCn = "C", order = 1, regionId = "r")
        assertFalse(chapter.isUnlocked)
        assertFalse(chapter.isCompleted)
        assertEquals(0f, chapter.completionPercentage, 0.001f)
    }

    // ------------------------------------------------------------------
    // PlayerProgress
    // ------------------------------------------------------------------

    @Test
    fun `player progress defaults to level one`() {
        val progress = PlayerProgress()
        assertEquals(1, progress.level)
        assertEquals(0, progress.totalXp)
        assertEquals(100, progress.xpToNextLevel)
    }

    @Test
    fun `hasFeature false when not unlocked`() {
        val progress = PlayerProgress(unlockedFeatures = emptyList())
        assertFalse(progress.hasFeature(FeatureUnlock.SPEAKING))
    }

    @Test
    fun `hasFeature true when unlocked`() {
        val progress = PlayerProgress(unlockedFeatures = listOf(FeatureUnlock.SPEAKING))
        assertTrue(progress.hasFeature(FeatureUnlock.SPEAKING))
    }

    @Test
    fun `nextFeatureToUnlock is speaking initially`() {
        val progress = PlayerProgress(unlockedFeatures = emptyList())
        assertEquals(FeatureUnlock.SPEAKING, progress.nextFeatureToUnlock)
    }

    @Test
    fun `nextFeatureToUnlock null when all unlocked`() {
        val progress = PlayerProgress(unlockedFeatures = FeatureUnlock.entries)
        assertNull(progress.nextFeatureToUnlock)
    }

    @Test
    fun `xp progress in level clamps to one`() {
        val progress = PlayerProgress(xpIntoLevel = 999, xpToNextLevel = 100)
        assertEquals(1f, progress.xpProgressInLevel, 0.001f)
    }

    @Test
    fun `isFeatureUnlocked property works`() {
        val progress = PlayerProgress(unlockedFeatures = listOf(FeatureUnlock.READING))
        assertTrue(progress.isFeatureUnlocked(FeatureUnlock.READING))
        assertFalse(progress.isFeatureUnlocked(FeatureUnlock.LISTENING))
    }

    // ------------------------------------------------------------------
    // LearningProgress
    // ------------------------------------------------------------------

    @Test
    fun `learning overall percent is average of ten`() {
        val learning = LearningProgress(
            speakingPercent = 1f,
            listeningPercent = 1f,
            readingPercent = 1f,
            writingPercent = 1f,
            vocabularyPercent = 1f,
            conversationPercent = 1f,
            questPercent = 1f,
            friendshipPercent = 1f,
            explorationPercent = 1f,
            passportPercent = 1f,
        )
        assertEquals(1f, learning.overallPercent, 0.001f)
    }

    @Test
    fun `learning overall percent handles zeroes`() {
        assertEquals(0f, LearningProgress().overallPercent, 0.001f)
    }

    @Test
    fun `percentFor maps each index`() {
        val learning = LearningProgress(
            speakingPercent = 0.1f,
            listeningPercent = 0.2f,
            readingPercent = 0.3f,
            writingPercent = 0.35f,
            vocabularyPercent = 0.4f,
            conversationPercent = 0.5f,
            questPercent = 0.6f,
            friendshipPercent = 0.7f,
            explorationPercent = 0.8f,
            passportPercent = 0.9f,
        )
        assertEquals(0.1f, learning.percentFor(0), 0.001f)
        assertEquals(0.35f, learning.percentFor(3), 0.001f)
        assertEquals(0.9f, learning.percentFor(9), 0.001f)
    }

    @Test
    fun `percentFor out of range returns zero`() {
        assertEquals(0f, LearningProgress().percentFor(10), 0.001f)
        assertEquals(0f, LearningProgress().percentFor(-1), 0.001f)
    }

    @Test
    fun `learning labels has ten entries`() {
        assertEquals(10, LearningProgress.LABELS.size)
    }

    // ------------------------------------------------------------------
    // DailyProgress
    // ------------------------------------------------------------------

    @Test
    fun `daily completion percent is ratio`() {
        val daily = DailyProgress(activitiesCompletedToday = 1, dailyGoal = 3)
        assertEquals(1f / 3f, daily.completionPercent, 0.001f)
    }

    @Test
    fun `daily completion percent caps at one`() {
        val daily = DailyProgress(activitiesCompletedToday = 10, dailyGoal = 3)
        assertEquals(1f, daily.completionPercent, 0.001f)
    }

    @Test
    fun `daily goal reached only when complete`() {
        assertFalse(DailyProgress(activitiesCompletedToday = 2, dailyGoal = 3).isGoalReached)
        assertTrue(DailyProgress(activitiesCompletedToday = 3, dailyGoal = 3).isGoalReached)
    }

    @Test
    fun `daily activities remaining never negative`() {
        assertEquals(0, DailyProgress(activitiesCompletedToday = 9, dailyGoal = 3).activitiesRemaining)
    }

    // ------------------------------------------------------------------
    // CurrentObjective
    // ------------------------------------------------------------------

    @Test
    fun `objective progress is ratio`() {
        val objective = CurrentObjective(
            id = "o",
            title = "T",
            description = "D",
            category = ObjectiveCategory.LEARNING,
            currentCount = 2,
            targetCount = 4,
        )
        assertEquals(0.5f, objective.progress, 0.001f)
    }

    @Test
    fun `objective completed when count reaches target`() {
        val objective = CurrentObjective(
            id = "o",
            title = "T",
            description = "D",
            category = ObjectiveCategory.STORY,
            currentCount = 4,
            targetCount = 4,
        )
        assertTrue(objective.isCompleted)
    }

    @Test
    fun `objective progress with zero target is full`() {
        val objective = CurrentObjective(
            id = "o",
            title = "T",
            description = "D",
            category = ObjectiveCategory.EXPLORATION,
            currentCount = 0,
            targetCount = 0,
        )
        assertEquals(1f, objective.progress, 0.001f)
    }

    @Test
    fun `objective categories has four entries`() {
        assertEquals(4, ObjectiveCategory.entries.size)
    }

    // ------------------------------------------------------------------
    // RecentUnlock / Timeline / Results
    // ------------------------------------------------------------------

    @Test
    fun `recent unlock generates an id`() {
        val unlock = RecentUnlock(title = "T", description = "D", icon = "I")
        assertNotNull(unlock.id)
        assertTrue(unlock.id.isNotBlank())
    }

    @Test
    fun `feature unlock timeline defaults empty`() {
        assertTrue(FeatureUnlockTimeline().entries.isEmpty())
    }

    @Test
    fun `xp awarded result carries payload`() {
        val result = ProgressionResult.XpAwarded(XpSource.DIALOGUE, 40, 1)
        assertEquals(XpSource.DIALOGUE, result.source)
        assertEquals(40, result.amount)
        assertEquals(1, result.newLevel)
    }

    @Test
    fun `feature unlocked result carries feature`() {
        val result = ProgressionResult.FeatureUnlocked(FeatureUnlock.SPEAKING)
        assertEquals(FeatureUnlock.SPEAKING, result.feature)
    }
}
