package com.sworddao.phoenix.feature.quest.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuestRepositoryTest {

    private lateinit var repository: MockQuestRepository

    @Before
    fun setup() {
        repository = MockQuestRepository()
    }

    @Test
    fun `getAllQuests returns all quests`() = runTest {
        val quests = repository.getAllQuests().first()

        assertTrue(quests.isNotEmpty())
        assertEquals(10, quests.size)
    }

    @Test
    fun `getQuestById returns correct quest`() = runTest {
        val quest = repository.getQuestById("quest_help_grandma_mei").first()

        assertNotNull(quest)
        assertEquals("帮助梅奶奶", quest?.title)
    }

    @Test
    fun `getQuestById returns null for non-existent quest`() = runTest {
        val quest = repository.getQuestById("non_existent_quest").first()

        assertEquals(null, quest)
    }

    @Test
    fun `getQuestsByType filters correctly`() = runTest {
        val conversationQuests = repository.getQuestsByType(QuestType.CONVERSATION).first()

        assertTrue(conversationQuests.isNotEmpty())
        assertTrue(conversationQuests.all { it.type == QuestType.CONVERSATION })
    }

    @Test
    fun `getQuestsByDifficulty filters correctly`() = runTest {
        val easyQuests = repository.getQuestsByDifficulty(QuestDifficulty.EASY).first()

        assertTrue(easyQuests.isNotEmpty())
        assertTrue(easyQuests.all { it.difficulty == QuestDifficulty.EASY })
    }

    @Test
    fun `getQuestsByCategory filters correctly`() = runTest {
        val storyQuests = repository.getQuestsByCategory(QuestCategory.STORY).first()

        assertTrue(storyQuests.isNotEmpty())
        assertTrue(storyQuests.all { it.category == QuestCategory.STORY })
    }

    @Test
    fun `getActiveQuests returns only active quests`() = runTest {
        val activeQuests = repository.getActiveQuests().first()

        assertTrue(activeQuests.all { it.status == QuestStatus.ACTIVE })
    }

    @Test
    fun `getCompletedQuests returns only completed quests`() = runTest {
        val completedQuests = repository.getCompletedQuests().first()

        assertTrue(completedQuests.all { it.status == QuestStatus.COMPLETED })
    }

    @Test
    fun `getAvailableQuests returns only available quests`() = runTest {
        val availableQuests = repository.getAvailableQuests().first()

        assertTrue(availableQuests.isNotEmpty())
        assertTrue(availableQuests.all { it.status == QuestStatus.AVAILABLE })
    }

    @Test
    fun `startQuest changes quest status to active`() = runTest {
        val result = repository.startQuest("quest_help_grandma_mei")

        assertTrue(result is QuestResult.Success)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.ACTIVE, quest?.status)
    }

    @Test
    fun `startQuest fails for non-existent quest`() = runTest {
        val result = repository.startQuest("non_existent_quest")

        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `startQuest fails for locked quest`() = runTest {
        val result = repository.startQuest("quest_buy_dumplings")

        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `completeQuest changes quest status to completed`() = runTest {
        repository.startQuest("quest_help_grandma_mei")

        // Complete all objectives
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)

        val result = repository.completeQuest("quest_help_grandma_mei")

        assertTrue(result is QuestResult.QuestCompleted)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.COMPLETED, quest?.status)
    }

    @Test
    fun `completeQuest fails for non-active quest`() = runTest {
        val result = repository.completeQuest("quest_help_grandma_mei")

        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `abandonQuest changes quest status back to available`() = runTest {
        repository.startQuest("quest_help_grandma_mei")

        val result = repository.abandonQuest("quest_help_grandma_mei")

        assertTrue(result is QuestResult.Success)

        val quest = repository.getQuestById("quest_help_grandma_mei").first()
        assertEquals(QuestStatus.AVAILABLE, quest?.status)
    }

    @Test
    fun `abandonQuest fails for non-active quest`() = runTest {
        val result = repository.abandonQuest("quest_help_grandma_mei")

        assertTrue(result is QuestResult.Error)
    }

    @Test
    fun `updateObjectiveProgress increments progress correctly`() = runTest {
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
    fun `updateObjectiveProgress completes objective when target reached`() = runTest {
        repository.startQuest("quest_help_grandma_mei")

        val result = repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)

        assertTrue(result is QuestResult.ObjectiveCompleted)

        val progress = repository.getQuestProgress("quest_help_grandma_mei").first()
        val objective = progress?.objectives?.find { it.id == "obj_1_1" }
        assertTrue(objective?.isComplete == true)
    }

    @Test
    fun `updateObjectiveProgress caps at target count`() = runTest {
        repository.startQuest("quest_help_grandma_mei")

        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 10)

        val progress = repository.getQuestProgress("quest_help_grandma_mei").first()
        val objective = progress?.objectives?.find { it.id == "obj_1_2" }
        assertEquals(5, objective?.currentCount) // Target is 5
    }

    @Test
    fun `getQuestStats returns correct stats`() = runTest {
        val stats = repository.getQuestStats().first()

        assertEquals(10, stats.totalQuests)
        assertEquals(0, stats.completedQuests)
        assertEquals(0, stats.activeQuests)
        assertTrue(stats.lockedQuests > 0)
        assertTrue(stats.availableQuests > 0)
    }

    @Test
    fun `getQuestsByFilter filters correctly`() = runTest {
        val filter = QuestFilter(
            types = listOf(QuestType.CONVERSATION),
            difficulties = listOf(QuestDifficulty.EASY),
        )

        val quests = repository.getQuestsByFilter(filter).first()

        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.type == QuestType.CONVERSATION && it.difficulty == QuestDifficulty.EASY })
    }

    @Test
    fun `refreshQuestAvailability unlocks prerequisites`() = runTest {
        // Complete the first quest
        repository.startQuest("quest_help_grandma_mei")
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        repository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        repository.completeQuest("quest_help_grandma_mei")

        // Refresh availability
        repository.refreshQuestAvailability()

        // Check if the second quest is now available
        val quest2 = repository.getQuestById("quest_buy_dumplings").first()
        assertEquals(QuestStatus.AVAILABLE, quest2?.status)
    }

    @Test
    fun `getQuestsByNpc filters correctly`() = runTest {
        val quests = repository.getQuestsByNpc("npc_mei").first()

        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.npcId == "npc_mei" })
    }

    @Test
    fun `getQuestsByLocation filters correctly`() = runTest {
        val quests = repository.getQuestsByLocation("location_mei_house").first()

        assertTrue(quests.isNotEmpty())
        assertTrue(quests.all { it.locationId == "location_mei_house" })
    }
}
