package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.data.local.AppMetadataDao
import com.sworddao.phoenix.data.local.AppMetadataEntity
import com.sworddao.phoenix.data.seed.DiscoverySeedData
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDao
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import com.sworddao.phoenix.feature.vocabulary.data.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomDiscoveryRepository @Inject constructor(
    private val dao: DiscoveryDao,
    private val vocabularyDao: VocabularyDao,
    private val metadataDao: AppMetadataDao,
) : DiscoveryRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private val streakFlow = MutableStateFlow(0)
    private val lastDateFlow = MutableStateFlow<Long?>(null)

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.countAll() == 0) {
                dao.upsertAll(DiscoverySeedData.createInitialDiscoveries().map { it.toEntity() })
            }
            streakFlow.value = metadataDao.getValue(KEY_STREAK)?.toIntOrNull() ?: 0
            lastDateFlow.value = metadataDao.getValue(KEY_LAST_DATE)?.toLongOrNull()
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getAllDiscoveries(): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getAllDiscoveries().map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveryById(discoveryId: String): Flow<VocabularyDiscovery?> =
        seededFlow { dao.getDiscoveryById(discoveryId).map { it?.toDomain() } }

    override fun getDiscoveriesByWord(wordId: String): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getDiscoveriesByWord(wordId).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveriesBySource(source: DiscoverySourceType): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getDiscoveriesBySource(source.name).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveriesByNpc(npcId: String): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getDiscoveriesByNpc(npcId).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveriesByQuest(questId: String): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getDiscoveriesByQuest(questId).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveriesByRegion(regionId: String): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getDiscoveriesByRegion(regionId).map { list -> list.map { it.toDomain() } } }

    override fun getDiscoveriesByCategory(category: VocabularyCategory): Flow<List<VocabularyDiscovery>> =
        seededFlow {
            dao.getAllDiscoveries().map { discoveries ->
                discoveries.map { it.toDomain() }.filter { it.word?.category == category }
            }
        }

    override fun getRecentDiscoveries(limit: Int): Flow<List<VocabularyDiscovery>> =
        seededFlow { dao.getRecentDiscoveries(limit).map { list -> list.map { it.toDomain() } } }

    override fun getTodayDiscoveries(): Flow<List<VocabularyDiscovery>> =
        seededFlow {
            dao.getDiscoveriesAfter(getStartOfDay(System.currentTimeMillis()))
                .map { list -> list.map { it.toDomain() } }
        }

    override fun getWeekDiscoveries(): Flow<List<VocabularyDiscovery>> =
        seededFlow {
            dao.getDiscoveriesAfter(System.currentTimeMillis() - WEEK_MILLIS)
                .map { list -> list.map { it.toDomain() } }
        }

    override fun getDiscoveryHistory(): Flow<DiscoveryHistory> = seededFlow {
        combine(dao.getAllDiscoveries(), streakFlow) { entities, streak ->
            val discoveries = entities.map { it.toDomain() }
            val now = System.currentTimeMillis()
            val today = getStartOfDay(now)
            val weekAgo = now - WEEK_MILLIS
            DiscoveryHistory(
                discoveries = discoveries,
                totalCount = discoveries.size,
                todayCount = discoveries.count { it.discoveredAt >= today },
                weekCount = discoveries.count { it.discoveredAt >= weekAgo },
                streakDays = streak,
                lastDiscoveryDate = lastDateFlow.value,
                wordsBySource = discoveries.groupBy { it.source }.mapValues { it.value.size },
                wordsByCategory = discoveries.mapNotNull { it.word?.category }
                    .groupBy { it }.mapValues { it.value.size },
                wordsByRegion = discoveries.mapNotNull { it.relatedRegionId }
                    .groupBy { it }.mapValues { it.value.size },
            )
        }
    }

    override fun getDiscoveryStatistics(): Flow<DiscoveryStatistics> = seededFlow {
        combine(dao.getAllDiscoveries(), streakFlow) { entities, streak ->
            val discoveries = entities.map { it.toDomain() }
            val now = System.currentTimeMillis()
            val today = getStartOfDay(now)
            val weekAgo = now - WEEK_MILLIS
            val monthAgo = now - MONTH_MILLIS
            val totalAvailable = 100
            val daysSinceFirst = if (discoveries.isNotEmpty()) {
                val firstDiscovery = discoveries.minOf { it.discoveredAt }
                ((now - firstDiscovery) / DAY_MILLIS).toInt().coerceAtLeast(1)
            } else 1
            DiscoveryStatistics(
                totalDiscovered = discoveries.size,
                totalAvailable = totalAvailable,
                todayDiscovered = discoveries.count { it.discoveredAt >= today },
                weekDiscovered = discoveries.count { it.discoveredAt >= weekAgo },
                monthDiscovered = discoveries.count { it.discoveredAt >= monthAgo },
                streakDays = streak,
                longestStreak = maxOf(streak, 7),
                lastDiscoveryDate = lastDateFlow.value,
                wordsBySource = discoveries.groupBy { it.source }.mapValues { it.value.size },
                wordsByCategory = discoveries.mapNotNull { it.word?.category }
                    .groupBy { it }.mapValues { it.value.size },
                wordsByMastery = discoveries.mapNotNull { it.word?.mastery }
                    .groupBy { it }.mapValues { it.value.size },
                wordsByRegion = discoveries.mapNotNull { it.relatedRegionId }
                    .groupBy { it }.mapValues { it.value.size },
                averageDiscoveriesPerDay = discoveries.size.toFloat() / daysSinceFirst,
                completionPercentage = discoveries.size.toFloat() / totalAvailable,
            )
        }
    }

    override fun getDiscoverySessions(): Flow<List<DiscoverySession>> =
        seededFlow { dao.getAllSessions().map { list -> list.map { it.toDomain() } } }

    override fun getDailyDiscoveries(): Flow<List<DailyDiscovery>> = seededFlow {
        dao.getAllDiscoveries().map { entities ->
            val discoveries = entities.map { it.toDomain() }
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
    }

    override fun getStreakDays(): Flow<Int> = seededFlow { flow { emit(streakFlow.value) } }

    override suspend fun discoverWord(
        wordId: String,
        source: DiscoverySourceType,
        sourceId: String,
        sourceName: String,
        relatedNpcId: String?,
        relatedQuestId: String?,
        relatedRegionId: String?,
    ): DiscoveryResult {
        ensureSeeded()
        val existing = dao.getDiscoveryByWord(wordId).first()
        if (existing != null) {
            val word = vocabularyDao.getWordById(wordId).first()?.toDomain()
                ?: createPlaceholderWord(wordId)
            return DiscoveryResult.WordAlreadyDiscovered(
                word = word,
                discovery = existing.toDomain(),
            )
        }

        val word = vocabularyDao.getWordById(wordId).first()?.toDomain()
            ?: createPlaceholderWord(wordId)
        val isFirst = dao.countByWord(wordId) == 0
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

        dao.upsert(discovery.toEntity())
        vocabularyDao.discoverWord(wordId, discovery.discoveredAt)
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
                else -> {}
            }
        }

        return DiscoveryResult.BatchDiscovered(
            words = newlyUnlocked,
            totalXp = totalXp,
            totalFriendshipXp = totalFriendshipXp,
        )
    }

    override suspend fun isWordDiscovered(wordId: String): Boolean {
        ensureSeeded()
        return dao.countByWord(wordId) > 0
    }

    override suspend fun getDiscoveryCount(): Int {
        ensureSeeded()
        return dao.countAll()
    }

    override suspend fun getDiscoveryCountBySource(source: DiscoverySourceType): Int {
        ensureSeeded()
        return dao.countBySource(source.name)
    }

    override suspend fun getDiscoveryCountByRegion(regionId: String): Int {
        ensureSeeded()
        return dao.countByRegion(regionId)
    }

    override suspend fun getDiscoveryCountByCategory(category: VocabularyCategory): Int {
        ensureSeeded()
        return dao.getAllDiscoveries().first().map { it.toDomain() }.count { it.word?.category == category }
    }

    override suspend fun resetDailyStreak() {
        ensureSeeded()
        streakFlow.value = 0
        metadataDao.setValue(AppMetadataEntity(KEY_STREAK, "0"))
    }

    override suspend fun recordDiscoverySession(session: DiscoverySession) {
        ensureSeeded()
        dao.upsertSession(session.toEntity())
    }

    override suspend fun clearDiscoveryHistory() {
        ensureSeeded()
        dao.clearAll()
        dao.clearSessions()
        streakFlow.value = 0
        lastDateFlow.value = null
        metadataDao.setValue(AppMetadataEntity(KEY_STREAK, "0"))
        metadataDao.delete(KEY_LAST_DATE)
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
            DiscoverySourceType.PASSPORT -> xp += 10
            DiscoverySourceType.STORY -> xp += 15
            DiscoverySourceType.LISTENING -> xp += 10
            DiscoverySourceType.SPEAKING -> xp += 12
            DiscoverySourceType.MINI_GAME -> xp += 8
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

        streakBonus = (streakFlow.value * 2).coerceAtMost(20)
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

    private suspend fun updateStreak() {
        val now = System.currentTimeMillis()
        val today = getStartOfDay(now)
        val lastDate = lastDateFlow.value

        val newStreak = if (lastDate == null) {
            1
        } else {
            val lastDay = getStartOfDay(lastDate)
            val yesterday = getStartOfDay(now - DAY_MILLIS)
            when {
                lastDay == today -> streakFlow.value
                lastDay == yesterday -> streakFlow.value + 1
                else -> 1
            }
        }
        streakFlow.value = newStreak
        lastDateFlow.value = now
        metadataDao.setValue(AppMetadataEntity(KEY_STREAK, newStreak.toString()))
        metadataDao.setValue(AppMetadataEntity(KEY_LAST_DATE, now.toString()))
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
        val dayBefore = date - DAY_MILLIS
        return discoveries.any { getStartOfDay(it.discoveredAt) == getStartOfDay(dayBefore) }
    }

    private fun createPlaceholderWord(wordId: String): VocabularyWord = VocabularyWord(
        id = wordId,
        mandarin = wordId,
        pinyin = wordId,
        english = wordId,
        category = VocabularyCategory.DAILY_LIFE,
        difficulty = VocabularyDifficulty.BEGINNER,
        exampleSentence = "",
        exampleTranslation = "",
        examplePinyin = "",
    )

    private companion object {
        const val KEY_STREAK = "discovery_streak"
        const val KEY_LAST_DATE = "discovery_last_date"
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val WEEK_MILLIS = 7 * DAY_MILLIS
        const val MONTH_MILLIS = 30 * DAY_MILLIS
    }
}
