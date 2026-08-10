package com.sworddao.phoenix.feature.progression.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.discovery.data.MockDiscoveryRepository
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.reading.data.MockHanziRenderer
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import com.sworddao.phoenix.feature.writing.data.MockWritingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomProgressionRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomProgressionRepository
    private lateinit var game: MockGameProgressRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        game = MockGameProgressRepository()
        val world = MockWorldRepository()
        val quest = MockQuestRepository()
        val passport = MockPassportRepository()
        val vocabulary = MockVocabularyRepository()
        val friendship = MockFriendshipRepository()
        val discovery = MockDiscoveryRepository(vocabulary)
        val pronunciation = MockPronunciationRepository(vocabulary, quest, friendship, game, passport)
        val listening = MockListeningRepository(vocabulary, quest, friendship, game, passport, pronunciation)
        val reading = MockReadingRepository(vocabulary, quest, friendship, game, passport, pronunciation, listening, MockHanziRenderer())
        val writing = MockWritingRepository(vocabulary, quest, friendship, game, passport)

        repository = RoomProgressionRepository(
            database.progressionDao(),
            game,
            world,
            quest,
            passport,
            vocabulary,
            friendship,
            discovery,
            pronunciation,
            listening,
            reading,
            writing,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initial player progress is level one with zero xp`() = runBlocking {
        val player = repository.getPlayerProgress().first()
        assertEquals(1, player.level)
        assertEquals(0, player.totalXp)
        assertTrue(player.unlockedFeatures.isEmpty())
    }

    @Test
    fun `awardXp grants base xp and records daily activity`() = runBlocking {
        val result = repository.awardXp(XpSource.DIALOGUE)
        assertTrue(result is ProgressionResult.XpAwarded)
        val awarded = result as ProgressionResult.XpAwarded
        assertEquals(XpSource.DIALOGUE, awarded.source)
        assertEquals(20, awarded.amount)
        assertEquals(1, awarded.newLevel)

        val daily = repository.getDailyProgress().first()
        assertEquals(20, daily.xpEarnedToday)
        assertEquals(1, daily.activitiesCompletedToday)
        assertEquals(1, daily.activitiesByType[XpSource.DIALOGUE])
    }

    @Test
    fun `awardXp adds recent unlocks`() = runBlocking {
        repository.awardXp(XpSource.QUEST_COMPLETION)
        val recent = repository.getRecentUnlocks().first()
        assertTrue(recent.isNotEmpty())
        assertTrue(recent.any { it.title == "任务完成" })
    }

    @Test
    fun `large xp award triggers level up and feature unlocks`() = runBlocking {
        val result = repository.awardXp(XpSource.QUEST_COMPLETION, 10)
        assertTrue(result is ProgressionResult.LevelUp)
        val levelUp = result as ProgressionResult.LevelUp
        assertEquals(4, levelUp.newLevel)
        assertTrue(FeatureUnlock.SPEAKING in levelUp.unlockedFeatures)
        assertTrue(FeatureUnlock.LISTENING in levelUp.unlockedFeatures)
        assertTrue(FeatureUnlock.READING in levelUp.unlockedFeatures)

        val timeline = repository.getFeatureUnlockTimeline().first()
        assertTrue(timeline.any { it.feature == FeatureUnlock.SPEAKING && it.isUnlocked })

        val player = repository.getPlayerProgress().first()
        assertEquals(4, player.level)
        assertTrue(player.hasFeature(FeatureUnlock.SPEAKING))
    }

    @Test
    fun `refresh builds learning progress and objectives`() = runBlocking {
        val result = repository.refresh()
        assertTrue(result is ProgressionResult.Refreshed)
        val refreshed = result as ProgressionResult.Refreshed
        assertTrue(refreshed.learningProgress.overallPercent >= 0f)
        assertTrue(refreshed.playerProgress.chapters.isNotEmpty())

        val objectives = repository.getCurrentObjectives().first()
        assertEquals(9, objectives.size)
    }

    @Test
    fun `refresh after gameplay gains awards xp via snapshot deltas`() = runBlocking {
        repository.refresh()
        game.recordWordDiscovered("word_new")
        game.recordQuestCompleted("quest_new")
        repository.refresh()

        val player = repository.getPlayerProgress().first()
        assertEquals(90, player.totalXp)
        assertTrue(repository.getDailyProgress().first().activitiesCompletedToday >= 2)
    }

    @Test
    fun `passport stamp earned via game progress awards stamp xp on refresh`() = runBlocking {
        repository.refresh()
        game.recordPassportStampEarned("qingyuan_village")
        repository.refresh()

        val player = repository.getPlayerProgress().first()
        assertEquals(55, player.totalXp)
        val daily = repository.getDailyProgress().first()
        assertEquals(55, daily.xpEarnedToday)
        assertEquals(1, daily.activitiesByType[XpSource.PASSPORT_STAMP])
        assertEquals(1, daily.activitiesByType[XpSource.ACHIEVEMENT])
    }

    @Test
    fun `resetProgression clears all progress`() = runBlocking {
        repository.awardXp(XpSource.QUEST_COMPLETION, 5)
        repository.resetProgression()

        val player = repository.getPlayerProgress().first()
        assertEquals(1, player.level)
        assertEquals(0, player.totalXp)
        assertTrue(repository.getRecentUnlocks().first().isEmpty())
        assertTrue(repository.getCurrentObjectives().first().isEmpty())
    }
}
