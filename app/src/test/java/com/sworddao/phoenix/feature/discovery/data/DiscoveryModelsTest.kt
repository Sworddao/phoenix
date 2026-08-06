package com.sworddao.phoenix.feature.discovery.data

import org.junit.Assert.*
import org.junit.Test

class DiscoveryModelsTest {

    @Test
    fun `DiscoverySourceType has all expected entries`() {
        val sources = DiscoverySourceType.entries
        assertEquals(13, sources.size)
        assertTrue(sources.contains(DiscoverySourceType.NPC))
        assertTrue(sources.contains(DiscoverySourceType.DIALOGUE))
        assertTrue(sources.contains(DiscoverySourceType.QUEST))
        assertTrue(sources.contains(DiscoverySourceType.FRIENDSHIP))
        assertTrue(sources.contains(DiscoverySourceType.REGION))
        assertTrue(sources.contains(DiscoverySourceType.PASSPORT))
        assertTrue(sources.contains(DiscoverySourceType.STORY))
        assertTrue(sources.contains(DiscoverySourceType.LISTENING))
        assertTrue(sources.contains(DiscoverySourceType.SPEAKING))
        assertTrue(sources.contains(DiscoverySourceType.MINI_GAME))
        assertTrue(sources.contains(DiscoverySourceType.FESTIVAL))
        assertTrue(sources.contains(DiscoverySourceType.HIDDEN))
        assertTrue(sources.contains(DiscoverySourceType.EXPLORATION))
    }

    @Test
    fun `DiscoverySourceType has display names`() {
        assertEquals("NPC Interaction", DiscoverySourceType.NPC.displayName)
        assertEquals("Conversation", DiscoverySourceType.DIALOGUE.displayName)
        assertEquals("Quest Reward", DiscoverySourceType.QUEST.displayName)
        assertEquals("Friendship Milestone", DiscoverySourceType.FRIENDSHIP.displayName)
        assertEquals("Region Discovery", DiscoverySourceType.REGION.displayName)
        assertEquals("Passport Stamp", DiscoverySourceType.PASSPORT.displayName)
        assertEquals("Story Progression", DiscoverySourceType.STORY.displayName)
        assertEquals("Listening Practice", DiscoverySourceType.LISTENING.displayName)
        assertEquals("Speaking Practice", DiscoverySourceType.SPEAKING.displayName)
        assertEquals("Mini Game", DiscoverySourceType.MINI_GAME.displayName)
        assertEquals("Festival Event", DiscoverySourceType.FESTIVAL.displayName)
        assertEquals("Hidden Discovery", DiscoverySourceType.HIDDEN.displayName)
        assertEquals("Exploration", DiscoverySourceType.EXPLORATION.displayName)
    }

    @Test
    fun `AnimationPhase has all expected entries`() {
        val phases = AnimationPhase.entries
        assertEquals(5, phases.size)
        assertTrue(phases.contains(AnimationPhase.IDLE))
        assertTrue(phases.contains(AnimationPhase.WORD_APPEARING))
        assertTrue(phases.contains(AnimationPhase.WORD_DISPLAYING))
        assertTrue(phases.contains(AnimationPhase.REWARD_SHOWING))
        assertTrue(phases.contains(AnimationPhase.COMPLETING))
    }

    @Test
    fun `VocabularyDiscovery data class defaults`() {
        val discovery = VocabularyDiscovery(
            id = "disc_001",
            wordId = "greet_001",
            source = DiscoverySourceType.NPC,
            sourceId = "npc_001",
            sourceName = "Grandma Mei",
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = true,
        )

        assertEquals("disc_001", discovery.id)
        assertEquals("greet_001", discovery.wordId)
        assertEquals(DiscoverySourceType.NPC, discovery.source)
        assertEquals("npc_001", discovery.sourceId)
        assertEquals("Grandma Mei", discovery.sourceName)
        assertTrue(discovery.isFirstDiscovery)
        assertEquals(0, discovery.bonusXp)
        assertEquals(0, discovery.bonusFriendshipXp)
        assertNull(discovery.relatedNpcId)
        assertNull(discovery.relatedQuestId)
        assertNull(discovery.relatedRegionId)
        assertTrue(discovery.metadata.isEmpty())
    }

