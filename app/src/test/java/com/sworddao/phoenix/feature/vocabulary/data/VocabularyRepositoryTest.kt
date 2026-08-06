package com.sworddao.phoenix.feature.vocabulary.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VocabularyRepositoryTest {

    private lateinit var repository: MockVocabularyRepository

    @Before
    fun setup() {
        repository = MockVocabularyRepository()
    }

    @Test
    fun `getAllWords returns all words`() = runTest {
        val words = repository.getAllWords().first()
        assertTrue(words.isNotEmpty())
        assertTrue(words.size >= 100)
    }

    @Test
    fun `getWordById returns correct word`() = runTest {
        val word = repository.getWordById("greet_001").first()
        assertNotNull(word)
        assertEquals("nǐ hǎo", word?.pinyin)
        assertEquals("hello", word?.english)
    }

    @Test
    fun `getWordById returns null for non-existent word`() = runTest {
        val word = repository.getWordById("non_existent").first()
        assertNull(word)
    }

    @Test
    fun `getWordsByCategory returns correct words`() = runTest {
        val words = repository.getWordsByCategory(VocabularyCategory.GREETINGS).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyCategory.GREETINGS, word.category)
        }
    }

    @Test
    fun `getWordsByMastery returns correct words`() = runTest {
        val words = repository.getWordsByMastery(VocabularyMastery.MASTERED).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyMastery.MASTERED, word.mastery)
        }
    }

    @Test
    fun `getWordsByDifficulty returns correct words`() = runTest {
        val words = repository.getWordsByDifficulty(VocabularyDifficulty.BEGINNER).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyDifficulty.BEGINNER, word.difficulty)
        }
    }

    @Test
    fun `getWordsByRegion returns correct words`() = runTest {
        val words = repository.getWordsByRegion("qingyuan_village").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals("qingyuan_village", word.relatedRegionId)
        }
    }

    @Test
    fun `getWordsByNpc returns correct words`() = runTest {
        val words = repository.getWordsByNpc("grandma_mei").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals("grandma_mei", word.relatedNpcId)
        }
    }

    @Test
    fun `getDiscoveredWords returns only discovered words`() = runTest {
        val words = repository.getDiscoveredWords().first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.isDiscovered)
        }
    }

    @Test
    fun `getUndiscoveredWords returns only undiscovered words`() = runTest {
        val words = repository.getUndiscoveredWords().first()
        words.forEach { word ->
            assertFalse(word.isDiscovered)
        }
    }

    @Test
    fun `getFavorites returns only favorite words`() = runTest {
        val words = repository.getFavorites().first()
        words.forEach { word ->
            assertTrue(word.isFavorite)
        }
    }

    @Test
    fun `getRecentlyLearned returns recently discovered words`() = runTest {
        val words = repository.getRecentlyLearned(5).first()
        assertTrue(words.size <= 5)
        if (words.size > 1) {
            for (i in 0 until words.size - 1) {
                assertTrue(words[i].discoveredAt!! >= words[i + 1].discoveredAt!!)
            }
        }
    }

    @Test
    fun `searchWords finds by pinyin`() = runTest {
        val words = repository.searchWords("nǐ hǎo").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.pinyin.lowercase().contains("nǐ hǎo"))
        }
    }

    @Test
    fun `searchWords finds by english`() = runTest {
        val words = repository.searchWords("hello").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.english.lowercase().contains("hello"))
        }
    }

    @Test
    fun `searchWords finds by mandarin`() = runTest {
        val words = repository.searchWords("你好").first()
        assertTrue(words.isNotEmpty())
    }

    @Test
    fun `getStatistics returns statistics`() = runTest {
        val stats = repository.getStatistics().first()
        assertNotNull(stats)
        assertTrue(stats.totalWords > 0)
        assertTrue(stats.discoveredWords > 0)
    }

    @Test
    fun `getCategories returns all categories`() = runTest {
        val categories = repository.getCategories().first()
        assertEquals(12, categories.size)
    }

    @Test
    fun `discoverWord marks word as discovered`() = runTest {
        val undiscovered = repository.getWordById("undiscovered_002").first()
        assertNotNull(undiscovered)
        assertFalse(undiscovered?.isDiscovered == true)

        val result = repository.discoverWord("undiscovered_002")
        assertTrue(result is VocabularyResult.WordDiscovered)

        val word = repository.getWordById("undiscovered_002").first()
        assertNotNull(word)
        assertTrue(word?.isDiscovered == true)
    }

    @Test
    fun `discoverWord returns error for non-existent word`() = runTest {
        val result = repository.discoverWord("non_existent")
        assertTrue(result is VocabularyResult.Error)
    }

    @Test
    fun `discoverWord returns error for already discovered word`() = runTest {
        val result = repository.discoverWord("greet_001")
        assertTrue(result is VocabularyResult.Error)
    }

    @Test
    fun `updateMastery updates word mastery`() = runTest {
        val result = repository.updateMastery("greet_002", VocabularyMastery.FAMILIAR)
        assertTrue(result is VocabularyResult.MasteryUpgraded)

        val word = repository.getWordById("greet_002").first()
        assertEquals(VocabularyMastery.FAMILIAR, word?.mastery)
    }

    @Test
    fun `toggleFavorite toggles word favorite status`() = runTest {
        val initial = repository.getWordById("greet_001").first()
        val initialFavorite = initial?.isFavorite ?: false

        val result = repository.toggleFavorite("greet_001")
        assertTrue(result is VocabularyResult.FavoriteToggled)

        val updated = repository.getWordById("greet_001").first()
        assertEquals(!initialFavorite, updated?.isFavorite)
    }

    @Test
    fun `incrementReview increments review count`() = runTest {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesReviewed ?: 0

        repository.incrementReview("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesReviewed)
    }

    @Test
    fun `incrementSpoken increments spoken count`() = runTest {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesSpoken ?: 0

        repository.incrementSpoken("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesSpoken)
    }

    @Test
    fun `incrementHeard increments heard count`() = runTest {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesHeard ?: 0

        repository.incrementHeard("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesHeard)
    }

    @Test
    fun `addWords adds new words`() = runTest {
        val newWords = listOf(
            VocabularyWord(
                id = "new_001",
                mandarin = "xīn",
                pinyin = "xīn",
                english = "new",
                category = VocabularyCategory.DAILY_LIFE,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Zhè ge hěn xīn.",
                exampleTranslation = "This is very new.",
                examplePinyin = "zhè ge hěn xīn."
            )
        )
        val result = repository.addWords(newWords)
        assertTrue(result is VocabularyResult.Success)

        val word = repository.getWordById("new_001").first()
        assertNotNull(word)
    }

    @Test
    fun `getProgress returns progress for word`() = runTest {
        val progress = repository.getProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals("greet_001", progress?.wordId)
    }

    @Test
    fun `qingyuan_village has vocabulary words`() = runTest {
        val words = repository.getWordsByRegion("qingyuan_village").first()
        assertTrue(words.isNotEmpty())
    }

    @Test
    fun `jade_forest has vocabulary words`() = runTest {
        val words = repository.getWordsByRegion("jade_forest").first()
        assertTrue(words.isNotEmpty())
    }

    @Test
    fun `words span all categories`() = runTest {
        val words = repository.getAllWords().first()
        val categories = words.map { it.category }.toSet()
        assertEquals(12, categories.size)
    }
}
