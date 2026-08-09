package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DiscoveryRepositoryTest {

    private lateinit var repository: MockDiscoveryRepository

    @Before
    fun setup() {
        repository = MockDiscoveryRepository(FakeVocabularyRepository())
    }

    @Test
    fun `getAllDiscoveries returns initial discoveries`() = runTest {
        val discoveries = repository.getAllDiscoveries().first()
        assertTrue(discoveries.isNotEmpty())
        assertEquals(8, discoveries.size)
    }

    @Test
    fun `getDiscoveryById returns correct discovery`() = runTest {
        val discovery = repository.getDiscoveryById("disc_001").first()
        assertNotNull(discovery)
        assertEquals("greet_001", discovery?.wordId)
        assertEquals(DiscoverySourceType.NPC, discovery?.source)
    }

    @Test
    fun `getDiscoveryById returns null for non-existent`() = runTest {
        val discovery = repository.getDiscoveryById("non_existent").first()
        assertNull(discovery)
    }

    @Test
    fun `getDiscoveriesByWord returns correct discoveries`() = runTest {
        val discoveries = repository.getDiscoveriesByWord("greet_001").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.wordId == "greet_001" })
    }

    @Test
    fun `getDiscoveriesBySource returns correct discoveries`() = runTest {
        val discoveries = repository.getDiscoveriesBySource(DiscoverySourceType.NPC).first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.source == DiscoverySourceType.NPC })
    }

    @Test
    fun `getDiscoveriesByNpc returns correct discoveries`() = runTest {
        val discoveries = repository.getDiscoveriesByNpc("grandma_mei").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.relatedNpcId == "grandma_mei" })
    }

    @Test
    fun `getDiscoveriesByQuest returns correct discoveries`() = runTest {
        val discoveries = repository.getDiscoveriesByQuest("quest_help_grandma_mei").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.relatedQuestId == "quest_help_grandma_mei" })
    }

    @Test
    fun `getDiscoveriesByRegion returns correct discoveries`() = runTest {
        val discoveries = repository.getDiscoveriesByRegion("qingyuan_village").first()
        assertTrue(discoveries.isNotEmpty())
        assertTrue(discoveries.all { it.relatedRegionId == "qingyuan_village" })
    }

    @Test
    fun `getRecentDiscoveries returns limited results`() = runTest {
        val discoveries = repository.getRecentDiscoveries(3).first()
        assertEquals(3, discoveries.size)
    }

    @Test
    fun `getStreakDays returns streak`() = runTest {
        val streak = repository.getStreakDays().first()
        assertNotNull(streak)
        assertTrue(streak >= 0)
    }

    @Test
    fun `discoverWord succeeds for undiscovered word`() = runTest {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
            relatedNpcId = "grandma_mei",
            relatedRegionId = "qingyuan_village",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertEquals("greet_002", discovered.word.id)
        assertTrue(discovered.isFirstDiscovery)
        assertTrue(discovered.reward.xp > 0)
    }

    @Test
    fun `discoverWord returns already discovered for known word`() = runTest {
        val result = repository.discoverWord(
            wordId = "greet_001",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
        )

        assertTrue(result is DiscoveryResult.WordAlreadyDiscovered)
    }

    @Test
    fun `discoverWords returns batch result`() = runTest {
        val result = repository.discoverWords(
            wordIds = listOf("greet_002", "greet_004"),
            source = DiscoverySourceType.QUEST,
            sourceId = "quest_001",
            sourceName = "Test Quest",
        )

        assertTrue(result is DiscoveryResult.BatchDiscovered)
        val batch = result as DiscoveryResult.BatchDiscovered
        assertTrue(batch.words.isNotEmpty())
        assertTrue(batch.totalXp > 0)
    }

    @Test
    fun `isWordDiscovered returns true for discovered word`() = runTest {
        val discovered = repository.isWordDiscovered("greet_001")
        assertTrue(discovered)
    }

    @Test
    fun `isWordDiscovered returns false for undiscovered word`() = runTest {
        val discovered = repository.isWordDiscovered("greet_002")
        assertFalse(discovered)
    }

    @Test
    fun `getDiscoveryCount returns correct count`() = runTest {
        val count = repository.getDiscoveryCount()
        assertEquals(8, count)
    }

    @Test
    fun `getDiscoveryCountBySource returns correct count`() = runTest {
        val count = repository.getDiscoveryCountBySource(DiscoverySourceType.NPC)
        assertTrue(count > 0)
    }

    @Test
    fun `getDiscoveryCountByRegion returns correct count`() = runTest {
        val count = repository.getDiscoveryCountByRegion("qingyuan_village")
        assertTrue(count > 0)
    }

    @Test
    fun `clearDiscoveryHistory clears all data`() = runTest {
        repository.clearDiscoveryHistory()

        val count = repository.getDiscoveryCount()
        assertEquals(0, count)
    }

    @Test
    fun `discovery increases count`() = runTest {
        val initialCount = repository.getDiscoveryCount()

        repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.NPC,
            sourceId = "grandma_mei",
            sourceName = "Grandma Mei",
        )

        val newCount = repository.getDiscoveryCount()
        assertEquals(initialCount + 1, newCount)
    }

    @Test
    fun `quest discovery includes quest metadata`() = runTest {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.QUEST,
            sourceId = "quest_001",
            sourceName = "Test Quest",
            relatedQuestId = "quest_help_grandma",
            relatedNpcId = "grandma_mei",
            relatedRegionId = "qingyuan_village",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertEquals("quest_help_grandma", discovered.discovery.relatedQuestId)
        assertEquals("grandma_mei", discovered.discovery.relatedNpcId)
        assertEquals("qingyuan_village", discovered.discovery.relatedRegionId)
    }

    @Test
    fun `friendship discovery has higher xp`() = runTest {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.FRIENDSHIP,
            sourceId = "friendship_001",
            sourceName = "Friendship Milestone",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertTrue(discovered.reward.friendshipXp > 0)
    }

    @Test
    fun `hidden discovery has highest xp`() = runTest {
        val result = repository.discoverWord(
            wordId = "greet_002",
            source = DiscoverySourceType.HIDDEN,
            sourceId = "hidden_001",
            sourceName = "Hidden Discovery",
        )

        assertTrue(result is DiscoveryResult.WordDiscovered)
        val discovered = result as DiscoveryResult.WordDiscovered
        assertTrue(discovered.reward.xp >= 25)
        assertTrue(discovered.reward.categoryBonus)
        assertTrue(discovered.reward.regionBonus)
    }

    @Test
    fun `recordDiscoverySession adds session`() = runTest {
        val session = DiscoverySession(
            id = "session_001",
            startTime = System.currentTimeMillis(),
            discoveries = emptyList(),
            source = DiscoverySourceType.DIALOGUE,
            sourceId = "dialogue_001",
        )

        repository.recordDiscoverySession(session)

        val sessions = repository.getDiscoverySessions().first()
        assertTrue(sessions.any { it.id == "session_001" })
    }
}