    @Test
    fun `VocabularyDiscovery with optional fields`() {
        val discovery = VocabularyDiscovery(
            id = "disc_002",
            wordId = "food_001",
            source = DiscoverySourceType.QUEST,
            sourceId = "quest_001",
            sourceName = "Help Grandma",
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = false,
            bonusXp = 25,
            bonusFriendshipXp = 5,
            relatedNpcId = "grandma_mei",
            relatedQuestId = "quest_help_grandma",
            relatedRegionId = "qingyuan_village",
            metadata = mapOf("dialogue_id" to "dialogue_001"),
        )

        assertEquals(25, discovery.bonusXp)
        assertEquals(5, discovery.bonusFriendshipXp)
        assertEquals("grandma_mei", discovery.relatedNpcId)
        assertEquals("quest_help_grandma", discovery.relatedQuestId)
        assertEquals("qingyuan_village", discovery.relatedRegionId)
        assertEquals("dialogue_001", discovery.metadata["dialogue_id"])
    }

    @Test
    fun `DiscoveryReward data class defaults`() {
        val reward = DiscoveryReward()

        assertEquals(0, reward.xp)
        assertEquals(0, reward.friendshipXp)
        assertTrue(reward.vocabularyWords.isEmpty())
        assertEquals(0, reward.streakBonus)
        assertFalse(reward.categoryBonus)
        assertFalse(reward.regionBonus)
    }

    @Test
    fun `DiscoveryReward with values`() {
        val reward = DiscoveryReward(
            xp = 50,
            friendshipXp = 10,
            vocabularyWords = listOf("word_1", "word_2"),
            streakBonus = 5,
            categoryBonus = true,
            regionBonus = true,
        )

        assertEquals(50, reward.xp)
        assertEquals(10, reward.friendshipXp)
        assertEquals(2, reward.vocabularyWords.size)
        assertEquals(5, reward.streakBonus)
        assertTrue(reward.categoryBonus)
        assertTrue(reward.regionBonus)
    }

    @Test
    fun `DiscoveryStatistics data class defaults`() {
        val stats = DiscoveryStatistics(
            totalDiscovered = 25,
            totalAvailable = 100,
            todayDiscovered = 3,
            weekDiscovered = 10,
            monthDiscovered = 20,
            streakDays = 5,
            longestStreak = 10,
            lastDiscoveryDate = System.currentTimeMillis(),
            wordsBySource = emptyMap(),
            wordsByCategory = emptyMap(),
            wordsByMastery = emptyMap(),
            wordsByRegion = emptyMap(),
            averageDiscoveriesPerDay = 2.5f,
            completionPercentage = 0.25f,
        )

        assertEquals(25, stats.totalDiscovered)
        assertEquals(100, stats.totalAvailable)
        assertEquals(3, stats.todayDiscovered)
        assertEquals(10, stats.weekDiscovered)
        assertEquals(20, stats.monthDiscovered)
        assertEquals(5, stats.streakDays)
        assertEquals(10, stats.longestStreak)
        assertEquals(2.5f, stats.averageDiscoveriesPerDay)
        assertEquals(0.25f, stats.completionPercentage)
    }

    @Test
    fun `DiscoveryHistory data class defaults`() {
        val history = DiscoveryHistory(
            discoveries = emptyList(),
            totalCount = 0,
            todayCount = 0,
            weekCount = 0,
            streakDays = 0,
            lastDiscoveryDate = null,
            wordsBySource = emptyMap(),
            wordsByCategory = emptyMap(),
            wordsByRegion = emptyMap(),
        )

        assertEquals(0, history.totalCount)
        assertEquals(0, history.todayCount)
        assertEquals(0, history.weekCount)
        assertEquals(0, history.streakDays)
        assertNull(history.lastDiscoveryDate)
    }

