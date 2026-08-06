package com.sworddao.phoenix.feature.vocabulary.data

import org.junit.Assert.*
import org.junit.Test

class VocabularyModelsTest {

    @Test
    fun `VocabularyCategory has all expected entries`() {
        assertEquals(12, VocabularyCategory.entries.size)
        assertTrue(VocabularyCategory.entries.contains(VocabularyCategory.GREETINGS))
        assertTrue(VocabularyCategory.entries.contains(VocabularyCategory.FAMILY))
        assertTrue(VocabularyCategory.entries.contains(VocabularyCategory.FOOD))
        assertTrue(VocabularyCategory.entries.contains(VocabularyCategory.NUMBERS))
        assertTrue(VocabularyCategory.entries.contains(VocabularyCategory.DAILY_LIFE))
    }

    @Test
    fun `VocabularyCategory has correct displayName and icon`() {
        assertEquals("Greetings", VocabularyCategory.GREETINGS.displayName)
        assertEquals("👋", VocabularyCategory.GREETINGS.icon)
        assertEquals("Family", VocabularyCategory.FAMILY.displayName)
        assertEquals("👨‍👩‍👧", VocabularyCategory.FAMILY.icon)
        assertEquals("Food", VocabularyCategory.FOOD.displayName)
        assertEquals("🍜", VocabularyCategory.FOOD.icon)
    }

    @Test
    fun `VocabularyDifficulty has correct levels`() {
        assertEquals(5, VocabularyDifficulty.entries.size)
        assertEquals(1, VocabularyDifficulty.BEGINNER.level)
        assertEquals(2, VocabularyDifficulty.ELEMENTARY.level)
        assertEquals(3, VocabularyDifficulty.INTERMEDIATE.level)
        assertEquals(4, VocabularyDifficulty.UPPER_INTERMEDIATE.level)
        assertEquals(5, VocabularyDifficulty.ADVANCED.level)
    }

    @Test
    fun `VocabularySource has all expected entries`() {
        assertEquals(10, VocabularySource.entries.size)
        assertTrue(VocabularySource.entries.contains(VocabularySource.NPC))
        assertTrue(VocabularySource.entries.contains(VocabularySource.DIALOGUE))
        assertTrue(VocabularySource.entries.contains(VocabularySource.QUEST))
        assertTrue(VocabularySource.entries.contains(VocabularySource.FRIENDSHIP))
    }

    @Test
    fun `VocabularyMastery has correct levels`() {
        assertEquals(5, VocabularyMastery.entries.size)
        assertEquals(0, VocabularyMastery.UNKNOWN.level)
        assertEquals(1, VocabularyMastery.SEEN.level)
        assertEquals(2, VocabularyMastery.LEARNING.level)
        assertEquals(3, VocabularyMastery.FAMILIAR.level)
        assertEquals(4, VocabularyMastery.MASTERED.level)
    }

