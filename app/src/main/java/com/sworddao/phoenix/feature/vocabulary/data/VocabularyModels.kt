package com.sworddao.phoenix.feature.vocabulary.data

import kotlinx.serialization.Serializable

@Serializable
enum class VocabularyCategory(val displayName: String, val displayNameCn: String, val icon: String) {
    GREETINGS("Greetings", "问候", "👋"),
    FAMILY("Family", "家庭", "👨‍👩‍👧"),
    FOOD("Food", "食物", "🍜"),
    NUMBERS("Numbers", "数字", "🔢"),
    DIRECTIONS("Directions", "方向", "🧭"),
    TRAVEL("Travel", "旅行", "✈️"),
    SHOPPING("Shopping", "购物", "🛒"),
    RESTAURANT("Restaurant", "餐厅", "🍽️"),
    WEATHER("Weather", "天气", "🌤️"),
    TIME("Time", "时间", "⏰"),
    TRANSPORTATION("Transportation", "交通", "🚌"),
    DAILY_LIFE("Daily Life", "日常生活", "🏠"),
}

@Serializable
enum class VocabularyDifficulty(val displayName: String, val level: Int) {
    BEGINNER("Beginner", 1),
    ELEMENTARY("Elementary", 2),
    INTERMEDIATE("Intermediate", 3),
    UPPER_INTERMEDIATE("Upper Intermediate", 4),
    ADVANCED("Advanced", 5),
}

@Serializable
enum class VocabularySource(val displayName: String) {
    NPC("NPC Interaction"),
    DIALOGUE("Dialogue"),
    QUEST("Quest Reward"),
    FRIENDSHIP("Friendship Milestone"),
    EXPLORATION("Exploration"),
    SHOP("Shop Purchase"),
    FESTIVAL("Festival Event"),
    DAILY("Daily Activity"),
    ACHIEVEMENT("Achievement Reward"),
    HIDDEN("Hidden Discovery"),
}

@Serializable
enum class VocabularyMastery(val displayName: String, val level: Int) {
    UNKNOWN("Unknown", 0),
    SEEN("Seen", 1),
    LEARNING("Learning", 2),
    FAMILIAR("Familiar", 3),
    MASTERED("Mastered", 4),
}

@Serializable
data class VocabularyWord(
    val id: String,
    val mandarin: String,
    val pinyin: String,
    val english: String,
    val hanzi: String? = null,
    val audioPath: String? = null,
    val category: VocabularyCategory,
    val difficulty: VocabularyDifficulty,
    val exampleSentence: String,
    val exampleTranslation: String,
    val examplePinyin: String,
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedRegionId: String? = null,
    val discoveredAt: Long? = null,
    val mastery: VocabularyMastery = VocabularyMastery.UNKNOWN,
    val timesReviewed: Int = 0,
    val timesSpoken: Int = 0,
    val timesHeard: Int = 0,
    val timesRead: Int = 0,
    val timesWritten: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
) {
    val isDiscovered: Boolean
        get() = discoveredAt != null

    val displayHanzi: String
        get() = hanzi ?: mandarin
}

@Serializable
data class VocabularyProgress(
    val wordId: String,
    val mastery: VocabularyMastery,
    val timesReviewed: Int = 0,
    val timesSpoken: Int = 0,
    val timesHeard: Int = 0,
    val timesRead: Int = 0,
    val timesWritten: Int = 0,
    val lastReviewedAt: Long? = null,
    val discoveredAt: Long? = null,
    val isFavorite: Boolean = false,
)

@Serializable
data class VocabularyExample(
    val sentence: String,
    val translation: String,
    val pinyin: String,
)

@Serializable
data class VocabularyPronunciation(
    val pinyin: String,
    val tone: Int,
    val audioPath: String? = null,
)

@Serializable
data class VocabularyTag(
    val id: String,
    val name: String,
    val nameCn: String,
)

@Serializable
data class VocabularyStatistics(
    val totalWords: Int = 0,
    val discoveredWords: Int = 0,
    val masteredWords: Int = 0,
    val favoriteWords: Int = 0,
    val wordsByCategory: Map<VocabularyCategory, Int> = emptyMap(),
    val wordsByMastery: Map<VocabularyMastery, Int> = emptyMap(),
    val wordsByDifficulty: Map<VocabularyDifficulty, Int> = emptyMap(),
    val totalReviewed: Int = 0,
    val totalSpoken: Int = 0,
    val totalHeard: Int = 0,
    val completionPercentage: Float = 0f,
)

sealed class VocabularyResult {
    data class Success(val message: String) : VocabularyResult()
    data class Error(val message: String) : VocabularyResult()
    data class WordDiscovered(val word: VocabularyWord) : VocabularyResult()
    data class MasteryUpgraded(val word: VocabularyWord, val newMastery: VocabularyMastery) : VocabularyResult()
    data class FavoriteToggled(val word: VocabularyWord, val isFavorite: Boolean) : VocabularyResult()
    data class WordsFound(val words: List<VocabularyWord>) : VocabularyResult()
}