private class FakeVocabularyRepository : VocabularyRepository {
    private val words = MutableStateFlow(
        listOf(
            VocabularyWord(
                id = "greet_001",
                mandarin = "nǐ hǎo",
                pinyin = "nǐ hǎo",
                english = "hello",
                category = VocabularyCategory.GREETINGS,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Nǐ hǎo!",
                exampleTranslation = "Hello!",
                examplePinyin = "nǐ hǎo!",
                discoveredAt = System.currentTimeMillis() - 86400000 * 10,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "greet_002",
                mandarin = "zàijiàn",
                pinyin = "zài jiàn",
                english = "goodbye",
                category = VocabularyCategory.GREETINGS,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Zàijiàn!",
                exampleTranslation = "Goodbye!",
                examplePinyin = "zài jiàn!",
            ),
            VocabularyWord(
                id = "greet_003",
                mandarin = "xièxie",
                pinyin = "xiè xie",
                english = "thank you",
                category = VocabularyCategory.GREETINGS,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Xièxie!",
                exampleTranslation = "Thank you!",
                examplePinyin = "xiè xie!",
                discoveredAt = System.currentTimeMillis() - 86400000 * 10,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "greet_004",
                mandarin = "bù kèqì",
                pinyin = "bù kè qì",
                english = "you're welcome",
                category = VocabularyCategory.GREETINGS,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Bù kèqì!",
                exampleTranslation = "You're welcome!",
                examplePinyin = "bù kè qì!",
            ),
            VocabularyWord(
                id = "food_001",
                mandarin = "chīfàn",
                pinyin = "chī fàn",
                english = "eat a meal",
                category = VocabularyCategory.FOOD,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Wǒmen chīfàn ba!",
                exampleTranslation = "Let's eat!",
                examplePinyin = "wǒ men chī fàn ba!",
                discoveredAt = System.currentTimeMillis() - 86400000 * 9,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "food_005",
                mandarin = "jiǎozi",
                pinyin = "jiǎo zi",
                english = "dumplings",
                category = VocabularyCategory.FOOD,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Wǒ xǐhuan chī jiǎozi.",
                exampleTranslation = "I like to eat dumplings.",
                examplePinyin = "wǒ xǐ huan chī jiǎo zi.",
                discoveredAt = System.currentTimeMillis() - 86400000 * 8,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "reg_005",
                mandarin = "cūnzhuāng",
                pinyin = "cūn zhuāng",
                english = "village",
                category = VocabularyCategory.TRAVEL,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Zhè ge cūnzhuāng hěn měi.",
                exampleTranslation = "This village is very beautiful.",
                examplePinyin = "zhè ge cūn zhuāng hěn měi.",
                discoveredAt = System.currentTimeMillis() - 86400000 * 7,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "shop_006",
                mandarin = "yào",
                pinyin = "yào",
                english = "want",
                category = VocabularyCategory.SHOPPING,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Wǒ yào zhè ge.",
                exampleTranslation = "I want this.",
                examplePinyin = "wǒ yào zhè ge.",
                discoveredAt = System.currentTimeMillis() - 86400000 * 6,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "trans_006",
                mandarin = "zǒu",
                pinyin = "zǒu",
                english = "walk",
                category = VocabularyCategory.TRANSPORTATION,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Wǒmen zǒu ba!",
                exampleTranslation = "Let's walk!",
                examplePinyin = "wǒ men zǒu ba!",
                discoveredAt = System.currentTimeMillis() - 86400000 * 5,
                mastery = VocabularyMastery.MASTERED,
            ),
            VocabularyWord(
                id = "common_011",
                mandarin = "wǒ",
                pinyin = "wǒ",
                english = "I/me",
                category = VocabularyCategory.DAILY_LIFE,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Wǒ jiào Bao.",
                exampleTranslation = "My name is Bao.",
                examplePinyin = "wǒ jiào bao.",
                discoveredAt = System.currentTimeMillis() - 86400000 * 4,
                mastery = VocabularyMastery.MASTERED,
            ),
        )
    )

