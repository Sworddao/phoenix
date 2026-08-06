package com.sworddao.phoenix.feature.quest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestModelsTest {

    @Test
    fun `quest progress calculates correctly`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 5, currentCount = 3),
                createTestObjective(id = "2", targetCount = 5, currentCount = 5),
            ),
        )

        assertEquals(0.8f, quest.progress, 0.01f)
        assertEquals(1, quest.completedObjectives)
        assertEquals(2, quest.totalObjectives)
    }

    @Test
    fun `quest is complete when all non-optional objectives are done`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 1, currentCount = 1),
                createTestObjective(id = "2", targetCount = 1, currentCount = 1),
            ),
        )

        assertTrue(quest.isComplete)
    }

    @Test
    fun `quest is not complete when objectives are incomplete`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 5, currentCount = 3),
                createTestObjective(id = "2", targetCount = 1, currentCount = 1),
            ),
        )

        assertFalse(quest.isComplete)
    }

    @Test
    fun `quest is complete when optional objectives are incomplete`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 1, currentCount = 1),
                createTestObjective(id = "2", targetCount = 5, currentCount = 3, optional = true),
            ),
        )

        assertTrue(quest.isComplete)
    }

    @Test
    fun `quest active objectives filters completed ones`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 1, currentCount = 1),
                createTestObjective(id = "2", targetCount = 5, currentCount = 3),
            ),
        )

        assertEquals(1, quest.activeObjectives.size)
        assertEquals("2", quest.activeObjectives[0].id)
    }

    @Test
    fun `quest objective progress calculates correctly`() {
        val objective = createTestObjective(id = "1", targetCount = 10, currentCount = 5)

        assertEquals(0.5f, objective.progress, 0.01f)
        assertFalse(objective.isComplete)
    }

    @Test
    fun `quest objective is complete when count reaches target`() {
        val objective = createTestObjective(id = "1", targetCount = 5, currentCount = 5)

        assertTrue(objective.isComplete)
        assertEquals(1.0f, objective.progress, 0.01f)
    }

    @Test
    fun `quest difficulty experience reward is correct`() {
        assertEquals(10, QuestDifficulty.EASY.experienceReward)
        assertEquals(20, QuestDifficulty.MEDIUM.experienceReward)
        assertEquals(30, QuestDifficulty.HARD.experienceReward)
        assertEquals(50, QuestDifficulty.EXPERT.experienceReward)
    }

    @Test
    fun `quest progress calculates correctly with empty objectives`() {
        val quest = createTestQuest(objectives = emptyList())

        assertEquals(0f, quest.progress)
    }

    @Test
    fun `quest progress with multiple objectives calculates average`() {
        val quest = createTestQuest(
            objectives = listOf(
                createTestObjective(id = "1", targetCount = 10, currentCount = 10),
                createTestObjective(id = "2", targetCount = 10, currentCount = 0),
                createTestObjective(id = "3", targetCount = 10, currentCount = 5),
            ),
        )

        assertEquals(0.5f, quest.progress, 0.01f)
    }

    @Test
    fun `quest with prerequisites has correct structure`() {
        val quest = createTestQuest(
            prerequisites = QuestPrerequisite(
                questIds = listOf("quest_1", "quest_2"),
                friendshipLevel = 2,
                requiredLevel = 5,
            ),
        )

        assertEquals(2, quest.prerequisites.questIds.size)
        assertEquals(2, quest.prerequisites.friendshipLevel)
        assertEquals(5, quest.prerequisites.requiredLevel)
    }

    @Test
    fun `quest reward contains expected fields`() {
        val reward = QuestReward(
            experience = 50,
            vocabulary = listOf("你好", "谢谢"),
            items = listOf("钥匙"),
            friendshipPoints = 10,
            unlockQuests = listOf("quest_unlocked"),
            unlockAreas = listOf("area_new"),
        )

        assertEquals(50, reward.experience)
        assertEquals(2, reward.vocabulary.size)
        assertEquals(1, reward.items.size)
        assertEquals(10, reward.friendshipPoints)
        assertEquals(1, reward.unlockQuests.size)
        assertEquals(1, reward.unlockAreas.size)
    }

    @Test
    fun `quest stats calculates correctly`() {
        val quests = listOf(
            createTestQuest(status = QuestStatus.COMPLETED),
            createTestQuest(status = QuestStatus.COMPLETED),
            createTestQuest(status = QuestStatus.ACTIVE),
            createTestQuest(status = QuestStatus.LOCKED),
        )

        val stats = QuestStats(
            totalQuests = quests.size,
            completedQuests = quests.count { it.status == QuestStatus.COMPLETED },
            activeQuests = quests.count { it.status == QuestStatus.ACTIVE },
            lockedQuests = quests.count { it.status == QuestStatus.LOCKED },
            availableQuests = quests.count { it.status == QuestStatus.AVAILABLE },
            completionRate = quests.count { it.status == QuestStatus.COMPLETED }.toFloat() / quests.size,
            totalExperienceEarned = 0,
            favoriteQuestType = null,
        )

        assertEquals(4, stats.totalQuests)
        assertEquals(2, stats.completedQuests)
        assertEquals(1, stats.activeQuests)
        assertEquals(1, stats.lockedQuests)
        assertEquals(0.5f, stats.completionRate, 0.01f)
    }

    @Test
    fun `quest filter matches correctly`() {
        val filter = QuestFilter(
            types = listOf(QuestType.CONVERSATION),
            difficulties = listOf(QuestDifficulty.EASY),
            statuses = listOf(QuestStatus.AVAILABLE),
            categories = listOf(QuestCategory.STORY),
            searchQuery = "测试",
        )

        val matchingQuest = createTestQuest(
            type = QuestType.CONVERSATION,
            difficulty = QuestDifficulty.EASY,
            status = QuestStatus.AVAILABLE,
            category = QuestCategory.STORY,
            title = "测试任务",
        )

        val nonMatchingQuest = createTestQuest(
            type = QuestType.STORY,
            difficulty = QuestDifficulty.HARD,
            status = QuestStatus.LOCKED,
            category = QuestCategory.EXPLORATION,
            title = "其他任务",
        )

        assertTrue(
            matchesFilter(matchingQuest, filter),
        )
        assertFalse(
            matchesFilter(nonMatchingQuest, filter),
        )
    }

    private fun matchesFilter(quest: Quest, filter: QuestFilter): Boolean {
        return (filter.types.isEmpty() || filter.types.contains(quest.type)) &&
            (filter.difficulties.isEmpty() || filter.difficulties.contains(quest.difficulty)) &&
            (filter.statuses.isEmpty() || filter.statuses.contains(quest.status)) &&
            (filter.categories.isEmpty() || filter.categories.contains(quest.category)) &&
            (filter.searchQuery.isEmpty() ||
                quest.title.contains(filter.searchQuery, ignoreCase = true) ||
                quest.description.contains(filter.searchQuery, ignoreCase = true))
    }

    private fun createTestQuest(
        id: String = "test_quest",
        title: String = "测试任务",
        description: String = "这是一个测试任务",
        type: QuestType = QuestType.CONVERSATION,
        difficulty: QuestDifficulty = QuestDifficulty.EASY,
        status: QuestStatus = QuestStatus.AVAILABLE,
        category: QuestCategory = QuestCategory.STORY,
        objectives: List<QuestObjective> = listOf(createTestObjective()),
        rewards: QuestReward = QuestReward(),
        prerequisites: QuestPrerequisite = QuestPrerequisite(),
    ) = Quest(
        id = id,
        title = title,
        description = description,
        type = type,
        difficulty = difficulty,
        status = status,
        category = category,
        objectives = objectives,
        rewards = rewards,
        prerequisites = prerequisites,
    )

    private fun createTestObjective(
        id: String = "test_objective",
        type: ObjectiveType = ObjectiveType.TALK_TO_NPC,
        description: String = "测试目标",
        targetCount: Int = 1,
        currentCount: Int = 0,
        optional: Boolean = false,
    ) = QuestObjective(
        id = id,
        type = type,
        description = description,
        targetCount = targetCount,
        currentCount = currentCount,
        optional = optional,
    )
}
