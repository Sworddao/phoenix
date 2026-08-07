package com.sworddao.phoenix.data.seed


import com.sworddao.phoenix.feature.discovery.data.DiscoverySourceType
import com.sworddao.phoenix.feature.discovery.data.VocabularyDiscovery

object DiscoverySeedData {

fun createInitialDiscoveries(): List<VocabularyDiscovery> {
        val now = System.currentTimeMillis()
        return listOf(
            VocabularyDiscovery(
                id = "disc_001",
                wordId = "greet_001",
                source = DiscoverySourceType.NPC,
                sourceId = "grandma_mei",
                sourceName = "Grandma Mei",
                discoveredAt = now - 86400000 * 10,
                isFirstDiscovery = true,
                bonusXp = 15,
                relatedNpcId = "grandma_mei",
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_002",
                wordId = "greet_003",
                source = DiscoverySourceType.DIALOGUE,
                sourceId = "grandma_mei_greeting",
                sourceName = "Greeting Conversation",
                discoveredAt = now - 86400000 * 10,
                isFirstDiscovery = true,
                bonusXp = 18,
                relatedNpcId = "grandma_mei",
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_003",
                wordId = "food_001",
                source = DiscoverySourceType.QUEST,
                sourceId = "quest_help_grandma_mei",
                sourceName = "Help Grandma Mei",
                discoveredAt = now - 86400000 * 9,
                isFirstDiscovery = true,
                bonusXp = 25,
                relatedQuestId = "quest_help_grandma_mei",
                relatedNpcId = "grandma_mei",
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_004",
                wordId = "food_005",
                source = DiscoverySourceType.FRIENDSHIP,
                sourceId = "grandma_mei_friend_1",
                sourceName = "Friendship with Grandma Mei",
                discoveredAt = now - 86400000 * 8,
                isFirstDiscovery = true,
                bonusXp = 30,
                bonusFriendshipXp = 10,
                relatedNpcId = "grandma_mei",
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_005",
                wordId = "reg_005",
                source = DiscoverySourceType.REGION,
                sourceId = "qingyuan_village",
                sourceName = "Qingyuan Village",
                discoveredAt = now - 86400000 * 7,
                isFirstDiscovery = true,
                bonusXp = 10,
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_006",
                wordId = "shop_006",
                source = DiscoverySourceType.STORY,
                sourceId = "story_chapter_1",
                sourceName = "Chapter 1: Arrival",
                discoveredAt = now - 86400000 * 6,
                isFirstDiscovery = true,
                bonusXp = 15,
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_007",
                wordId = "trans_006",
                source = DiscoverySourceType.EXPLORATION,
                sourceId = "explore_village",
                sourceName = "Exploring the Village",
                discoveredAt = now - 86400000 * 5,
                isFirstDiscovery = true,
                bonusXp = 12,
                relatedRegionId = "qingyuan_village",
            ),
            VocabularyDiscovery(
                id = "disc_008",
                wordId = "common_011",
                source = DiscoverySourceType.NPC,
                sourceId = "university_student_wei",
                sourceName = "Student Wei",
                discoveredAt = now - 86400000 * 4,
                isFirstDiscovery = true,
                bonusXp = 15,
                relatedNpcId = "university_student_wei",
                relatedRegionId = "qingyuan_village",
            ),
        )
    }

}
