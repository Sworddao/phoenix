package com.sworddao.phoenix.feature.quest.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.RoomGameProgressRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomQuestRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomQuestRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomQuestRepository(
            dao = database.questDao(),
            gameProgressRepository = RoomGameProgressRepository(database.gameProgressDao()),
            vocabularyRepository = MockVocabularyRepository(),
            friendshipRepository = MockFriendshipRepository(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllQuests returns all quests`() = runBlocking {
        val quests = repository.getAllQuests().first()
        assertTrue(quests.isNotEmpty())
        assertEquals(10, quests.size)
    }

    @Test
    fun `getQuestById returns correct quest`() = runBlocking {
        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertNotNull(quest)
        assertEquals("帮助梅奶奶", quest?.title)
    }

    @Test
    fun `getQuestById returns null for non-existent quest`() = runBlocking {
        val quest = repository.getQuestById("non_existent_quest").first()
        assertEquals(null, quest)
    }

    @Test
    fun `getQuestsByType filters correctly`() = runBlocking {
        val conversationQuests = repository.getQuestsByType(QuestType.CONVERSATION).first()
        assertTrue(conversationQuests.isNotEmpty())
        assertTrue(conversationQuests.all { it.type == QuestType.CONVERSATION })
    }

    @Test
    fun `getQuestsByDifficulty filters correctly`() = runBlocking {
        val easyQuests = repository.getQuestsByDifficulty(QuestDifficulty.EASY).first()
        assertTrue(easyQuests.isNotEmpty())
        assertTrue(easyQuests.all { it.difficulty == QuestDifficulty.EASY })
    }

    @Test
    fun `getQuestsByCategory filters correctly`() = runBlocking {
        val storyQuests = repository.getQuestsByCategory(QuestCategory.STORY).first()
        assertTrue(storyQuests.isNotEmpty())
        assertTrue(storyQuests.all { it.category == QuestCategory.STORY })
    }

    @Test
    fun `getActiveQuests returns only active quests`() = runBlocking {
        val activeQuests = repository.getActiveQuests().first()
        assertTrue(activeQuests.all { it.status == QuestStatus.ACTIVE })
    }

    @Test
    fun `getAvailableQuests returns only available quests`() = runBlocking {
        val availableQuests = repository.getAvailableQuests().first()
        assertTrue(availableQuests.isNotEmpty())
        assertTrue(availableQuests.all { it.status == QuestStatus.AVAILABLE })
    }

    @Test
    fun `startQuest changes quest status to active`() = runBlocking {
        val result = repository.startQuest("quest_help_grandma_mei")
        assertTrue(result is QuestResult.Success)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.ACTIVE, quest?.status)
    }

    @Test
    fun `startQuest fails for non-existent quest`() = runBlocking {
        val result = repository.startQuest("non_existent_quest")
        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `startQuest fails for locked quest`() = runBlocking {
        val result = repository.startQuest("quest_buy_dumplings")
        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `completeQuest changes quest status to completed`() = runBlocking {
        repository.startQuest("quest_help_grandma_mei")
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)

        val result = repository.completeQuest("quest_help_grandma_mei")
        assertTrue(result is QuestResult.QuestCompleted)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.COMPLETED, quest?.status)
    }

    @Test
    fun `completeQuest records quest completion in game progress`() = runBlocking {
        val gameProgressRepository = RoomGameProgressRepository(database.gameProgressDao())

        repository.startQuest("quest_help_grandma_mei")
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)

        val result = repository.completeQuest("quest_help_grandma_mei")
        assertTrue(result is QuestResult.QuestCompleted)

        val progress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, progress.totalQuestsCompleted)
        assertTrue(progress.hasCompletedFirstQuest)
    }

    @Test
    fun `completeQuest fails for non-active quest`() = runBlocking {
        val result = repository.completeQuest("quest_help_grandma_mei")
        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `abandonQuest changes quest status back to available`() = runBlocking {
        repository.startQuest("quest_help_grandma_mei")

        val result = repository.abandonQuest("quest_help_grandma_mei")
        assertTrue(result is QuestResult.Success)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.AVAILABLE, quest?.status)
    }

    @Test
    fun `updateObjectiveProgress increments progress correctly`() = runBlocking {
        repository.startQuest("quest_help_grandma_mei")

        val result = repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 3)
        assertTrue(result is QuestResult.Success)

        val progress = repository.getQuestProgress("quest_help_grandma_mei").first()
        assertNotNull(progress)

        val objective = progress?.objectives?.find { it.id == "obj_1_2" }
        assertNotNull(objective)
        assertEquals(3, objective?.currentCount)
    }

    @Test
    fun `updateObjectiveProgress caps at target count`() = runBlocking {
        repository.startQuest("quest_help_grandma_mei")

        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 10)

        val progress = repository.getQuestProgress("quest_help_grandma_mei").first()
        val objective = progress?.objectives?.find { it.id == "obj_1_2" }
        assertEquals(5, objective?.currentCount)
    }

    @Test
    fun `getQuestStats returns correct stats`() = runBlocking {
        val stats = repository.getQuestStats().first()
        assertEquals(10, stats.totalQuests)
        assertEquals(0, stats.completedQuests)
        assertTrue(stats.lockedQuests > 0)
        assertTrue(stats.availableQuests > 0)
    }

    @Test
    fun `getQuestsByFilter filters correctly`() = runBlocking {
        val filter = QuestFilter(
            types = listOf(QuestType.CONVERSATION),
            difficulties = listOf(QuestDifficulty.EASY),
        )

        val quests = repository.getQuestsByFilter(filter).first()
        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.type == QuestType.CONVERSATION && it.difficulty == QuestDifficulty.EASY })
    }

    @Test
    fun `refreshQuestAvailability unlocks prerequisites`() = runBlocking {
        repository.startQuest("quest_help_grandma_mei")
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        repository.completeQuest("quest_help_grandma_mei")

        repository.refreshQuestAvailability()

        val quest2 = repository.getQuestById("quest_buy_dumplings").first()
        assertEquals(QuestStatus.AVAILABLE, quest2?.status)
    }

    @Test
    fun `getQuestsByNpc filters correctly`() = runBlocking {
        val quests = repository.getQuestsByNpc("npc_mei").first()
        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.npcId == "npc_mei" })
    }

    @Test
    fun `getQuestsByLocation filters correctly`() = runBlocking {
        val quests = repository.getQuestsByLocation("location_mei_house").first()
        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.locationId == "location_mei_house" })
    }
}
