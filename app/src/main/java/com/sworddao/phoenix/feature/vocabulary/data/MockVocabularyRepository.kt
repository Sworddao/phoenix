package com.sworddao.phoenix.feature.vocabulary.data

import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockVocabularyRepository @Inject constructor() : VocabularyRepository {

    private val _words = MutableStateFlow(createInitialWords())

    override fun getAllWords(): Flow<List<VocabularyWord>> = _words

    override fun getWordById(wordId: String): Flow<VocabularyWord?> =
        _words.map { words -> words.find { it.id == wordId } }

    override fun getWordsByCategory(category: VocabularyCategory): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.category == category } }

    override fun getWordsByMastery(mastery: VocabularyMastery): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.mastery == mastery } }

    override fun getWordsByDifficulty(difficulty: VocabularyDifficulty): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.difficulty == difficulty } }

    override fun getWordsByRegion(regionId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedRegionId == regionId } }

    override fun getWordsByNpc(npcId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedNpcId == npcId } }

    override fun getWordsByQuest(questId: String): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.relatedQuestId == questId } }

    override fun getDiscoveredWords(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.isDiscovered } }

    override fun getUndiscoveredWords(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { !it.isDiscovered } }

    override fun getFavorites(): Flow<List<VocabularyWord>> =
        _words.map { words -> words.filter { it.isFavorite } }

    override fun getRecentlyLearned(limit: Int): Flow<List<VocabularyWord>> =
        _words.map { words ->
            words.filter { it.isDiscovered }
                .sortedByDescending { it.discoveredAt }
                .take(limit)
        }

    override fun searchWords(query: String): Flow<List<VocabularyWord>> =
        _words.map { words ->
            val lowerQuery = query.lowercase()
            words.filter { word ->
                word.pinyin.lowercase().contains(lowerQuery) ||
                    word.english.lowercase().contains(lowerQuery) ||
                    word.mandarin.contains(query) ||
                    word.hanzi?.contains(query) == true
            }
        }

    override fun getStatistics(): Flow<VocabularyStatistics> =
        _words.map { words ->
            val discovered = words.filter { it.isDiscovered }
            VocabularyStatistics(
                totalWords = words.size,
                discoveredWords = discovered.size,
                masteredWords = words.count { it.mastery == VocabularyMastery.MASTERED },
                favoriteWords = words.count { it.isFavorite },
                wordsByCategory = words.groupBy { it.category }.mapValues { it.value.size },
                wordsByMastery = words.groupBy { it.mastery }.mapValues { it.value.size },
                wordsByDifficulty = words.groupBy { it.difficulty }.mapValues { it.value.size },
                totalReviewed = words.sumOf { it.timesReviewed },
                totalSpoken = words.sumOf { it.timesSpoken },
                totalHeard = words.sumOf { it.timesHeard },
                completionPercentage = if (words.isNotEmpty()) discovered.size.toFloat() / words.size else 0f,
            )
        }

    override fun getCategories(): Flow<List<VocabularyCategory>> =
        MutableStateFlow(VocabularyCategory.entries.toList())

    override fun getProgress(wordId: String): Flow<VocabularyProgress?> =
        _words.map { words ->
            val word = words.find { it.id == wordId }
            word?.let {
                VocabularyProgress(
                    wordId = it.id,
                    mastery = it.mastery,
                    timesReviewed = it.timesReviewed,
                    timesSpoken = it.timesSpoken,
                    timesHeard = it.timesHeard,
                    discoveredAt = it.discoveredAt,
                    isFavorite = it.isFavorite,
                )
            }
        }

    override suspend fun discoverWord(wordId: String): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        if (word.isDiscovered) {
            return VocabularyResult.Error("Word already discovered")
        }

        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(
                    discoveredAt = System.currentTimeMillis(),
                    mastery = VocabularyMastery.SEEN,
                ) else w
            }
        }

        return VocabularyResult.WordDiscovered(word.copy(discoveredAt = System.currentTimeMillis()))
    }

    override suspend fun updateMastery(wordId: String, mastery: VocabularyMastery): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(mastery = mastery) else w
            }
        }

        return VocabularyResult.MasteryUpgraded(word, mastery)
    }

    override suspend fun toggleFavorite(wordId: String): VocabularyResult {
        val words = _words.value
        val word = words.find { it.id == wordId }
            ?: return VocabularyResult.Error("Word not found")

        val newFavorite = !word.isFavorite
        _words.update { wordList ->
            wordList.map { w ->
                if (w.id == wordId) w.copy(isFavorite = newFavorite) else w
            }
        }

        return VocabularyResult.FavoriteToggled(word, newFavorite)
    }

    override suspend fun incrementReview(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesReviewed = w.timesReviewed + 1) else w
            }
        }
        return VocabularyResult.Success("Review recorded")
    }

    override suspend fun incrementSpoken(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesSpoken = w.timesSpoken + 1) else w
            }
        }
        return VocabularyResult.Success("Spoken recorded")
    }

    override suspend fun incrementHeard(wordId: String): VocabularyResult {
        _words.update { wordList: List<VocabularyWord> ->
            wordList.map { w: VocabularyWord ->
                if (w.id == wordId) w.copy(timesHeard = w.timesHeard + 1) else w
            }
        }
        return VocabularyResult.Success("Heard recorded")
    }

    override suspend fun recordDiscovery(wordId: String, source: VocabularySource): VocabularyResult {
        return discoverWord(wordId)
    }

    override suspend fun addWords(words: List<VocabularyWord>): VocabularyResult {
        _words.update { currentWords -> currentWords + words }
        return VocabularyResult.Success("Words added: ${words.size}")
    }

    private fun createInitialWords(): List<VocabularyWord> = listOf(
        // Greetings
        VocabularyWord("greet_001", "nǐ hǎo", "nǐ hǎo", "hello", "你好", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ hǎo! Wǒ jiào Bao.", exampleTranslation = "Hello! My name is Bao.", examplePinyin = "nǐ hǎo! wǒ jiào bao.", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("greet_002", "zàijiàn", "zài jiàn", "goodbye", "再见", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zàijiàn! Míngtiān jiàn.", exampleTranslation = "Goodbye! See you tomorrow.", examplePinyin = "zài jiàn! míng tiān jiàn.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 12, timesSpoken = 6, timesHeard = 15),
        VocabularyWord("greet_003", "xièxie", "xiè xie", "thank you", "谢谢", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Xièxie nǐ de bāngzhù!", exampleTranslation = "Thank you for your help!", examplePinyin = "xiè xie nǐ de bāng zhù!", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 10, timesHeard = 25),
        VocabularyWord("greet_004", "bù kèqì", "bù kè qì", "you're welcome", "不客气", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Bù kèqì, zhè shì wǒ yīnggāi zuò de.", exampleTranslation = "You're welcome, this is what I should do.", examplePinyin = "bù kè qì, zhè shì wǒ yīng gāi zuò de.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 12),
        VocabularyWord("greet_005", "duìbuqǐ", "duì bu qǐ", "sorry", "对不起", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Duìbuqǐ, wǒ lái wǎn le.", exampleTranslation = "Sorry, I'm late.", examplePinyin = "duì bu qǐ, wǒ lái wǎn le.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 8, timesSpoken = 3, timesHeard = 10),
        VocabularyWord("greet_006", "méi guānxi", "méi guān xi", "it's okay", "没关系", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Méi guānxi, bùyào jǐn.", exampleTranslation = "It's okay, don't worry.", examplePinyin = "méi guān xi, bù yào jǐn.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 2, timesHeard = 8),
        VocabularyWord("greet_007", "nǐ hǎo ma", "nǐ hǎo ma", "how are you", "你好吗", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ hǎo ma? Wǒ hěn hǎo!", exampleTranslation = "How are you? I'm very good!", examplePinyin = "nǐ hǎo ma? wǒ hěn hǎo!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 4, timesHeard = 11),
        VocabularyWord("greet_008", "hěn gāoxìng", "hěn gāo xìng", "nice to meet you", "很高兴", category = VocabularyCategory.GREETINGS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Hěn gāoxìng rènshi nǐ!", exampleTranslation = "Nice to meet you!", examplePinyin = "hěn gāo xìng rèn shi nǐ!", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 7, timesSpoken = 4, timesHeard = 9),

        // Family
        VocabularyWord("fam_001", "māma", "mā ma", "mother", "妈妈", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ de māma hěn hǎo.", exampleTranslation = "My mother is very good.", examplePinyin = "wǒ de mā ma hěn hǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 3, timesHeard = 10),
        VocabularyWord("fam_002", "bàba", "bà ba", "father", "爸爸", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ de bàba shì gōngzuò.", exampleTranslation = "My father is working.", examplePinyin = "wǒ de bà ba shì gōng zuò.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 7, timesSpoken = 3, timesHeard = 9),
        VocabularyWord("fam_003", "yéye", "yé ye", "grandfather", "爷爷", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Yéye xǐhuan hē chá.", exampleTranslation = "Grandfather likes to drink tea.", examplePinyin = "yé ye xǐ huan hē chá.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("fam_004", "nǎinai", "nǎi nai", "grandmother", "奶奶", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǎinai zuò de fàn hěn hǎo chī.", exampleTranslation = "Grandmother's cooking is delicious.", examplePinyin = "nǎi nai zuò de fàn hěn hǎo chī.", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 4, timesHeard = 11),
        VocabularyWord("fam_005", "gēge", "gē ge", "older brother", "哥哥", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ gēge zài dàxué.", exampleTranslation = "My older brother is at university.", examplePinyin = "wǒ gē ge zài dà xué.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("fam_006", "mèimei", "mèi mei", "younger sister", "妹妹", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ mèimei hěn kě'ài.", exampleTranslation = "My younger sister is very cute.", examplePinyin = "wǒ mèi mei hěn kě ài.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.LEARNING, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("fam_007", "jiārén", "jiā rén", "family", "家人", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ de jiārén dōu hěn hǎo.", exampleTranslation = "My family is all very good.", examplePinyin = "wǒ de jiā rén dōu hěn hǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 2, timesHeard = 8),
        VocabularyWord("fam_008", "péngyou", "péng you", "friend", "朋友", category = VocabularyCategory.FAMILY, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ shì wǒ de hǎo péngyou.", exampleTranslation = "You are my good friend.", examplePinyin = "nǐ shì wǒ de hǎo péng you.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 14, timesSpoken = 7, timesHeard = 18),

        // Food
        VocabularyWord("food_001", "chīfàn", "chī fàn", "eat a meal", "吃饭", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒmen yīqǐ chīfàn ba!", exampleTranslation = "Let's eat together!", examplePinyin = "wǒ men yī qǐ chī fàn ba!", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 16, timesSpoken = 9, timesHeard = 22),
        VocabularyWord("food_002", "hē", "hē", "drink", "喝", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ yào hē shénme?", exampleTranslation = "What do you want to drink?", examplePinyin = "nǐ yào hē shén me?", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("food_003", "fàn", "fàn", "rice/meal", "饭", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge fàn hěn hǎo chī.", exampleTranslation = "This rice is very delicious.", examplePinyin = "zhè ge fàn hěn hǎo chī.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 11, timesSpoken = 6, timesHeard = 14),
        VocabularyWord("food_004", "shuǐguǒ", "shuǐ guǒ", "fruit", "水果", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ xǐhuan chī shuǐguǒ.", exampleTranslation = "I like to eat fruit.", examplePinyin = "wǒ xǐ huan chī shuǐ guǒ.", relatedRegionId = "jade_forest", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("food_005", "jiǎozi", "jiǎo zi", "dumplings", "饺子", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ māma zuò de jiǎozi hěn hǎo chī.", exampleTranslation = "My mother's dumplings are delicious.", examplePinyin = "wǒ mā ma zuò de jiǎo zi hěn hǎo chī.", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 20, timesSpoken = 12, timesHeard = 25),
        VocabularyWord("food_006", "chá", "chá", "tea", "茶", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Qǐng hē chá.", exampleTranslation = "Please drink tea.", examplePinyin = "qǐng hē chá.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("food_007", "mǐfàn", "mǐ fàn", "cooked rice", "米饭", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Yī wǎn mǐfàn, xièxie.", exampleTranslation = "One bowl of rice, thank you.", examplePinyin = "yī wǎn mǐ fàn, xiè xie.", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 7, timesSpoken = 4, timesHeard = 9),
        VocabularyWord("food_008", "miànbāo", "miàn bāo", "bread", "面包", category = VocabularyCategory.FOOD, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào yī gè miànbāo.", exampleTranslation = "I want one bread.", examplePinyin = "wǒ yào yī gè miàn bāo.", relatedRegionId = "qingyuan_village", relatedNpcId = "grandma_mei", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 11),

        // Numbers
        VocabularyWord("num_001", "yī", "yī", "one", "一", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Yī, èr, sān!", exampleTranslation = "One, two, three!", examplePinyin = "yī, èr, sān!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.MASTERED, timesReviewed = 20, timesSpoken = 10, timesHeard = 25),
        VocabularyWord("num_002", "èr", "èr", "two", "二", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào èr gè.", exampleTranslation = "I want two.", examplePinyin = "wǒ yào èr gè.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 9, timesHeard = 22),
        VocabularyWord("num_003", "sān", "sān", "three", "三", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Sān ge rén.", exampleTranslation = "Three people.", examplePinyin = "sān ge rén.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 9, timesHeard = 22),
        VocabularyWord("num_004", "shí", "shí", "ten", "十", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Shí kuài qián.", exampleTranslation = "Ten yuan.", examplePinyin = "shí kuài qián.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 12, timesSpoken = 6, timesHeard = 15),
        VocabularyWord("num_005", "bǎi", "bǎi", "hundred", "百", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Yī bǎi kuài qián.", exampleTranslation = "One hundred yuan.", examplePinyin = "yī bǎi kuài qián.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("num_006", "qiān", "qiān", "thousand", "千", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Yī qiān mǐ.", exampleTranslation = "One thousand meters.", examplePinyin = "yī qiān mǐ.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 1, timesHeard = 6),
        VocabularyWord("num_007", "wàn", "wàn", "ten thousand", "万", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Shí wàn rén.", exampleTranslation = "One hundred thousand people.", examplePinyin = "shí wàn rén.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("num_008", "duōshǎo", "duō shǎo", "how much", "多少", category = VocabularyCategory.NUMBERS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge duōshǎo qián?", exampleTranslation = "How much is this?", examplePinyin = "zhè ge duō shǎo qián?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),

        // Directions
        VocabularyWord("dir_001", "zuǒ", "zuǒ", "left", "左", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǎng zuǒ zhuǎn.", exampleTranslation = "Turn left.", examplePinyin = "wǎng zuǒ zhuǎn.", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("dir_002", "yòu", "yòu", "right", "右", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǎng yòu zhuǎn.", exampleTranslation = "Turn right.", examplePinyin = "wǎng yòu zhuǎn.", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("dir_003", "zhízǒu", "zhí zǒu", "go straight", "直走", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Qǐng zhízǒu.", exampleTranslation = "Please go straight.", examplePinyin = "qǐng zhí zǒu.", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("dir_004", "dōng", "dōng", "east", "东", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Dōng biān yǒu yī gè dàshù.", exampleTranslation = "There is a big tree on the east side.", examplePinyin = "dōng biān yǒu yī gè dà shù.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("dir_005", "xī", "xī", "west", "西", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Xī biān shì shān.", exampleTranslation = "The west side is mountains.", examplePinyin = "xī biān shì shān.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("dir_006", "nán", "nán", "south", "南", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Nán biān yǒu yī gè chēzhàn.", exampleTranslation = "There is a station on the south side.", examplePinyin = "nán biān yǒu yī gè chē zhàn.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("dir_007", "běi", "běi", "north", "北", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Běi biān hěn lěng.", exampleTranslation = "The north side is very cold.", examplePinyin = "běi biān hěn lěng.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("dir_008", "zài nǎlǐ", "zài nǎ lǐ", "where is it", "在哪里", category = VocabularyCategory.DIRECTIONS, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Chēzhàn zài nǎlǐ?", exampleTranslation = "Where is the station?", examplePinyin = "chē zhàn zài nǎ lǐ?", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),

        // Travel
        VocabularyWord("trav_001", "lǚxíng", "lǚ xíng", "travel", "旅行", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ xǐhuan lǚxíng.", exampleTranslation = "I like to travel.", examplePinyin = "wǒ xǐ huan lǚ xíng.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("trav_002", "huǒchē", "huǒ chē", "train", "火车", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Huǒchē hěn kuài.", exampleTranslation = "The train is very fast.", examplePinyin = "huǒ chē hěn kuài.", relatedRegionId = "high_speed_rail", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("trav_003", "fēijī", "fēi jī", "airplane", "飞机", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Fēijī yào qǐfēi le.", exampleTranslation = "The airplane is about to take off.", examplePinyin = "fēi jī yào qǐ fēi le.", relatedRegionId = "shanghai", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("trav_004", "jiǔdiàn", "jiǔ diàn", "hotel", "酒店", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Jiǔdiàn zài nǎlǐ?", exampleTranslation = "Where is the hotel?", examplePinyin = "jiǔ diàn zài nǎ lǐ?", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("trav_005", "xíngli", "xíng li", "luggage", "行李", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ de xíngli hěn zhòng.", exampleTranslation = "My luggage is very heavy.", examplePinyin = "wǒ de xíng li hěn zhòng.", relatedRegionId = "high_speed_rail", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("trav_006", "zhàopiàn", "zhào piàn", "photo", "照片", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒmen yīqǐ zhào piàn!", exampleTranslation = "Let's take a photo together!", examplePinyin = "wǒ men yī qǐ zhào piàn!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 7, timesSpoken = 4, timesHeard = 9),
        VocabularyWord("trav_007", "dàochù", "dào chù", "everywhere", "到处", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Zhèlǐ dàochù dōu shì huā.", exampleTranslation = "There are flowers everywhere here.", examplePinyin = "zhè lǐ dào chù dōu shì huā.", relatedRegionId = "jade_forest", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("trav_008", "jǐngsè", "jǐng sè", "scenery", "景色", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Zhèlǐ de jǐngsè hěn měi.", exampleTranslation = "The scenery here is very beautiful.", examplePinyin = "zhè lǐ de jǐng sè hěn měi.", relatedRegionId = "jade_forest", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),

        // Shopping
        VocabularyWord("shop_001", "mǎi", "mǎi", "buy", "买", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào mǎi zhè ge.", exampleTranslation = "I want to buy this.", examplePinyin = "wǒ yào mǎi zhè ge.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 12),
        VocabularyWord("shop_002", "mài", "mài", "sell", "卖", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge mài duōshǎo qián?", exampleTranslation = "How much does this sell for?", examplePinyin = "zhè ge mài duō shǎo qián?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("shop_003", "guì", "guì", "expensive", "贵", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge tài guì le!", exampleTranslation = "This is too expensive!", examplePinyin = "zhè ge tài guì le!", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("shop_004", "piányi", "pián yi", "cheap", "便宜", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge hěn piányi.", exampleTranslation = "This is very cheap.", examplePinyin = "zhè ge hěn pián yi.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("shop_005", "qián", "qián", "money", "钱", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ méiyǒu qián le.", exampleTranslation = "I don't have money anymore.", examplePinyin = "wǒ méi yǒu qián le.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("shop_006", "yào", "yào", "want", "要", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào zhè ge.", exampleTranslation = "I want this.", examplePinyin = "wǒ yào zhè ge.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 14, timesSpoken = 8, timesHeard = 18),
        VocabularyWord("shop_007", "kàn", "kàn", "look", "看", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ kàn yīxià.", exampleTranslation = "Let me take a look.", examplePinyin = "wǒ kàn yī xià.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("shop_008", "fùkuǎn", "fù kuǎn", "payment", "付款", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Zěnme fùkuǎn?", exampleTranslation = "How do I pay?", examplePinyin = "zěn me fù kuǎn?", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),

        // Restaurant
        VocabularyWord("rest_001", "cài", "cài", "dish/food", "菜", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Yào shénme cài?", exampleTranslation = "What dish do you want?", examplePinyin = "yào shén me cài?", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("rest_002", "míngdān", "míng dān", "menu", "菜单", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Qǐng gěi wǒ míngdān.", exampleTranslation = "Please give me the menu.", examplePinyin = "qǐng gěi wǒ míng dān.", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("rest_003", "dùzi", "dù zi", "stomach/hungry", "肚子", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ dùzi è le.", exampleTranslation = "I'm hungry.", examplePinyin = "wǒ dù zi è le.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("rest_004", "hǎo chī", "hǎo chī", "delicious", "好吃", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge hǎo chī!", exampleTranslation = "This is delicious!", examplePinyin = "zhè ge hǎo chī!", relatedRegionId = "qingyuan_village", relatedNpcId = "owner_lin", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 16, timesSpoken = 9, timesHeard = 20),
        VocabularyWord("rest_005", "kě yǐ", "kě yǐ", "can/may", "可以", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Kě yǐ zuò zhèlǐ ma?", exampleTranslation = "Can I sit here?", examplePinyin = "kě yǐ zuò zhè lǐ ma?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 11),
        VocabularyWord("rest_006", "wèidào", "wèi dào", "taste/flavor", "味道", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Zhè ge wèidào hěn hǎo.", exampleTranslation = "This taste is very good.", examplePinyin = "zhè ge wèi dào hěn hǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("rest_007", "rè", "rè", "hot", "热", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge fàn tài rè le.", exampleTranslation = "This food is too hot.", examplePinyin = "zhè ge fàn tài rè le.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("rest_008", "lěng", "lěng", "cold", "冷", category = VocabularyCategory.RESTAURANT, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Chá yào rè de, bùyào lěng de.", exampleTranslation = "Tea should be hot, not cold.", examplePinyin = "chá yào rè de, bù yào lěng de.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),

        // Weather
        VocabularyWord("weath_001", "tiānqì", "tiān qì", "weather", "天气", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Jīntiān tiānqì hěn hǎo.", exampleTranslation = "The weather is very good today.", examplePinyin = "jīn tiān tiān qì hěn hǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("weath_002", "xià yǔ", "xià yǔ", "rain", "下雨", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Míngtiān huì xià yǔ.", exampleTranslation = "It will rain tomorrow.", examplePinyin = "míng tiān huì xià yǔ.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("weath_003", "qíng", "qíng", "sunny", "晴", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Jīntiān shì qíng tiān.", exampleTranslation = "Today is a sunny day.", examplePinyin = "jīn tiān shì qíng tiān.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("weath_004", "yǔ", "yǔ", "rain", "雨", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Yǔ hěn dà.", exampleTranslation = "The rain is heavy.", examplePinyin = "yǔ hěn dà.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("weath_005", "xuě", "xuě", "snow", "雪", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Dōngtiān huì xià xuě.", exampleTranslation = "It snows in winter.", examplePinyin = "dōng tiān huì xià xuě.", relatedRegionId = "beijing", discoveredAt = System.currentTimeMillis() - 86400000 * 3, mastery = VocabularyMastery.SEEN, timesReviewed = 2, timesSpoken = 1, timesHeard = 3),
        VocabularyWord("weath_006", "fēng", "fēng", "wind", "风", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Jīntiān fēng hěn dà.", exampleTranslation = "The wind is very strong today.", examplePinyin = "jīn tiān fēng hěn dà.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("weath_007", "duōyún", "duō yún", "cloudy", "多云", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Míngtiān shì duōyún tiānqì.", exampleTranslation = "Tomorrow will be cloudy.", examplePinyin = "míng tiān shì duō yún tiān qì.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("weath_008", "wēndù", "wēn dù", "temperature", "温度", category = VocabularyCategory.WEATHER, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Jīntiān wēndù duōshāo?", exampleTranslation = "What's the temperature today?", examplePinyin = "jīn tiān wēn dù duō shāo?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),

        // Time
        VocabularyWord("time_001", "jīntiān", "jīn tiān", "today", "今天", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Jīntiān shì xīngqī jǐ?", exampleTranslation = "What day is today?", examplePinyin = "jīn tiān shì xīng qī jǐ?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("time_002", "míngtiān", "míng tiān", "tomorrow", "明天", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Míngtiān jiàn!", exampleTranslation = "See you tomorrow!", examplePinyin = "míng tiān jiàn!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("time_003", "zuótiān", "zuó tiān", "yesterday", "昨天", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zuótiān wǒ qù le shìchǎng.", exampleTranslation = "Yesterday I went to the market.", examplePinyin = "zuó tiān wǒ qù le shì chǎng.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("time_004", "shíjiān", "shí jiān", "time", "时间", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Xiànzài shì shénme shíjiān?", exampleTranslation = "What time is it now?", examplePinyin = "xiànzài shì shén me shí jiān?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("time_005", "zǎoshang", "zǎo shang", "morning", "早上", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zǎoshang hǎo!", exampleTranslation = "Good morning!", examplePinyin = "zǎo shang hǎo!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 11),
        VocabularyWord("time_006", "wǔshàng", "wǔ shang", "noon", "中午", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǔshàng wǒmen chīfàn.", exampleTranslation = "We eat at noon.", examplePinyin = "wǔ shang wǒ men chī fàn.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("time_007", "wǎnshàng", "wǎn shang", "evening", "晚上", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǎnshàng hǎo!", exampleTranslation = "Good evening!", examplePinyin = "wǎn shang hǎo!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("time_008", "xiànzài", "xiàn zài", "now", "现在", category = VocabularyCategory.TIME, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Xiànzài jǐ diǎn le?", exampleTranslation = "What time is it now?", examplePinyin = "xiàn zài jǐ diǎn le?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),

        // Transportation
        VocabularyWord("trans_001", "chē", "chē", "vehicle", "车", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nà ge shì shénme chē?", exampleTranslation = "What vehicle is that?", examplePinyin = "nà ge shì shén me chē?", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 11),
        VocabularyWord("trans_002", "gōngjiāochē", "gōng jiāo chē", "bus", "公交车", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Gōngjiāochē lái le!", exampleTranslation = "The bus is here!", examplePinyin = "gōng jiāo chē lái le!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("trans_003", "chūzūchē", "chū zū chē", "taxi", "出租车", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Qǐng jiào yī liàng chūzūchē.", exampleTranslation = "Please call a taxi.", examplePinyin = "qǐng jiào yī liàng chū zū chē.", relatedRegionId = "qingyuan_village", relatedNpcId = "taxi_chen", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("trans_004", "dìtiě", "dì tiě", "subway", "地铁", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Dìtiě hěn kuài.", exampleTranslation = "The subway is very fast.", examplePinyin = "dì tiě hěn kuài.", relatedRegionId = "shanghai", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("trans_005", "qí", "qí", "ride", "骑", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ huì qí zìxíngchē.", exampleTranslation = "I can ride a bicycle.", examplePinyin = "wǒ huì qí zì xíng chē.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("trans_006", "zǒu", "zǒu", "walk", "走", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒmen zǒu ba!", exampleTranslation = "Let's walk!", examplePinyin = "wǒ men zǒu ba!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 12, timesSpoken = 7, timesHeard = 15),
        VocabularyWord("trans_007", "kuài", "kuài", "fast", "快", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Kuài diǎn! Huǒchē yào zǒu le.", exampleTranslation = "Hurry! The train is leaving.", examplePinyin = "kuài diǎn! huǒ chē yào zǒu le.", relatedRegionId = "high_speed_rail", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("trans_008", "màn", "màn", "slow", "慢", category = VocabularyCategory.TRANSPORTATION, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Màn yīdiǎn, bùyào jí.", exampleTranslation = "Slow down, don't rush.", examplePinyin = "màn yī diǎn, bù yào jí.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),

        // Daily Life
        VocabularyWord("daily_001", "shuìjiào", "shuì jiào", "sleep", "睡觉", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǎnshàng shí diǎn shuìjiào.", exampleTranslation = "Sleep at 10 PM.", examplePinyin = "wǎn shang shí diǎn shuì jiào.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("daily_002", "qǐchuáng", "qǐ chuáng", "get up", "起床", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zǎoshang qī diǎn qǐchuáng.", exampleTranslation = "Get up at 7 AM.", examplePinyin = "zǎo shang qī diǎn qǐ chuáng.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("daily_003", "xǐzǎo", "xǐ zǎo", "take a bath", "洗澡", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào xǐzǎo.", exampleTranslation = "I want to take a bath.", examplePinyin = "wǒ yào xǐ zǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("daily_004", "chuān", "chuān", "wear", "穿", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ chuān shénme yīfu?", exampleTranslation = "What are you wearing?", examplePinyin = "nǐ chuān shén me yī fu?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("daily_005", "yīfu", "yī fu", "clothes", "衣服", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè jiàn yīfu hěn piàoliang.", exampleTranslation = "This piece of clothing is very beautiful.", examplePinyin = "zhè jiàn yī fu hěn piào liang.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("daily_006", "xǐ", "xǐ", "wash", "洗", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào xǐ yīfu.", exampleTranslation = "I want to wash clothes.", examplePinyin = "wǒ yào xǐ yī fu.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),
        VocabularyWord("daily_007", "gōngzuò", "gōng zuò", "work", "工作", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ zài nǎlǐ gōngzuò?", exampleTranslation = "Where do you work?", examplePinyin = "nǐ zài nǎ lǐ gōng zuò?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("daily_008", "xiūxi", "xiū xi", "rest", "休息", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒmen xiūxi yīxià ba.", exampleTranslation = "Let's rest a bit.", examplePinyin = "wǒ men xiū xi yī xià ba.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),

        // Additional words for regions
        VocabularyWord("reg_001", "shān", "shān", "mountain", "山", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge shān hěn gāo.", exampleTranslation = "This mountain is very tall.", examplePinyin = "zhè ge shān hěn gāo.", relatedRegionId = "mountain_temple", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("reg_002", "hé", "hé", "river", "河", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Hé biān yǒu yī gè cūnzhuāng.", exampleTranslation = "There is a village by the river.", examplePinyin = "hé biān yǒu yī gè cūn zhuāng.", relatedRegionId = "riverside_town", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 5),
        VocabularyWord("reg_003", "sì", "sì", "temple", "寺", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.ELEMENTARY, exampleSentence = "Zhè ge sì hěn lǎo.", exampleTranslation = "This temple is very old.", examplePinyin = "zhè ge sì hěn lǎo.", relatedRegionId = "mountain_temple", discoveredAt = System.currentTimeMillis() - 86400000 * 4, mastery = VocabularyMastery.SEEN, timesReviewed = 3, timesSpoken = 1, timesHeard = 4),
        VocabularyWord("reg_004", "chéngshì", "chéng shì", "city", "城市", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge chéngshì hěn dà.", exampleTranslation = "This city is very big.", examplePinyin = "zhè ge chéng shì hěn dà.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("reg_005", "cūnzhuāng", "cūn zhuāng", "village", "村庄", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge cūnzhuāng hěn měi.", exampleTranslation = "This village is very beautiful.", examplePinyin = "zhè ge cūn zhuāng hěn měi.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("reg_006", "shìchǎng", "shì chǎng", "market", "市场", category = VocabularyCategory.SHOPPING, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Shìchǎng lǐ hěn rènao.", exampleTranslation = "The market is very lively.", examplePinyin = "shì chǎng lǐ hěn rè nao.", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("reg_007", "gōngyuán", "gōng yuán", "park", "公园", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Gōngyuán lǐ yǒu hěn duō shù.", exampleTranslation = "There are many trees in the park.", examplePinyin = "gōng yuán lǐ yǒu hěn duō shù.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),
        VocabularyWord("reg_008", "lǚguǎn", "lǚ guǎn", "inn/guesthouse", "旅馆", category = VocabularyCategory.TRAVEL, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Lǚguǎn zài nǎlǐ?", exampleTranslation = "Where is the inn?", examplePinyin = "lǚ guǎn zài nǎ lǐ?", relatedRegionId = "jingdezhen", discoveredAt = System.currentTimeMillis() - 86400000 * 5, mastery = VocabularyMastery.LEARNING, timesReviewed = 4, timesSpoken = 2, timesHeard = 6),

        // Common words
        VocabularyWord("common_001", "shì", "shì", "is/am/are", "是", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ shì xuéshēng.", exampleTranslation = "I am a student.", examplePinyin = "wǒ shì xué shēng.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 20, timesSpoken = 12, timesHeard = 25),
        VocabularyWord("common_002", "yǒu", "yǒu", "have", "有", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yǒu yī gè wèntí.", exampleTranslation = "I have a question.", examplePinyin = "wǒ yǒu yī gè wèn tí.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 10, timesHeard = 22),
        VocabularyWord("common_003", "qù", "qù", "go", "去", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yào qù shìchǎng.", exampleTranslation = "I want to go to the market.", examplePinyin = "wǒ yào qù shì chǎng.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("common_004", "lái", "lái", "come", "来", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Kuài lái!", exampleTranslation = "Come quickly!", examplePinyin = "kuài lái!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 14, timesSpoken = 8, timesHeard = 18),
        VocabularyWord("common_005", "zài", "zài", "at/in/on", "在", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ zài jiālǐ.", exampleTranslation = "I am at home.", examplePinyin = "wǒ zài jiā lǐ.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 10, timesHeard = 22),
        VocabularyWord("common_006", "bù", "bù", "not", "不", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Bù yòng xièxie.", exampleTranslation = "No need to thank.", examplePinyin = "bù yòng xiè xie.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 20, timesSpoken = 12, timesHeard = 25),
        VocabularyWord("common_007", "hěn", "hěn", "very", "很", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè ge hěn hǎo.", exampleTranslation = "This is very good.", examplePinyin = "zhè ge hěn hǎo.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 18, timesSpoken = 10, timesHeard = 22),
        VocabularyWord("common_008", "shénme", "shén me", "what", "什么", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ zài zuò shénme?", exampleTranslation = "What are you doing?", examplePinyin = "nǐ zài zuò shén me?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("common_009", "zhè", "zhè", "this", "这", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Zhè shì shénme?", exampleTranslation = "What is this?", examplePinyin = "zhè shì shén me?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 15, timesSpoken = 8, timesHeard = 20),
        VocabularyWord("common_010", "nà", "nà", "that", "那", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nà shì shénme?", exampleTranslation = "What is that?", examplePinyin = "nà shì shén me?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 9, mastery = VocabularyMastery.MASTERED, timesReviewed = 14, timesSpoken = 8, timesHeard = 18),
        VocabularyWord("common_011", "wǒ", "wǒ", "I/me", "我", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ jiào Bao.", exampleTranslation = "My name is Bao.", examplePinyin = "wǒ jiào bao.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 25, timesSpoken = 15, timesHeard = 30),
        VocabularyWord("common_012", "nǐ", "nǐ", "you", "你", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ hǎo!", exampleTranslation = "Hello!", examplePinyin = "nǐ hǎo!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 25, timesSpoken = 15, timesHeard = 30),
        VocabularyWord("common_013", "tā", "tā", "he/she", "他", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Tā shì wǒ de péngyou.", exampleTranslation = "He is my friend.", examplePinyin = "tā shì wǒ de péng you.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("common_014", "hǎo", "hǎo", "good", "好", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Hǎo de!", exampleTranslation = "Okay!", examplePinyin = "hǎo de!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 10, mastery = VocabularyMastery.MASTERED, timesReviewed = 20, timesSpoken = 12, timesHeard = 25),
        VocabularyWord("common_015", "duì", "duì", "correct", "对", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Duì! Hěn hǎo!", exampleTranslation = "Correct! Very good!", examplePinyin = "duì! hěn hǎo!", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 10, timesSpoken = 5, timesHeard = 13),
        VocabularyWord("common_016", "wèntí", "wèn tí", "question/problem", "问题", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ yǒu yī gè wèntí.", exampleTranslation = "I have a question.", examplePinyin = "wǒ yǒu yī gè wèn tí.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("common_017", "méiyǒu", "méi yǒu", "don't have", "没有", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ méiyǒu qián.", exampleTranslation = "I don't have money.", examplePinyin = "wǒ méi yǒu qián.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 8, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 9, timesSpoken = 5, timesHeard = 11),
        VocabularyWord("common_018", "kěyǐ", "kě yǐ", "can/may", "可以", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ kěyǐ zuò ma?", exampleTranslation = "Can I sit?", examplePinyin = "wǒ kě yǐ zuò ma?", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.FAMILIAR, timesReviewed = 8, timesSpoken = 4, timesHeard = 10),
        VocabularyWord("common_019", "dōu", "dōu", "all", "都", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒmen dōu qù.", exampleTranslation = "We all go.", examplePinyin = "wǒ men dōu qù.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 7, mastery = VocabularyMastery.LEARNING, timesReviewed = 6, timesSpoken = 3, timesHeard = 8),
        VocabularyWord("common_020", "hái", "hái", "still/also", "还", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ hái yào.", exampleTranslation = "I still want.", examplePinyin = "wǒ hái yào.", relatedRegionId = "qingyuan_village", discoveredAt = System.currentTimeMillis() - 86400000 * 6, mastery = VocabularyMastery.LEARNING, timesReviewed = 5, timesSpoken = 2, timesHeard = 7),

        // Undiscovered words (for testing discoverWord)
        VocabularyWord("undiscovered_001", "xué", "xué", "study/learn", "学", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ zài xué zhōngwén.", exampleTranslation = "I am learning Chinese.", examplePinyin = "wǒ zài xué zhōng wén.", relatedRegionId = "qingyuan_village"),
        VocabularyWord("undiscovered_002", "jiào", "jiào", "call/name", "叫", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Wǒ jiào Bao.", exampleTranslation = "My name is Bao.", examplePinyin = "wǒ jiào bao.", relatedRegionId = "qingyuan_village"),
        VocabularyWord("undiscovered_003", "zuò", "zuò", "sit/do/make", "做", category = VocabularyCategory.DAILY_LIFE, difficulty = VocabularyDifficulty.BEGINNER, exampleSentence = "Nǐ zài zuò shénme?", exampleTranslation = "What are you doing?", examplePinyin = "nǐ zài zuò shén me?", relatedRegionId = "qingyuan_village"),
    )
}
