package com.sworddao.phoenix.feature.progression.data

import com.sworddao.phoenix.feature.discovery.data.MockDiscoveryRepository
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.reading.data.MockHanziRenderer
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.writing.data.MockWritingRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressionRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var worldRepository: MockWorldRepository
    private lateinit var discoveryRepository: MockDiscoveryRepository
    private lateinit var pronunciationRepository: MockPronunciationRepository
    private lateinit var listeningRepository: MockListeningRepository
    private lateinit var readingRepository: MockReadingRepository
    private lateinit var writingRepository: MockWritingRepository
    private lateinit var repository: MockProgressionRepository

    @Before
    fun setup() {
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        worldRepository = MockWorldRepository()
        discoveryRepository = MockDiscoveryRepository(
            vocabularyRepository = vocabularyRepository,
        )
        pronunciationRepository = MockPronunciationRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        listeningRepository = MockListeningRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
        )
        readingRepository = MockReadingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            hanziRenderer = MockHanziRenderer(),
        )
        writingRepository = MockWritingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        repository = MockProgressionRepository(
            gameProgressRepository = gameProgressRepository,
            worldRepository = worldRepository,
            questRepository = questRepository,
            passportRepository = passportRepository,
            vocabularyRepository = vocabularyRepository,
            friendshipRepository = friendshipRepository,
            discoveryRepository = discoveryRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            readingRepository = readingRepository,
            writingRepository = writingRepository,
        )
    }

    private suspend fun initialRefresh() {
        repository.refresh()
    }

    // ------------------------------------------------------------------
    // Initial state
    // ------------------------------------------------------------------

    @Test
    fun `refresh initializes zero xp state`() = runTest {
        initialRefresh()

        val player = repository.getPlayerProgress().first()
        assertEquals(1, player.level)
        assertEquals(0, player.totalXp)
        assertEquals(100, player.xpToNextLevel)
    }

    @Test
    fun `refresh returns refreshed result`() = runTest {
        val result = repository.refresh()
        assertTrue(result is ProgressionResult.Refreshed)
    }

    @Test
    fun `initial player starts in village`() = runTest {
        initialRefresh()

        val player = repository.getPlayerProgress().first()
        assertEquals("village_intro", player.currentStoryStage)
        assertEquals(1, player.currentChapter)
        assertTrue(player.unlockedRegionIds.contains("qingyuan_village"))
    }

    @Test
    fun `player progress defaults expose no unlocked features`() = runTest {
        initialRefresh()

        val player = repository.getPlayerProgress().first()
        assertTrue(player.unlockedFeatures.isEmpty())
        assertEquals(FeatureUnlock.SPEAKING, player.nextFeatureToUnlock)
    }

    @Test
    fun `refresh twice awards no xp`() = runTest {
        initialRefresh()
        repository.refresh()

        assertEquals(0, repository.getPlayerProgress().first().totalXp)
    }

    // ------------------------------------------------------------------
    // awardXp
    // ------------------------------------------------------------------

    @Test
    fun `awardXp grants source base xp`() = runTest {
        val result = repository.awardXp(XpSource.DIALOGUE)

        assertTrue(result is ProgressionResult.XpAwarded)
        assertEquals(20, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `awardXp counts multiply xp`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 3)

        assertEquals(60, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `awardXp to one hundred levels up`() = runTest {
        val result = repository.awardXp(XpSource.DIALOGUE, 5)

        assertTrue(result is ProgressionResult.LevelUp)
        val levelUp = result as ProgressionResult.LevelUp
        assertEquals(2, levelUp.newLevel)
        assertTrue(levelUp.unlockedFeatures.contains(FeatureUnlock.SPEAKING))
        assertEquals(2, repository.getPlayerProgress().first().level)
    }

    @Test
    fun `level two unlocks speaking feature`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)

        val player = repository.getPlayerProgress().first()
        assertTrue(player.hasFeature(FeatureUnlock.SPEAKING))
        assertEquals(FeatureUnlock.LISTENING, player.nextFeatureToUnlock)
    }

    @Test
    fun `level three unlocks listening feature`() = runTest {
        repository.awardXp(XpSource.QUEST_COMPLETION, 5)

        val player = repository.getPlayerProgress().first()
        assertTrue(player.hasFeature(FeatureUnlock.LISTENING))
        assertEquals(3, player.level)
    }

    @Test
    fun `feature unlock timeline marks unlocked entries`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)

        val timeline = repository.getFeatureUnlockTimeline().first()
        assertEquals(8, timeline.size)
        val speaking = timeline.first { it.feature == FeatureUnlock.SPEAKING }
        assertTrue(speaking.isUnlocked)
        assertNotNull(speaking.unlockedAt)
        val regions = timeline.first { it.feature == FeatureUnlock.REGIONS }
        assertFalse(regions.isUnlocked)
    }

    @Test
    fun `level ten unlocks regions feature`() = runTest {
        repository.awardXp(XpSource.QUEST_COMPLETION, 36)

        val player = repository.getPlayerProgress().first()
        assertEquals(10, player.level)
        assertTrue(player.hasFeature(FeatureUnlock.REGIONS))
        assertNull(player.nextFeatureToUnlock)
    }

    @Test
    fun `player xp fields stay consistent`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)

        val player = repository.getPlayerProgress().first()
        assertEquals(100, player.totalXp)
        assertEquals(0, player.xpIntoLevel)
        assertEquals(125, player.xpToNextLevel)
    }

    // ------------------------------------------------------------------
    // Daily progress
    // ------------------------------------------------------------------

    @Test
    fun `daily progress records xp earned`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 2)
        repository.awardXp(XpSource.READING_PRACTICE)

        val daily = repository.getDailyProgress().first()
        assertEquals(50, daily.xpEarnedToday)
        assertEquals(3, daily.activitiesCompletedToday)
        assertEquals(2, daily.activitiesByType[XpSource.DIALOGUE])
        assertEquals(1, daily.activitiesByType[XpSource.READING_PRACTICE])
    }

    @Test
    fun `daily goal reached after three activities`() = runTest {
        repository.awardXp(XpSource.DIALOGUE)
        repository.awardXp(XpSource.DIALOGUE)
        repository.awardXp(XpSource.DIALOGUE)

        val daily = repository.getDailyProgress().first()
        assertTrue(daily.isGoalReached)
        assertEquals(1f, daily.completionPercent, 0.001f)
    }

    @Test
    fun `daily goal completion is recorded as recent unlock`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 3)

        val unlocks = repository.getRecentUnlocks().first()
        assertTrue(unlocks.any { it.title == "每日目标达成" })
    }

    @Test
    fun `daily progress defaults before activity`() = runTest {
        val daily = repository.getDailyProgress().first()
        assertEquals(0, daily.activitiesCompletedToday)
        assertEquals(3, daily.dailyGoal)
    }

    // ------------------------------------------------------------------
    // Snapshot delta detection
    // ------------------------------------------------------------------

    @Test
    fun `refresh detects dialogue completions`() = runTest {
        initialRefresh()
        gameProgressRepository.recordDialogueCompleted("grandma_mei")

        repository.refresh()

        val player = repository.getPlayerProgress().first()
        assertEquals(60, player.totalXp)
    }

    @Test
    fun `refresh detects word discoveries`() = runTest {
        initialRefresh()
        gameProgressRepository.recordWordDiscovered("greet_001")

        repository.refresh()

        assertEquals(50, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects quest completions`() = runTest {
        initialRefresh()
        questRepository.startQuest("quest_help_grandma_mei")
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        questRepository.completeQuest("quest_help_grandma_mei")

        repository.refresh()

        assertEquals(50, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects friendship level ups`() = runTest {
        initialRefresh()
        friendshipRepository.initializeFriendship("grandma_mei")
        friendshipRepository.addFriendshipXp("grandma_mei", 200)

        repository.refresh()

        assertEquals(30, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects passport stamps`() = runTest {
        initialRefresh()
        gameProgressRepository.recordPassportStampEarned("qingyuan_village")

        repository.refresh()

        assertEquals(55, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects speaking practice`() = runTest {
        initialRefresh()
        gameProgressRepository.recordSpeakingPractice()

        repository.refresh()

        assertEquals(50, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects listening practice`() = runTest {
        initialRefresh()
        gameProgressRepository.recordListeningPractice()

        repository.refresh()

        assertEquals(50, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects reading practice`() = runTest {
        initialRefresh()
        gameProgressRepository.recordReadingPractice()

        repository.refresh()

        assertEquals(50, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh detects exploration progress`() = runTest {
        initialRefresh()
        worldRepository.completeRegion("qingyuan_village")

        repository.refresh()

        val player = repository.getPlayerProgress().first()
        assertTrue("expected exploration xp, got ${player.totalXp}", player.totalXp >= 25)
        assertTrue(
            repository.getRecentUnlocks().first().any { it.title.contains("探索") }
        )
    }

    @Test
    fun `refresh detects achievements`() = runTest {
        initialRefresh()
        gameProgressRepository.unlockMilestone(GameMilestone.FIRST_DIALOGUE)

        repository.refresh()

        assertEquals(40, repository.getPlayerProgress().first().totalXp)
    }

    @Test
    fun `refresh records daily activity from deltas`() = runTest {
        initialRefresh()
        gameProgressRepository.recordDialogueCompleted("grandma_mei")

        repository.refresh()

        val daily = repository.getDailyProgress().first()
        assertEquals(60, daily.xpEarnedToday)
        assertEquals(2, daily.activitiesCompletedToday)
    }

    @Test
    fun `snapshot deltas can trigger level up`() = runTest {
        initialRefresh()
        gameProgressRepository.recordDialogueCompleted("grandma_mei")
        gameProgressRepository.recordDialogueCompleted("grandma_mei")
        gameProgressRepository.recordDialogueCompleted("grandma_mei")
        gameProgressRepository.recordDialogueCompleted("grandma_mei")
        gameProgressRepository.recordDialogueCompleted("grandma_mei")

        repository.refresh()

        val player = repository.getPlayerProgress().first()
        assertEquals(2, player.level)
        assertTrue(player.hasFeature(FeatureUnlock.SPEAKING))
    }

    // ------------------------------------------------------------------
    // Chapters
    // ------------------------------------------------------------------

    @Test
    fun `chapter catalog covers all twelve regions`() = runTest {
        initialRefresh()

        val chapters = repository.getPlayerProgress().first().chapters
        assertEquals(12, chapters.size)
        assertEquals("chapter_1", chapters.first().id)
        assertTrue(chapters.first().isUnlocked)
        assertTrue(chapters.all { it.id.startsWith("chapter_") })
    }

    @Test
    fun `second chapter requires previous region`() = runTest {
        initialRefresh()

        val chapters = repository.getPlayerProgress().first().chapters
        val second = chapters[1]
        assertNotNull(second.unlockRequirement.requiredRegionId)
        assertEquals(2, second.order)
    }

    @Test
    fun `chapter unlock requirement level grows`() = runTest {
        initialRefresh()

        val chapters = repository.getPlayerProgress().first().chapters
        assertTrue(chapters[1].unlockRequirement.requiredLevel >= 1)
        assertTrue(chapters.last().unlockRequirement.requiredLevel >= 6)
    }

    // ------------------------------------------------------------------
    // Objectives
    // ------------------------------------------------------------------

    @Test
    fun `objectives expose nine entries`() = runTest {
        initialRefresh()

        val objectives = repository.getCurrentObjectives().first()
        assertEquals(9, objectives.size)
        assertTrue(objectives.all { it.targetCount >= 1 })
    }

    @Test
    fun `objectives reflect dialogue progress`() = runTest {
        initialRefresh()
        gameProgressRepository.recordDialogueCompleted("grandma_mei")

        repository.refresh()

        val objective = repository.getCurrentObjectives().first().first { it.id == "obj_dialogue" }
        assertEquals(1, objective.currentCount)
    }

    // ------------------------------------------------------------------
    // Learning progress
    // ------------------------------------------------------------------

    @Test
    fun `learning progress overall percent within bounds`() = runTest {
        initialRefresh()

        val learning = repository.getLearningProgress().first()
        assertTrue(learning.overallPercent in 0f..1f)
    }

    @Test
    fun `learning conversation percent reaches full after ten dialogues`() = runTest {
        initialRefresh()
        repeat(10) { gameProgressRepository.recordDialogueCompleted("grandma_mei") }

        repository.refresh()

        val learning = repository.getLearningProgress().first()
        assertEquals(1f, learning.conversationPercent, 0.001f)
    }

    @Test
    fun `learning quest percent reflects completion rate`() = runTest {
        initialRefresh()
        questRepository.startQuest("quest_help_grandma_mei")
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        questRepository.completeQuest("quest_help_grandma_mei")

        repository.refresh()

        val learning = repository.getLearningProgress().first()
        assertTrue(learning.questPercent > 0f)
    }

    // ------------------------------------------------------------------
    // Recent unlocks & reset
    // ------------------------------------------------------------------

    @Test
    fun `recent unlocks populated on activity`() = runTest {
        repository.awardXp(XpSource.DIALOGUE)

        val unlocks = repository.getRecentUnlocks().first()
        assertTrue(unlocks.any { it.title == "对话" })
    }

    @Test
    fun `recent unlocks capped at twenty`() = runTest {
        repeat(30) { repository.awardXp(XpSource.READING_PRACTICE) }

        val unlocks = repository.getRecentUnlocks().first()
        assertEquals(20, unlocks.size)
    }

    @Test
    fun `resetProgression clears all state`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)
        repository.resetProgression()

        val player = repository.getPlayerProgress().first()
        assertEquals(1, player.level)
        assertEquals(0, player.totalXp)
        assertTrue(repository.getRecentUnlocks().first().isEmpty())
        assertTrue(repository.getCurrentObjectives().first().isEmpty())
        val daily = repository.getDailyProgress().first()
        assertEquals(0, daily.activitiesCompletedToday)
    }

    @Test
    fun `resetProgression clears feature timeline`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)
        repository.resetProgression()

        val timeline = repository.getFeatureUnlockTimeline().first()
        assertTrue(timeline.all { !it.isUnlocked })
    }

    @Test
    fun `level up adds recent unlock entries`() = runTest {
        repository.awardXp(XpSource.DIALOGUE, 5)

        val unlocks = repository.getRecentUnlocks().first()
        assertTrue(unlocks.any { it.title.startsWith("升级！") })
        assertTrue(unlocks.any { it.title.contains("解锁") })
    }

    @Test
    fun `village progress reported in player progress`() = runTest {
        initialRefresh()

        val player = repository.getPlayerProgress().first()
        assertTrue(player.villageProgress in 0f..1f)
        assertTrue(player.chapterProgress in 0f..1f)
        assertTrue(player.overallCompletion in 0f..1f)
    }
}