    @Test
    fun `VocabularyWord has correct default values`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        assertEquals("test", word.id)
        assertEquals("nǐ hǎo", word.mandarin)
        assertEquals("nǐ hǎo", word.pinyin)
        assertEquals("hello", word.english)
        assertNull(word.hanzi)
        assertNull(word.audioPath)
        assertEquals(VocabularyCategory.GREETINGS, word.category)
        assertEquals(VocabularyDifficulty.BEGINNER, word.difficulty)
        assertNull(word.relatedNpcId)
        assertNull(word.relatedQuestId)
        assertNull(word.relatedRegionId)
        assertNull(word.discoveredAt)
        assertEquals(VocabularyMastery.UNKNOWN, word.mastery)
        assertEquals(0, word.timesReviewed)
        assertEquals(0, word.timesSpoken)
        assertEquals(0, word.timesHeard)
        assertFalse(word.isFavorite)
        assertNull(word.notes)
        assertTrue(word.tags.isEmpty())
    }

    @Test
    fun `VocabularyWord isDiscovered returns true when discoveredAt is set`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
            discoveredAt = System.currentTimeMillis()
        )
        assertTrue(word.isDiscovered)
    }

    @Test
    fun `VocabularyWord isDiscovered returns false when discoveredAt is null`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        assertFalse(word.isDiscovered)
    }

    @Test
    fun `VocabularyWord displayHanzi returns hanzi when available`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!",
            hanzi = "你好"
        )
        assertEquals("你好", word.displayHanzi)
    }

    @Test
    fun `VocabularyWord displayHanzi returns mandarin when hanzi is null`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        assertEquals("nǐ hǎo", word.displayHanzi)
    }

    @Test
    fun `VocabularyProgress has correct default values`() {
        val progress = VocabularyProgress(wordId = "test", mastery = VocabularyMastery.SEEN)
        assertEquals("test", progress.wordId)
        assertEquals(VocabularyMastery.SEEN, progress.mastery)
        assertEquals(0, progress.timesReviewed)
        assertEquals(0, progress.timesSpoken)
        assertEquals(0, progress.timesHeard)
        assertNull(progress.lastReviewedAt)
        assertNull(progress.discoveredAt)
        assertFalse(progress.isFavorite)
    }

    @Test
    fun `VocabularyExample has correct values`() {
        val example = VocabularyExample(
            sentence = "Nǐ hǎo!",
            translation = "Hello!",
            pinyin = "nǐ hǎo!"
        )
        assertEquals("Nǐ hǎo!", example.sentence)
        assertEquals("Hello!", example.translation)
        assertEquals("nǐ hǎo!", example.pinyin)
    }

    @Test
    fun `VocabularyPronunciation has correct values`() {
        val pronunciation = VocabularyPronunciation(pinyin = "nǐ hǎo", tone = 3)
        assertEquals("nǐ hǎo", pronunciation.pinyin)
        assertEquals(3, pronunciation.tone)
        assertNull(pronunciation.audioPath)
    }

    @Test
    fun `VocabularyTag has correct values`() {
        val tag = VocabularyTag(id = "tag1", name = "greeting", nameCn = "问候")
        assertEquals("tag1", tag.id)
        assertEquals("greeting", tag.name)
        assertEquals("问候", tag.nameCn)
    }

    @Test
    fun `VocabularyStatistics has correct default values`() {
        val stats = VocabularyStatistics()
        assertEquals(0, stats.totalWords)
        assertEquals(0, stats.discoveredWords)
        assertEquals(0, stats.masteredWords)
        assertEquals(0, stats.favoriteWords)
        assertTrue(stats.wordsByCategory.isEmpty())
        assertTrue(stats.wordsByMastery.isEmpty())
        assertTrue(stats.wordsByDifficulty.isEmpty())
        assertEquals(0, stats.totalReviewed)
        assertEquals(0, stats.totalSpoken)
        assertEquals(0, stats.totalHeard)
        assertEquals(0f, stats.completionPercentage, 0.01f)
    }

    @Test
    fun `VocabularyResult Success contains message`() {
        val result = VocabularyResult.Success("Operation successful")
        assertTrue(result is VocabularyResult.Success)
        assertEquals("Operation successful", result.message)
    }

    @Test
    fun `VocabularyResult Error contains message`() {
        val result = VocabularyResult.Error("Something went wrong")
        assertTrue(result is VocabularyResult.Error)
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `VocabularyResult WordDiscovered contains word`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        val result = VocabularyResult.WordDiscovered(word)
        assertTrue(result is VocabularyResult.WordDiscovered)
        assertEquals("test", result.word.id)
    }

    @Test
    fun `VocabularyResult MasteryUpgraded contains word and newMastery`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        val result = VocabularyResult.MasteryUpgraded(word, VocabularyMastery.FAMILIAR)
        assertTrue(result is VocabularyResult.MasteryUpgraded)
        assertEquals("test", result.word.id)
        assertEquals(VocabularyMastery.FAMILIAR, result.newMastery)
    }

    @Test
    fun `VocabularyResult FavoriteToggled contains word and isFavorite`() {
        val word = VocabularyWord(
            id = "test",
            mandarin = "nǐ hǎo",
            pinyin = "nǐ hǎo",
            english = "hello",
            category = VocabularyCategory.GREETINGS,
            difficulty = VocabularyDifficulty.BEGINNER,
            exampleSentence = "Nǐ hǎo!",
            exampleTranslation = "Hello!",
            examplePinyin = "nǐ hǎo!"
        )
        val result = VocabularyResult.FavoriteToggled(word, true)
        assertTrue(result is VocabularyResult.FavoriteToggled)
        assertEquals("test", result.word.id)
        assertTrue(result.isFavorite)
    }

    @Test
    fun `VocabularyResult WordsFound contains words`() {
        val words = listOf(
            VocabularyWord(
                id = "test",
                mandarin = "nǐ hǎo",
                pinyin = "nǐ hǎo",
                english = "hello",
                category = VocabularyCategory.GREETINGS,
                difficulty = VocabularyDifficulty.BEGINNER,
                exampleSentence = "Nǐ hǎo!",
                exampleTranslation = "Hello!",
                examplePinyin = "nǐ hǎo!"
            )
        )
        val result = VocabularyResult.WordsFound(words)
        assertTrue(result is VocabularyResult.WordsFound)
        assertEquals(1, result.words.size)
    }
}
