package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDiscoveryRepository @Inject constructor(
    private val vocabularyRepository: VocabularyRepository,
) : DiscoveryRepository {

    private val _discoveries = MutableStateFlow(createInitialDiscoveries())
    private val _sessions = MutableStateFlow<List<DiscoverySession>>(emptyList())
    private val _dailyStreak = MutableStateFlow(0)
    private val _lastDiscoveryDate = MutableStateFlow<System?>(null).let {
        MutableStateFlow<Long?>(null)
    }

    override fun getAllDiscoveries(): Flow<List<VocabularyDiscovery>> = _discoveries

    override fun getDiscoveryById(discoveryId: String): Flow<VocabularyDiscovery?> =
        _discoveries.map { discoveries -> discoveries.find { it.id == discoveryId } }

    override fun getDiscoveriesByWord(wordId: String): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries -> discoveries.filter { it.wordId == wordId } }

    override fun getDiscoveriesBySource(source: DiscoverySourceType): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries -> discoveries.filter { it.source == source } }

    override fun getDiscoveriesByNpc(npcId: String): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries -> discoveries.filter { it.relatedNpcId == npcId } }

    override fun getDiscoveriesByQuest(questId: String): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries -> discoveries.filter { it.relatedQuestId == questId } }

    override fun getDiscoveriesByRegion(regionId: String): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries -> discoveries.filter { it.relatedRegionId == regionId } }

    override fun getDiscoveriesByCategory(category: VocabularyCategory): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries ->
            discoveries.filter { discovery ->
                discovery.word?.category == category
            }
        }

    override fun getRecentDiscoveries(limit: Int): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries ->
            discoveries.sortedByDescending { it.discoveredAt }.take(limit)
        }

    override fun getTodayDiscoveries(): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries ->
            val today = getStartOfDay(System.currentTimeMillis())
            discoveries.filter { it.discoveredAt >= today }
        }

    override fun getWeekDiscoveries(): Flow<List<VocabularyDiscovery>> =
        _discoveries.map { discoveries ->
            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            discoveries.filter { it.discoveredAt >= weekAgo }
        }

    override fun getDiscoveryHistory(): Flow<DiscoveryHistory> =
        _discoveries.map { discoveries ->
            val now = System.currentTimeMillis()
            val today = getStartOfDay(now)
            val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
            val todayCount = discoveries.count { it.discoveredAt >= today }
            val weekCount = discoveries.count { it.discoveredAt >= weekAgo }

            val wordsBySource = discoveries.groupBy { it.source }.mapValues { it.value.size }
            val wordsByCategory = discoveries.mapNotNull { it.word?.category }
                .groupBy { it }
                .mapValues { it.value.size }
            val wordsByRegion = discoveries.mapNotNull { it.relatedRegionId }
                .groupBy { it }
                .mapValues { it.value.size }

            DiscoveryHistory(
                discoveries = discoveries,
                totalCount = discoveries.size,
                todayCount = todayCount,
                weekCount = weekCount,
                streakDays = _dailyStreak.value,
                lastDiscoveryDate = _lastDiscoveryDate.value,
                wordsBySource = wordsBySource,
                wordsByCategory = wordsByCategory,
                wordsByRegion = wordsByRegion,
            )
        }

    override fun getDiscoveryStatistics(): Flow<DiscoveryStatistics> =
        _discoveries.map { discoveries ->
            val now = System.currentTimeMillis()
            val today = getStartOfDay(now)
            val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
            val monthAgo = now - 30 * 24 * 60 * 60 * 1000L
            val todayCount = discoveries.count { it.discoveredAt >= today }
            val weekCount = discoveries.count { it.discoveredAt >= weekAgo }
            val monthCount = discoveries.count { it.discoveredAt >= monthAgo }

            val totalAvailable = 100
            val wordsBySource = discoveries.groupBy { it.source }.mapValues { it.value.size }
            val wordsByCategory = discoveries.mapNotNull { it.word?.category }
                .groupBy { it }
                .mapValues { it.value.size }
            val wordsByMastery = discoveries.mapNotNull { it.word?.mastery }
                .groupBy { it }
                .mapValues { it.value.size }
            val wordsByRegion = discoveries.mapNotNull { it.relatedRegionId }
                .groupBy { it }
                .mapValues { it.value.size }

            val daysSinceFirst = if (discoveries.isNotEmpty()) {
                val firstDiscovery = discoveries.minOf { it.discoveredAt }
                ((now - firstDiscovery) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
            } else 1

            DiscoveryStatistics(
                totalDiscovered = discoveries.size,
                totalAvailable = totalAvailable,
                todayDiscovered = todayCount,
                weekDiscovered = weekCount,
                monthDiscovered = monthCount,
                streakDays = _dailyStreak.value,
                longestStreak = maxOf(_dailyStreak.value, 7),
                lastDiscoveryDate = _lastDiscoveryDate.value,
                wordsBySource = wordsBySource,
                wordsByCategory = wordsByCategory,
                wordsByMastery = wordsByMastery,
                wordsByRegion = wordsByRegion,
                averageDiscoveriesPerDay = discoveries.size.toFloat() / daysSinceFirst,
                completionPercentage = discoveries.size.toFloat() / totalAvailable,
            )
        }

    override fun getDiscoverySessions(): Flow<List<DiscoverySession>> = _sessions

    override fun getDailyDiscoveries(): Flow<List<DailyDiscovery>> =
        _discoveries.map { discoveries ->
            discoveries.groupBy { getStartOfDay(it.discoveredAt) }
                .map { (date, dayDiscoveries) ->
                    DailyDiscovery(
                        date = date,
                        discoveries = dayDiscoveries,
                        totalCount = dayDiscoveries.size,
                        streakDay = isConsecutiveDay(date, discoveries),
                    )
                }
                .sortedByDescending { it.date }
        }

    override fun getStreakDays(): Flow<Int> = _dailyStreak

    override suspend fun discoverWord(
        wordId: String,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String?,
        relatedQuestId: String?,
        relatedRegionId: String?,
    ): DiscoveryResult {
        val existing = _discoveries.value.find { it.wordId == wordId }
        if (existing != null) {
            val word = vocabularyRepository.getWordById(wordId)
                .map { it }.let { flow ->
                    var result: VocabularyWord? = null
                    flow.collect { result = it }
                    result
                }
            return DiscoveryResult.WordAlreadyDiscovered(
                word = word ?: createPlaceholderWord(wordId),
                discovery = existing,
            )
        }

        val word = vocabularyRepository.getWordById(wordId)
            .map { it }.let { flow ->
                var result: VocabularyWord? = null
                flow.collect { result = it }
                result
            } ?: createPlaceholderWord(wordId)

        val isFirst = !_discoveries.value.any { it.wordId == wordId }
        val reward = calculateReward(word, source, isFirst)

        val discovery = VocabularyDiscovery(
            id = UUID.randomUUID().toString(),
            wordId = wordId,
            word = word,
            source = source,
            sourceId = sourceId,
            sourceName = sourceName,
            discoveredAt = System.currentTimeMillis(),
            isFirstDiscovery = isFirst,
            bonusXp = reward.xp,
            bonusFriendshipXp = reward.friendshipXp,
            relatedNpcId = relatedNpcId,
            relatedQuestId = relatedQuestId,
            relatedRegionId = relatedRegionId,
        )

        _discoveries.update { current -> current + discovery }

        vocabularyRepository.discoverWord(wordId)

        updateStreak()

        return DiscoveryResult.WordDiscovered(
            word = word,
            discovery = discovery,
            isFirstDiscovery = isFirst,
            reward = reward,
        )
    }

    override suspend fun discoverWords(
        wordIds: List<String>,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String?,
        relatedQuestId: String?,
        relatedRegionId: String?,
    ): DiscoveryResult {
        val newlyUnlocked = mutableListOf<NewlyUnlockedWord>()
        var totalXp = 0
        var totalFriendshipXp = 0

        for (wordId in wordIds) {
            val result = discoverWord(
                wordId = wordId,
                source = source,
                sourceId = sourceId,
                sourceName = sourceName,
                relatedNpcId = relatedNpcId,
                relatedQuestId = relatedQuestId,
                relatedRegionId = relatedRegionId,
            )

            when (result) {
                is DiscoveryResult.WordDiscovered -> {
                    newlyUnlocked.add(
                        NewlyUnlockedWord(
                            word = result.word,
                            source = source,
                            sourceName = sourceName,
                            discoveredAt = result.discovery.discoveredAt,
                            isFirstDiscovery = result.isFirstDiscovery,
                            reward = result.reward,
                        )
                    )
                    totalXp += result.reward.xp
                    totalFriendshipXp += result.reward.friendshipXp
                }
                is DiscoveryResult.WordAlreadyDiscovered -> {
                    // Skip already discovered
                }
                else -> {}
            }
        }

        return DiscoveryResult.BatchDiscovered(
            words = newlyUnlocked,
            totalXp = totalXp,
            totalFriendshipXp = totalFriendshipXp,
        )
    }

    override suspend fun isWordDiscovered(wordId: String): Boolean =
        _discoveries.value.any { it.wordId == wordId }

    override suspend fun getDiscoveryCount(): Int = _discoveries.value.size

    override suspend fun getDiscoveryCountBySource(source: DiscoverySourceType): Int =
        _discoveries.value.count { it.source == source }

    override suspend fun getDiscoveryCountByRegion(regionId: String): Int =
        _discoveries.value.count { it.relatedRegionId == regionId }

    override suspend fun getDiscoveryCountByCategory(category: VocabularyCategory): Int =
        _discoveries.value.count { it.word?.category == category }

    override suspend fun resetDailyStreak() {
        _dailyStreak.value = 0
    }

    override suspend fun recordDiscoverySession(session: DiscoverySession) {
        _sessions.update { current -> current + session }
    }

    override suspend fun clearDiscoveryHistory() {
        _discoveries.value = emptyList()
        _sessions.value = emptyList()
        _dailyStreak.value = 0
        _lastDiscoveryDate.value = null
    }

    private fun calculateReward(
        word: VocabularyWord,
        source: DiscoverySourceType,
        isFirstDiscovery: Boolean,
    ): DiscoveryReward {
        var xp = 10
        var friendshipXp = 0
        var streakBonus = 0
        var categoryBonus = false
        var regionBonus = false

        if (isFirstDiscovery) {
            xp += 5
        }

        when (source) {
            DiscoverySourceType.NPC -> {
                xp += 5
                friendshipXp = 2
            }
            DiscoverySourceType.DIALOGUE -> {
                xp += 5
                friendshipXp = 3
            }
            DiscoverySourceType.QUEST -> {
                xp += 15
                friendshipXp = 5
            }
            DiscoverySourceType.FRIENDSHIP -> {
                xp += 20
                friendshipXp = 10
                categoryBonus = true
            }
            DiscoverySourceType.REGION -> {
                xp += 10
                regionBonus = true
            }
            DiscoverySourceType.PASSPORT -> {
                xp += 10
            }
            DiscoverySourceType.STORY -> {
                xp += 15
            }
            DiscoverySourceType.LISTENING -> {
                xp += 10
            }
            DiscoverySourceType.SPEAKING -> {
                xp += 12
            }
            DiscoverySourceType.MINI_GAME -> {
                xp += 8
            }
            DiscoverySourceType.FESTIVAL -> {
                xp += 20
                categoryBonus = true
            }
            DiscoverySourceType.HIDDEN -> {
                xp += 25
                categoryBonus = true
                regionBonus = true
            }
            DiscoverySourceType.EXPLORATION -> {
                xp += 10
                regionBonus = true
            }
        }

        when (word.difficulty.level) {
            3 -> xp += 3
            4 -> xp += 5
            5 -> xp += 8
        }

        streakBonus = (_dailyStreak.value * 2).coerceAtMost(20)
        xp += streakBonus

        return DiscoveryReward(
            xp = xp,
            friendshipXp = friendshipXp,
            vocabularyWords = listOf(word.id),
            streakBonus = streakBonus,
            categoryBonus = categoryBonus,
            regionBonus = regionBonus,
        )
    }

    private fun updateStreak() {
        val now = System.currentTimeMillis()
        val today = getStartOfDay(now)
        val lastDate = _lastDiscoveryDate.value

        if (lastDate == null) {
            _dailyStreak.value = 1
        } else {
            val lastDay = getStartOfDay(lastDate)
            val yesterday = getStartOfDay(now - 24 * 60 * 60 * 1000L)

            when {
                lastDay == today -> {
                    // Same day, no change
                }
                lastDay == yesterday -> {
                    _dailyStreak.value += 1
                }
                else -> {
                    _dailyStreak.value = 1
                }
            }
        }
        _lastDiscoveryDate.value = now
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun isConsecutiveDay(date: Long, discoveries: List<VocabularyDiscovery>): Boolean {
        val dayBefore = date - 24 * 60 * 60 * 1000L
        return discoveries.any { getStartOfDay(it.discoveredAt) == getStartOfDay(dayBefore) }
    }

    private fun createPlaceholderWord(wordId: String): VocabularyWord {
        return VocabularyWord(
            id = wordId,
            mandarin = wordId,
            pinyin = wordId,
            english = wordId,
            category = VocabularyCategory.DAILY_LIFE,
            difficulty = VocabularyCategory.DAILY_LIFE.let {
                com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty.BEGINNER
            },
            exampleSentence = "",
            exampleTranslation = "",
            examplePinyin = "",
        )
    }

    private fun createInitialDiscoveries(): List<VocabularyDiscovery> {
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