    override fun getAllWords(): Flow<List<VocabularyWord>> = words
    override fun getWordById(wordId: String): Flow<VocabularyWord?> = words.map { list -> list.find { it.id == wordId } }
    override fun getWordsByCategory(category: VocabularyCategory): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.category == category } }
    override fun getWordsByMastery(mastery: VocabularyMastery): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.mastery == mastery } }
    override fun getWordsByDifficulty(difficulty: VocabularyDifficulty): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.difficulty == difficulty } }
    override fun getWordsByRegion(regionId: String): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.relatedRegionId == regionId } }
    override fun getWordsByNpc(npcId: String): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.relatedNpcId == npcId } }
    override fun getWordsByQuest(questId: String): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.relatedQuestId == questId } }
    override fun getDiscoveredWords(): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.isDiscovered } }
    override fun getUndiscoveredWords(): Flow<List<VocabularyWord>> = words.map { list -> list.filter { !it.isDiscovered } }
    override fun getFavorites(): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.isFavorite } }
    override fun getRecentlyLearned(limit: Int): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.isDiscovered }.sortedByDescending { it.discoveredAt }.take(limit) }
    override fun searchWords(query: String): Flow<List<VocabularyWord>> = words.map { list -> list.filter { it.pinyin.contains(query, ignoreCase = true) || it.english.contains(query, ignoreCase = true) } }
    override fun getStatistics(): Flow<com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics> = words.map { list ->
        com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics(
            totalWords = list.size,
            discoveredWords = list.count { it.isDiscovered },
            masteredWords = list.count { it.mastery == VocabularyMastery.MASTERED },
        )
    }
    override fun getCategories(): Flow<List<VocabularyCategory>> = MutableStateFlow(VocabularyCategory.entries.toList())
    override fun getProgress(wordId: String): Flow<com.sworddao.phoenix.feature.vocabulary.data.VocabularyProgress?> = words.map { list ->
        list.find { it.id == wordId }?.let {
            com.sworddao.phoenix.feature.vocabulary.data.VocabularyProgress(
                wordId = it.id,
                mastery = it.mastery,
            )
        }
    }
    override suspend fun discoverWord(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(discoveredAt = System.currentTimeMillis(), mastery = VocabularyMastery.SEEN) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Word discovered")
    }
    override suspend fun updateMastery(wordId: String, mastery: VocabularyMastery): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(mastery = mastery) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Mastery updated")
    }
    override suspend fun toggleFavorite(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(isFavorite = !it.isFavorite) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Favorite toggled")
    }
    override suspend fun incrementReview(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(timesReviewed = it.timesReviewed + 1) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Review recorded")
    }
    override suspend fun incrementSpoken(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(timesSpoken = it.timesSpoken + 1) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Spoken recorded")
    }
    override suspend fun incrementHeard(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(timesHeard = it.timesHeard + 1) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Heard recorded")
    }
    override suspend fun incrementRead(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(timesRead = it.timesRead + 1) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Read recorded")
    }
    override suspend fun incrementWritten(wordId: String): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        words.value = words.value.map {
            if (it.id == wordId) it.copy(timesWritten = it.timesWritten + 1) else it
        }
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Written recorded")
    }
    override suspend fun recordDiscovery(wordId: String, source: com.sworddao.phoenix.feature.vocabulary.data.VocabularySource): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        return discoverWord(wordId)
    }
    override suspend fun addWords(words: List<VocabularyWord>): com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult {
        this.words.value = this.words.value + words
        return com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult.Success("Words added")
    }
}
