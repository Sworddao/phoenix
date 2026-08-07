package com.sworddao.phoenix.feature.vocabulary.data

import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.data.local.RoomTestDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomVocabularyRepositoryTest {

    private lateinit var database: PhoenixDatabase
    private lateinit var repository: RoomVocabularyRepository

    @Before
    fun setup() {
        database = RoomTestDb.create()
        repository = RoomVocabularyRepository(database.vocabularyDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `getAllWords returns seeded words from Room`() = runBlocking {
        val words = repository.getAllWords().first()
        assertTrue(words.isNotEmpty())
        assertTrue(words.size >= 100)
    }

    @Test
    fun `getWordById returns correct word`() = runBlocking {
        val word = repository.getWordById("greet_001").first()
        assertNotNull(word)
        assertEquals("nǐ hǎo", word?.pinyin)
        assertEquals("hello", word?.english)
    }

    @Test
    fun `getWordById returns null for non-existent word`() = runBlocking {
        val word = repository.getWordById("non_existent").first()
        assertNull(word)
    }

    @Test
    fun `getWordsByCategory returns only words of category`() = runBlocking {
        val words = repository.getWordsByCategory(VocabularyCategory.GREETINGS).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyCategory.GREETINGS, word.category)
        }
    }

    @Test
    fun `getWordsByMastery returns only words of mastery`() = runBlocking {
        val words = repository.getWordsByMastery(VocabularyMastery.MASTERED).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyMastery.MASTERED, word.mastery)
        }
    }

    @Test
    fun `getWordsByDifficulty returns only words of difficulty`() = runBlocking {
        val words = repository.getWordsByDifficulty(VocabularyDifficulty.BEGINNER).first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals(VocabularyDifficulty.BEGINNER, word.difficulty)
        }
    }

    @Test
    fun `getWordsByRegion returns correct words`() = runBlocking {
        val words = repository.getWordsByRegion("qingyuan_village").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals("qingyuan_village", word.relatedRegionId)
        }
    }

    @Test
    fun `getWordsByNpc returns correct words`() = runBlocking {
        val words = repository.getWordsByNpc("grandma_mei").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertEquals("grandma_mei", word.relatedNpcId)
        }
    }

    @Test
    fun `getDiscoveredWords returns only discovered words`() = runBlocking {
        val words = repository.getDiscoveredWords().first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.isDiscovered)
        }
    }

    @Test
    fun `getUndiscoveredWords returns only undiscovered words`() = runBlocking {
        val words = repository.getUndiscoveredWords().first()
        words.forEach { word ->
            assertFalse(word.isDiscovered)
        }
    }

    @Test
    fun `getFavorites returns only favorite words`() = runBlocking {
        val words = repository.getFavorites().first()
        words.forEach { word ->
            assertTrue(word.isFavorite)
        }
    }

    @Test
    fun `getRecentlyLearned returns limited recently discovered words`() = runBlocking {
        val words = repository.getRecentlyLearned(5).first()
        assertTrue(words.size <= 5)
    }

    @Test
    fun `searchWords finds by pinyin`() = runBlocking {
        val words = repository.searchWords("nǐ hǎo").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.pinyin.lowercase().contains("nǐ hǎo"))
        }
    }

    @Test
    fun `searchWords finds by english`() = runBlocking {
        val words = repository.searchWords("hello").first()
        assertTrue(words.isNotEmpty())
        words.forEach { word ->
            assertTrue(word.english.lowercase().contains("hello"))
        }
    }

    @Test
    fun `searchWords finds by mandarin`() = runBlocking {
        val words = repository.searchWords("你好").first()
        assertTrue(words.isNotEmpty())
    }

    @Test
    fun `getStatistics returns statistics`() = runBlocking {
        val stats = repository.getStatistics().first()
        assertTrue(stats.totalWords > 0)
        assertTrue(stats.discoveredWords > 0)
        assertTrue(stats.totalWords == stats.discoveredWords + repository.getUndiscoveredWords().first().size)
    }

    @Test
    fun `getCategories returns all categories`() = runBlocking {
        val categories = repository.getCategories().first()
        assertEquals(12, categories.size)
    }

    @Test
    fun `discoverWord marks word as discovered`() = runBlocking {
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
    fun `discoverWord returns error for non-existent word`() = runBlocking {
        val result = repository.discoverWord("non_existent")
        assertTrue(result is VocabularyResult.Error)
    }

    @Test
    fun `discoverWord returns error for already discovered word`() = runBlocking {
        val result = repository.discoverWord("greet_001")
        assertTrue(result is VocabularyResult.Error)
    }

    @Test
    fun `updateMastery updates word mastery`() = runBlocking {
        val result = repository.updateMastery("greet_002", VocabularyMastery.FAMILIAR)
        assertTrue(result is VocabularyResult.MasteryUpgraded)

        val word = repository.getWordById("greet_002").first()
        assertEquals(VocabularyMastery.FAMILIAR, word?.mastery)
    }

    @Test
    fun `toggleFavorite toggles word favorite status`() = runBlocking {
        val initial = repository.getWordById("greet_001").first()
        val initialFavorite = initial?.isFavorite ?: false

        val result = repository.toggleFavorite("greet_001")
        assertTrue(result is VocabularyResult.FavoriteToggled)

        val updated = repository.getWordById("greet_001").first()
        assertEquals(!initialFavorite, updated?.isFavorite)
    }

    @Test
    fun `incrementReview increments review count`() = runBlocking {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesReviewed ?: 0

        repository.incrementReview("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesReviewed)
    }

    @Test
    fun `incrementSpoken increments spoken count`() = runBlocking {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesSpoken ?: 0

        repository.incrementSpoken("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesSpoken)
    }

    @Test
    fun `incrementHeard increments heard count`() = runBlocking {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesHeard ?: 0

        repository.incrementHeard("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesHeard)
    }

    @Test
    fun `incrementRead increments read count`() = runBlocking {
        val initial = repository.getWordById("greet_001").first()
        val initialCount = initial?.timesRead ?: 0

        repository.incrementRead("greet_001")

        val updated = repository.getWordById("greet_001").first()
        assertEquals(initialCount + 1, updated?.timesRead)
    }

    @Test
    fun `addWords adds new words`() = runBlocking {
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
    fun `getProgress returns progress for word`() = runBlocking {
        val progress = repository.getProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals("greet_001", progress?.wordId)
    }

    @Test
    fun `words span all categories in Room`() = runBlocking {
        val words = repository.getAllWords().first()
        val categories = words.map { it.category }.toSet()
        assertEquals(12, categories.size)
    }
}