    @Test
    fun `DiscoveryResult WordDiscovered`() {
        val word = com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord(
            id = "greet_001",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory.GREETINGS,
            difficulty = com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
        )

        val discovery = VocabularyDiscovery(
            id = "disc_001",
            wordId = "greet_001",
            source = DiscoverySourceType.NPC,
            sourceId = "npc_001",
            sourceName = "Grandma Mei",
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = true,
        )

        val reward = DiscoveryReward(xp = 15, friendshipXp = 2)

        val result = DiscoveryResult.WordDiscovered(
            word = word,
            discovery = discovery,
            isFirstDiscovery = true,
            reward = reward,
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        assertEquals("greet_001", result.word.id)
        assertTrue(result.isFirstDiscovery)
        assertEquals(15, result.reward.xp)
    }

    @Test
    fun `DiscoveryResult WordAlreadyDiscovered`() {
        val word = com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord(
            id = "greet_001",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory.GREETINGS,
            difficulty = com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
        )

        val discovery = VocabularyDiscovery(
            id = "disc_001",
            wordId = "greet_001",
            source = DiscoverySourceType.NPC,
            sourceId = "npc_001",
            sourceName = "Grandma Mei",
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = true,
        )

        val result = DiscoveryResult.WordAlreadyDiscovered(
            word = word,
            discovery = discovery,
        )

        assertTrue(result is DiscoveryResult.WordAlreadyDiscovered)
        assertEquals("greet_001", result.word.id)
    }

    @Test
    fun `DiscoveryResult BatchDiscovered`() {
        val result = DiscoveryResult.BatchDiscovered(
            words = emptyList(),
            totalXp = 50,
            totalFriendshipXp = 10,
        )

        assertTrue(result is DiscoveryResult.BatchDiscovered)
        assertEquals(50, result.totalXp)
        assertEquals(10, result.totalFriendshipXp)
        assertTrue(result.words.isEmpty())
    }

    @Test
    fun `DiscoveryResult Success`() {
        val result = DiscoveryResult.Success("Discovery recorded")
        assertTrue(result is DiscoveryResult.Success)
        assertEquals("Discovery recorded", result.message)
    }

    @Test
    fun `DiscoveryResult Error`() {
        val result = DiscoveryResult.Error("Word not found")
        assertTrue(result is DiscoveryResult.Error)
        assertEquals("Word not found", result.message)
    }

    @Test
    fun `DiscoveryAnimationState defaults`() {
        val state = DiscoveryAnimationState()

        assertFalse(state.isShowing)
        assertNull(state.currentWord)
        assertNull(state.source)
        assertNull(state.sourceName)
        assertFalse(state.isFirstDiscovery)
        assertNull(state.reward)
        assertEquals(AnimationPhase.IDLE, state.animationPhase)
    }

    @Test
    fun `DiscoveryAnimationState with values`() {
        val word = com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord(
            id = "greet_001",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory.GREETINGS,
            difficulty = com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
        )

        val reward = DiscoveryReward(xp = 15)

        val state = DiscoveryAnimationState(
            isShowing = true,
            currentWord = word,
            source = DiscoverySourceType.NPC,
            sourceName = "Grandma Mei",
            isFirstDiscovery = true,
            reward = reward,
            animationPhase = AnimationPhase.WORD_APPEARING,
        )

        assertTrue(state.isShowing)
        assertEquals("greet_001", state.currentWord?.id)
        assertEquals(DiscoverySourceType.NPC, state.source)
        assertEquals("Grandma Mei", state.sourceName)
        assertTrue(state.isFirstDiscovery)
        assertEquals(15, state.reward?.xp)
        assertEquals(AnimationPhase.WORD_APPEARING, state.animationPhase)
    }

    @Test
    fun `NewlyUnlockedWord data class`() {
        val word = com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord(
            id = "greet_001",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory.GREETINGS,
            difficulty = com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
        )

        val reward = DiscoveryReward(xp = 15, friendshipXp = 2)

        val unlocked = NewlyUnlockedWord(
            word = word,
            source = DiscoverySourceType.NPC,
            sourceName = "Grandma Mei",
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = true,
            reward = reward,
        )

        assertEquals("greet_001", unlocked.word.id)
        assertEquals(DiscoverySourceType.NPC, unlocked.source)
        assertEquals("Grandma Mei", unlocked.sourceName)
        assertTrue(unlocked.isFirstDiscovery)
        assertEquals(15, unlocked.reward.xp)
    }

    @Test
    fun `DiscoverySession data class`() {
        val session = DiscoverySession(
            id = "session_001",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis(),
            discoveries = emptyList(),
            source = DiscoverySourceType.DIALOGUE,
            sourceId = "dialogue_001",
            totalXpEarned = 50,
            totalFriendshipXpEarned = 10,
            isActive = false,
        )

        assertEquals("session_001", session.id)
        assertEquals(DiscoverySourceType.DIALOGUE, session.source)
        assertEquals(50, session.totalXpEarned)
        assertFalse(session.isActive)
    }

    @Test
    fun `DailyDiscovery data class`() {
        val daily = DailyDiscovery(
            date = System.currentTimeMillis(),
            discoveries = emptyList(),
            totalCount = 5,
            streakDay = true,
        )

        assertEquals(5, daily.totalCount)
        assertTrue(daily.streakDay)
    }
}
