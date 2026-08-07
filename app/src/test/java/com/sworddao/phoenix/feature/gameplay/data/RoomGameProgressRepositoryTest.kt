package com.sworddao.phoenix.feature.gameplay.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomGameProgressRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomGameProgressRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomGameProgressRepository(database.gameProgressDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `initial game progress has zero counters`() = runBlocking {
        val progress = repository.getGameProgress().first()
        assertEquals(0, progress.totalDialoguesCompleted)
        assertEquals(0, progress.totalWordsDiscovered)
        assertEquals(0, progress.totalQuestsCompleted)
        assertTrue(progress.milestonesCompleted.isEmpty())
    }

    @Test
    fun `recordDialogueCompleted increments counter and unlocks milestone`() = runBlocking {
        repository.recordDialogueCompleted("grandma_mei")

        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalDialoguesCompleted)
        assertTrue(GameMilestone.FIRST_DIALOGUE in progress.milestonesCompleted)
        assertEquals(listOf("grandma_mei"), progress.npcsInteractedWith)

        val summary = repository.getSessionSummary().first()
        assertEquals(1, summary.dialoguesCompleted)
        assertTrue(GameMilestone.FIRST_DIALOGUE in summary.milestonesUnlocked)
    }

    @Test
    fun `recordWordDiscovered unlocks word collector at ten words`() = runBlocking {
        repository.recordWordDiscovered("word_1")
        assertFalse(GameMilestone.WORD_COLLECTOR in repository.getGameProgress().first().milestonesCompleted)
        for (i in 2..10) {
            repository.recordWordDiscovered("word_$i")
        }
        val progress = repository.getGameProgress().first()
        assertEquals(10, progress.totalWordsDiscovered)
        assertTrue(GameMilestone.WORD_COLLECTOR in progress.milestonesCompleted)
    }

    @Test
    fun `recordQuestCompleted unlocks quest master at five quests`() = runBlocking {
        for (i in 1..5) {
            repository.recordQuestCompleted("quest_$i")
        }
        val progress = repository.getGameProgress().first()
        assertEquals(5, progress.totalQuestsCompleted)
        assertTrue(GameMilestone.FIRST_QUEST in progress.milestonesCompleted)
        assertTrue(GameMilestone.QUEST_MASTER in progress.milestonesCompleted)
    }

    @Test
    fun `recordFriendshipLevelUp unlocks friendship milestone`() = runBlocking {
        repository.recordFriendshipLevelUp("grandma_mei")

        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalFriendshipLevels)
        assertTrue(GameMilestone.FIRST_FRIENDSHIP in progress.milestonesCompleted)
    }

    @Test
    fun `recordPassportStampEarned unlocks passport milestone`() = runBlocking {
        repository.recordPassportStampEarned("qingyuan_village")

        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalPassportStamps)
        assertTrue(GameMilestone.FIRST_PASSPORT_STAMP in progress.milestonesCompleted)
    }

    @Test
    fun `recordSpeakingPractice unlocks speaking milestone`() = runBlocking {
        repository.recordSpeakingPractice()
        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalSpeakingPractices)
        assertTrue(GameMilestone.FIRST_SPEAKING in progress.milestonesCompleted)
    }

    @Test
    fun `recordListeningPractice unlocks listening milestone`() = runBlocking {
        repository.recordListeningPractice()
        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalListeningPractices)
        assertTrue(GameMilestone.FIRST_LISTENING in progress.milestonesCompleted)
    }

    @Test
    fun `recordReadingPractice unlocks reading milestone`() = runBlocking {
        repository.recordReadingPractice()
        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.totalReadingPractices)
        assertTrue(GameMilestone.FIRST_READING in progress.milestonesCompleted)
    }

    @Test
    fun `unlockMilestone adds milestone only once`() = runBlocking {
        repository.unlockMilestone(GameMilestone.VILLAGE_EXPLORER)
        repository.unlockMilestone(GameMilestone.VILLAGE_EXPLORER)

        val progress = repository.getGameProgress().first()
        assertEquals(1, progress.milestonesCompleted.count { it == GameMilestone.VILLAGE_EXPLORER })
    }

    @Test
    fun `resetSession resets progress and summary`() = runBlocking {
        repository.recordDialogueCompleted("grandma_mei")
        repository.recordWordDiscovered("word_1")

        repository.resetSession()

        val progress = repository.getGameProgress().first()
        assertEquals(0, progress.totalDialoguesCompleted)
        assertEquals(0, progress.totalWordsDiscovered)
        assertTrue(progress.milestonesCompleted.isEmpty())

        val summary = repository.getSessionSummary().first()
        assertEquals(0, summary.dialoguesCompleted)
        assertTrue(summary.milestonesUnlocked.isEmpty())
    }
}
