package com.sworddao.phoenix.feature.progression.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.discovery.data.DiscoveryRepository
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import com.sworddao.phoenix.feature.progression.domain.ProgressionRepository
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import com.sworddao.phoenix.feature.world.data.RegionStatus
import com.sworddao.phoenix.feature.world.data.WorldRegion
import com.sworddao.phoenix.feature.world.domain.WorldRepository
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomProgressionRepository @Inject constructor(
    private val dao: ProgressionDao,
    private val gameProgressRepository: GameProgressRepository,
    private val worldRepository: WorldRepository,
    private val questRepository: QuestRepository,
    private val passportRepository: PassportRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val friendshipRepository: FriendshipRepository,
    private val discoveryRepository: DiscoveryRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val listeningRepository: ListeningRepository,
    private val readingRepository: ReadingRepository,
    private val writingRepository: WritingRepository,
) : ProgressionRepository {

    private val seeded = AtomicBoolean(false)
    private val seedLock = Mutex()

    private suspend fun ensureSeeded() {
        if (seeded.get()) return
        seedLock.withLock {
            if (seeded.get()) return
            if (dao.getStateDocOnce() == null) {
                dao.upsertStateDoc(ProgressionStateEntity(
                    id = "all",
                    lastTotalXp = 0,
                    lastLevel = 1,
                    dailyDate = today(),
                    goalStreak = 0,
                    lastUnlockedChaptersJson = RoomJson.toJsonList(emptyList<String>()),
                ))
            }
            if (dao.getSnapshotDocOnce() == null) {
                dao.upsertSnapshotDoc(ProgressionSnapshotEntity("all", null))
            }
            if (dao.getDailyDocOnce() == null) {
                dao.upsertDailyDoc(ProgressionDailyEntity("all", RoomJson.toJson(DailyProgress(date = today()))))
            }
            if (dao.getRecentDocOnce() == null) {
                dao.upsertRecentDoc(ProgressionRecentEntity("all", "[]"))
            }
            if (dao.getFeaturesDocOnce() == null) {
                dao.upsertFeaturesDoc(ProgressionFeaturesEntity(
                    "all",
                    RoomJson.toJsonList(FeatureUnlock.entries.map { FeatureUnlockEntry(feature = it) }),
                ))
            }
            if (dao.getPlayerDocOnce() == null) {
                dao.upsertPlayerDoc(ProgressionPlayerEntity("all", RoomJson.toJson(PlayerProgress())))
            }
            if (dao.getLearningDocOnce() == null) {
                dao.upsertLearningDoc(ProgressionLearningEntity("all", RoomJson.toJson(LearningProgress())))
            }
            if (dao.getObjectivesDocOnce() == null) {
                dao.upsertObjectivesDoc(ProgressionObjectivesEntity("all", "[]"))
            }
            seeded.set(true)
        }
    }

    private fun <T> seededFlow(block: () -> Flow<T>): Flow<T> = flow {
        ensureSeeded()
        emitAll(block())
    }

    override fun getPlayerProgress(): Flow<PlayerProgress> =
        seededFlow { dao.getPlayerDoc().map { doc -> RoomJson.fromJsonOrNull<PlayerProgress>(doc?.playerJson) ?: PlayerProgress() } }

    override fun getLearningProgress(): Flow<LearningProgress> =
        seededFlow { dao.getLearningDoc().map { doc -> RoomJson.fromJsonOrNull<LearningProgress>(doc?.learningJson) ?: LearningProgress() } }

    override fun getDailyProgress(): Flow<DailyProgress> =
        seededFlow { dao.getDailyDoc().map { doc -> RoomJson.fromJsonOrNull<DailyProgress>(doc?.dailyJson) ?: DailyProgress() } }

    override fun getRecentUnlocks(): Flow<List<RecentUnlock>> =
        seededFlow { dao.getRecentDoc().map { doc -> RoomJson.fromJsonList<RecentUnlock>(doc?.recentJson) } }

    override fun getCurrentObjectives(): Flow<List<CurrentObjective>> =
        seededFlow { dao.getObjectivesDoc().map { doc -> RoomJson.fromJsonList<CurrentObjective>(doc?.objectivesJson) } }

    override fun getFeatureUnlockTimeline(): Flow<List<FeatureUnlockEntry>> =
        seededFlow { dao.getFeaturesDoc().map { doc -> RoomJson.fromJsonList<FeatureUnlockEntry>(doc?.timelineJson) } }

    // ---------------------------------------------------------------------
    // State helpers
    // ---------------------------------------------------------------------

    private data class ProgressionStateHolder(
        val lastTotalXp: Int = 0,
        val lastLevel: Int = 1,
        val dailyDate: String = "",
        val goalStreak: Int = 0,
        val lastUnlockedChapters: Set<String> = emptySet(),
    )

    private suspend fun loadState(): ProgressionStateHolder {
        val doc = dao.getStateDocOnce() ?: return ProgressionStateHolder(dailyDate = today())
        return ProgressionStateHolder(
            lastTotalXp = doc.lastTotalXp,
            lastLevel = doc.lastLevel,
            dailyDate = doc.dailyDate,
            goalStreak = doc.goalStreak,
            lastUnlockedChapters = RoomJson.fromJsonList<String>(doc.lastUnlockedChaptersJson).toSet(),
        )
    }

    private suspend fun saveState(state: ProgressionStateHolder) {
        dao.upsertStateDoc(ProgressionStateEntity(
            id = "all",
            lastTotalXp = state.lastTotalXp,
            lastLevel = state.lastLevel,
            dailyDate = state.dailyDate,
            goalStreak = state.goalStreak,
            lastUnlockedChaptersJson = RoomJson.toJsonList(state.lastUnlockedChapters.toList()),
        ))
    }

    private suspend fun loadSnapshot(): SourceSnapshot? =
        RoomJson.fromJsonOrNull(dao.getSnapshotDocOnce()?.snapshotJson)

    private suspend fun saveSnapshot(snapshot: SourceSnapshot?) {
        dao.upsertSnapshotDoc(ProgressionSnapshotEntity("all", snapshot?.let { RoomJson.toJson(it) }))
    }

    private suspend fun loadDaily(): DailyProgress =
        RoomJson.fromJsonOrNull(dao.getDailyDocOnce()?.dailyJson) ?: DailyProgress(date = today())

    private suspend fun saveDaily(daily: DailyProgress) {
        dao.upsertDailyDoc(ProgressionDailyEntity("all", RoomJson.toJson(daily)))
    }

    private suspend fun loadRecent(): MutableList<RecentUnlock> =
        RoomJson.fromJsonList<RecentUnlock>(dao.getRecentDocOnce()?.recentJson).toMutableList()

    private suspend fun saveRecent(recent: List<RecentUnlock>) {
        dao.upsertRecentDoc(ProgressionRecentEntity("all", RoomJson.toJsonList(recent)))
    }

    private suspend fun loadFeatures(): MutableList<FeatureUnlockEntry> =
        RoomJson.fromJsonList<FeatureUnlockEntry>(dao.getFeaturesDocOnce()?.timelineJson).toMutableList()

    private suspend fun saveFeatures(features: List<FeatureUnlockEntry>) {
        dao.upsertFeaturesDoc(ProgressionFeaturesEntity("all", RoomJson.toJsonList(features)))
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    override suspend fun refresh(): ProgressionResult {
        ensureSeeded()
        val gameProgress = gameProgressRepository.getGameProgress().first()
        val regions = worldRepository.getAllRegions().first()
        val questStats = questRepository.getQuestStats().first()
        val passport = passportRepository.getPassport().first()
        val vocabularyStats = vocabularyRepository.getStatistics().first()
        val friendshipStates = friendshipRepository.getAllFriendshipStates().first()
        val speakingStats = pronunciationRepository.getSpeakingStatistics().first()
        val listeningStats = listeningRepository.getListeningStatistics().first()
        val readingStats = readingRepository.getReadingStatistics().first()
        val writingStats = writingRepository.getWritingStatistics().first()
        val discoveryStats = discoveryRepository.getDiscoveryStatistics().first()

        val snapshot = buildSnapshot(
            gameProgress = gameProgress,
            regions = regions,
            questStats = questStats,
            passport = passport,
            vocabularyStats = vocabularyStats,
            friendshipStates = friendshipStates,
            speakingStats = speakingStats,
            listeningStats = listeningStats,
            readingStats = readingStats,
        )

        applySnapshotDeltas(snapshot)

        val learning = buildLearningProgress(
            regions = regions,
            questStats = questStats,
            passport = passport,
            vocabularyStats = vocabularyStats,
            friendshipStates = friendshipStates,
            speakingStats = speakingStats,
            listeningStats = listeningStats,
            readingStats = readingStats,
            writingStats = writingStats,
            discoveryStats = discoveryStats,
            gameProgress = gameProgress,
        )
        dao.upsertLearningDoc(ProgressionLearningEntity("all", RoomJson.toJson(learning)))

        val player = buildPlayerProgress(
            learning = learning,
            regions = regions,
            gameProgress = gameProgress,
        )
        dao.upsertPlayerDoc(ProgressionPlayerEntity("all", RoomJson.toJson(player)))

        val objectives = buildObjectives(
            regions = regions,
            gameProgress = gameProgress,
            friendshipStates = friendshipStates,
            discoveryStats = discoveryStats,
        )
        dao.upsertObjectivesDoc(ProgressionObjectivesEntity("all", RoomJson.toJsonList(objectives)))

        saveSnapshot(snapshot)

        return ProgressionResult.Refreshed(
            playerProgress = player,
            learningProgress = learning,
        )
    }

    override suspend fun awardXp(source: XpSource, count: Int): ProgressionResult {
        ensureSeeded()
        val amount = source.baseXp * count
        recordDailyActivity(source, count)
        addRecentUnlock(
            title = source.displayName,
            description = "获得 $amount XP",
            icon = source.icon,
        )

        var state = loadState()
        val newTotalXp = state.lastTotalXp + amount
        val newLevel = XpCalculator.levelForTotalXp(newTotalXp)
        val levelUp = newLevel > state.lastLevel
        val unlockedFeatures = if (levelUp) {
            FeatureUnlock.entries
                .filter { it.requiredLevel <= newLevel && it.requiredLevel > state.lastLevel }
        } else {
            emptyList()
        }

        if (levelUp) {
            addRecentUnlock(
                title = "升级！Level $newLevel",
                description = "你达到了新的玩家等级！",
                icon = "⭐",
            )
            unlockedFeatures.forEach { feature ->
                markFeatureUnlocked(feature)
                addRecentUnlock(
                    title = "解锁：${feature.displayNameCn}",
                    description = feature.description,
                    icon = feature.icon,
                )
            }
        }

        state = state.copy(lastTotalXp = newTotalXp, lastLevel = newLevel)
        saveState(state)

        val regions = worldRepository.getAllRegions().first()
        checkChapterUnlocks(regions)

        val learning = buildLearningProgress(
            regions = regions,
            questStats = questRepository.getQuestStats().first(),
            passport = passportRepository.getPassport().first(),
            vocabularyStats = vocabularyRepository.getStatistics().first(),
            friendshipStates = friendshipRepository.getAllFriendshipStates().first(),
            speakingStats = pronunciationRepository.getSpeakingStatistics().first(),
            listeningStats = listeningRepository.getListeningStatistics().first(),
            readingStats = readingRepository.getReadingStatistics().first(),
            writingStats = writingRepository.getWritingStatistics().first(),
            discoveryStats = discoveryRepository.getDiscoveryStatistics().first(),
            gameProgress = gameProgressRepository.getGameProgress().first(),
        )
        dao.upsertLearningDoc(ProgressionLearningEntity("all", RoomJson.toJson(learning)))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        dao.upsertPlayerDoc(ProgressionPlayerEntity("all", RoomJson.toJson(
            buildPlayerProgress(
                learning = learning,
                regions = regions,
                gameProgress = gameProgress,
            )
        )))

        dao.upsertObjectivesDoc(ProgressionObjectivesEntity("all", RoomJson.toJsonList(
            buildObjectives(
                regions = regions,
                gameProgress = gameProgress,
                friendshipStates = friendshipRepository.getAllFriendshipStates().first(),
                discoveryStats = discoveryRepository.getDiscoveryStatistics().first(),
            )
        )))

        return if (levelUp) {
            ProgressionResult.LevelUp(newLevel, unlockedFeatures)
        } else {
            ProgressionResult.XpAwarded(source, amount, newLevel)
        }
    }

    override suspend fun resetProgression() {
        ensureSeeded()
        dao.upsertPlayerDoc(ProgressionPlayerEntity("all", RoomJson.toJson(PlayerProgress())))
        dao.upsertLearningDoc(ProgressionLearningEntity("all", RoomJson.toJson(LearningProgress())))
        dao.upsertDailyDoc(ProgressionDailyEntity("all", RoomJson.toJson(DailyProgress(date = today()))))
        dao.upsertRecentDoc(ProgressionRecentEntity("all", "[]"))
        dao.upsertObjectivesDoc(ProgressionObjectivesEntity("all", "[]"))
        dao.upsertFeaturesDoc(ProgressionFeaturesEntity(
            "all",
            RoomJson.toJsonList(FeatureUnlock.entries.map { FeatureUnlockEntry(feature = it) }),
        ))
        saveSnapshot(null)
        saveState(ProgressionStateHolder(dailyDate = today()))
    }

    // ---------------------------------------------------------------------
    // Snapshot delta detection
    // ---------------------------------------------------------------------

    private suspend fun applySnapshotDeltas(snapshot: SourceSnapshot) {
        val previous = loadSnapshot()
        var state = loadState()
        if (previous == null) {
            state = state.copy(lastTotalXp = 0, lastLevel = 1)
            saveState(state)
            return
        }

        val gains = snapshotGains(snapshot, previous)
        if (gains.isEmpty()) return

        var xp = 0
        gains.forEach { (source, count) ->
            xp += source.baseXp * count
            recordDailyActivity(source, count)
            addRecentUnlock(
                title = source.displayName,
                description = "获得 ${source.baseXp * count} XP",
                icon = source.icon,
            )
        }

        val newTotalXp = state.lastTotalXp + xp
        val newLevel = XpCalculator.levelForTotalXp(newTotalXp)
        if (newLevel > state.lastLevel) {
            val newlyUnlocked = FeatureUnlock.entries
                .filter { it.requiredLevel <= newLevel && it.requiredLevel > state.lastLevel }
            addRecentUnlock(
                title = "升级！Level $newLevel",
                description = "你达到了新的玩家等级！",
                icon = "⭐",
            )
            newlyUnlocked.forEach { feature ->
                markFeatureUnlocked(feature)
                addRecentUnlock(
                    title = "解锁：${feature.displayNameCn}",
                    description = feature.description,
                    icon = feature.icon,
                )
            }
        }

        state = state.copy(lastTotalXp = newTotalXp, lastLevel = newLevel)
        saveState(state)

        val regions = worldRepository.getAllRegions().first()
        checkChapterUnlocks(regions)
    }

    private fun snapshotGains(
        snapshot: SourceSnapshot,
        previous: SourceSnapshot,
    ): List<Pair<XpSource, Int>> = buildList {
        add(Pair(XpSource.DIALOGUE, snapshot.dialogues - previous.dialogues))
        add(Pair(XpSource.VOCABULARY_DISCOVERY, snapshot.wordsDiscovered - previous.wordsDiscovered))
        add(Pair(XpSource.QUEST_COMPLETION, snapshot.questsCompleted - previous.questsCompleted))
        add(Pair(XpSource.FRIENDSHIP_LEVEL_UP, snapshot.friendshipLevels - previous.friendshipLevels))
        add(Pair(XpSource.PASSPORT_STAMP, snapshot.passportStamps - previous.passportStamps))
        add(Pair(XpSource.SPEAKING_PRACTICE, snapshot.speakingPractices - previous.speakingPractices))
        add(Pair(XpSource.LISTENING_PRACTICE, snapshot.listeningPractices - previous.listeningPractices))
        add(Pair(XpSource.READING_PRACTICE, snapshot.readingPractices - previous.readingPractices))
        add(Pair(XpSource.WRITING_PRACTICE, snapshot.writingPractices - previous.writingPractices))
        add(Pair(XpSource.EXPLORATION, (snapshot.regionsUnlocked - previous.regionsUnlocked) +
            (snapshot.regionsCompleted - previous.regionsCompleted)))
        add(Pair(XpSource.ACHIEVEMENT, snapshot.achievements - previous.achievements))
    }.filter { it.second > 0 }

    // ---------------------------------------------------------------------
    // Daily progress
    // ---------------------------------------------------------------------

    private suspend fun recordDailyActivity(source: XpSource, count: Int) {
        val now = today()
        var state = loadState()
        var daily = loadDaily()
        if (now != state.dailyDate) {
            if (daily.isGoalReached) {
                state = state.copy(goalStreak = state.goalStreak + 1)
            }
            state = state.copy(dailyDate = now)
            daily = DailyProgress(
                date = now,
                dailyGoal = daily.dailyGoal,
                goalStreak = state.goalStreak,
            )
            saveState(state)
        }

        val byType = daily.activitiesByType.toMutableMap()
        byType[source] = (byType[source] ?: 0) + count
        val activities = daily.activitiesCompletedToday + count
        val goalReachedBefore = daily.isGoalReached

        val updated = daily.copy(
            date = now,
            xpEarnedToday = daily.xpEarnedToday + source.baseXp * count,
            activitiesCompletedToday = activities,
            activitiesByType = byType,
        )
        saveDaily(updated)

        if (!goalReachedBefore && updated.isGoalReached) {
            addRecentUnlock(
                title = "每日目标达成",
                description = "完成了今天的每日目标！",
                icon = "🎯",
            )
        }
    }

    // ---------------------------------------------------------------------
    // Unlock helpers
    // ---------------------------------------------------------------------

    private suspend fun markFeatureUnlocked(feature: FeatureUnlock) {
        val updated = loadFeatures().map { entry ->
            if (entry.feature == feature) {
                entry.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
            } else {
                entry
            }
        }
        saveFeatures(updated)
    }

    private suspend fun checkChapterUnlocks(regions: List<WorldRegion>) {
        val sorted = regions.sortedBy { it.order }
        var state = loadState()
        var changed = false
        sorted.forEachIndexed { index, region ->
            val chapterId = "chapter_${index + 1}"
            if (chapterId in state.lastUnlockedChapters) return@forEachIndexed

            val previous = if (index > 0) sorted[index - 1] else null
            val requirementMet = when {
                index == 0 -> true
                previous == null -> false
                else -> {
                    val previousCompleted = previous.status == RegionStatus.COMPLETED
                    val levelOk = XpCalculator.levelForTotalXp(state.lastTotalXp) >= (1 + index / 2)
                    levelOk || previousCompleted
                }
            }

            if (requirementMet && region.isUnlocked) {
                state = state.copy(lastUnlockedChapters = state.lastUnlockedChapters + chapterId)
                changed = true
                addRecentUnlock(
                    title = "新章节解锁：${region.nameCn}",
                    description = region.name,
                    icon = region.icon,
                )
            }
        }
        if (changed) {
            saveState(state)
        }
    }

    private fun chapterCatalog(regions: List<WorldRegion>): List<ChapterInfo> {
        val sorted = regions.sortedBy { it.order }
        return sorted.mapIndexed { index, region ->
            val previous = if (index > 0) sorted[index - 1] else null
            ChapterInfo(
                id = "chapter_${index + 1}",
                title = region.name,
                titleCn = region.nameCn,
                order = index + 1,
                regionId = region.id,
                icon = region.icon,
                isUnlocked = region.isUnlocked,
                isCompleted = region.status == RegionStatus.COMPLETED,
                completionPercentage = region.completionPercentage,
                unlockRequirement = ChapterUnlockRequirement(
                    requiredRegionId = previous?.id,
                    requiredLevel = 1 + (index / 2),
                    requiredQuestId = previous?.questIds?.lastOrNull(),
                ),
            )
        }
    }

    private suspend fun addRecentUnlock(title: String, description: String, icon: String) {
        val recent = (listOf(
            RecentUnlock(title = title, description = description, icon = icon)
        ) + loadRecent()).take(MAX_RECENT_UNLOCKS)
        saveRecent(recent)
    }

    // ---------------------------------------------------------------------
    // Progress computation
    // ---------------------------------------------------------------------

    private suspend fun buildLearningProgress(
        regions: List<WorldRegion>,
        questStats: com.sworddao.phoenix.feature.quest.data.QuestStats,
        passport: com.sworddao.phoenix.feature.passport.data.Passport,
        vocabularyStats: com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics,
        friendshipStates: List<com.sworddao.phoenix.feature.friendship.data.FriendshipState>,
        speakingStats: com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics,
        listeningStats: com.sworddao.phoenix.feature.listening.data.ListeningStatistics,
        readingStats: com.sworddao.phoenix.feature.reading.data.ReadingStatistics,
        writingStats: com.sworddao.phoenix.feature.writing.data.WritingStatistics,
        discoveryStats: com.sworddao.phoenix.feature.discovery.data.DiscoveryStatistics,
        gameProgress: com.sworddao.phoenix.feature.gameplay.data.GameProgress,
    ): LearningProgress {
        val discovered = vocabularyStats.discoveredWords.coerceAtLeast(1)
        val friendshipMaxLevel = friendshipStates.maxOfOrNull { it.friendshipLevel.level } ?: 0
        val discoveryPercent = if (discoveryStats.totalAvailable > 0) {
            discoveryStats.totalDiscovered.toFloat() / discoveryStats.totalAvailable
        } else {
            0f
        }

        return LearningProgress(
            speakingPercent = (speakingStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            listeningPercent = (listeningStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            readingPercent = (readingStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            writingPercent = (writingStats.charactersMastered.toFloat() / discovered).coerceIn(0f, 1f),
            vocabularyPercent = discoveryPercent.coerceIn(0f, 1f),
            conversationPercent = (gameProgress.totalDialoguesCompleted.toFloat() / CONVERSATION_TARGET).coerceIn(0f, 1f),
            questPercent = questStats.completionRate.coerceIn(0f, 1f),
            friendshipPercent = (friendshipMaxLevel.toFloat() / MAX_FRIENDSHIP_LEVEL).coerceIn(0f, 1f),
            explorationPercent = (regions.count { it.isUnlocked }.toFloat() / regions.size.coerceAtLeast(1)).coerceIn(0f, 1f),
            passportPercent = passport.completionPercentage.coerceIn(0f, 1f),
        )
    }

    private suspend fun buildPlayerProgress(
        learning: LearningProgress,
        regions: List<WorldRegion>,
        gameProgress: com.sworddao.phoenix.feature.gameplay.data.GameProgress,
    ): PlayerProgress {
        val state = loadState()
        val level = XpCalculator.levelForTotalXp(state.lastTotalXp)
        val chapters = chapterCatalog(regions)
        val unlockedRegions = regions.filter { it.isUnlocked }
        val unlockedNpcs = unlockedRegions.flatMap { it.npcIds }.distinct()
        val unlockedFeatures = FeatureUnlock.entries.filter { it.requiredLevel <= level }
        val currentChapter = chapters.firstOrNull { it.isUnlocked && !it.isCompleted }?.order
            ?: chapters.size.coerceAtLeast(1)
        val village = regions.find { it.id == "qingyuan_village" }
        val chapterRegions = chapters.filter { it.order == currentChapter }
        val chapterProgress = if (chapterRegions.isEmpty()) 0f else {
            chapterRegions.sumOf { it.completionPercentage.toDouble() }
                .div(chapterRegions.size).toFloat()
        }

        val overall = (
            learning.overallPercent * 0.4f +
                learning.questPercent * 0.2f +
                learning.explorationPercent * 0.2f +
                learning.friendshipPercent * 0.1f +
                learning.passportPercent * 0.1f
            ).coerceIn(0f, 1f)

        return PlayerProgress(
            level = level,
            totalXp = state.lastTotalXp,
            xpIntoLevel = XpCalculator.xpIntoLevel(state.lastTotalXp),
            xpToNextLevel = XpCalculator.xpRemainingToNextLevel(state.lastTotalXp),
            currentChapter = currentChapter,
            currentStoryStage = storyStageFor(currentChapter),
            unlockedRegionIds = unlockedRegions.map { it.id },
            unlockedNpcIds = unlockedNpcs,
            unlockedFeatures = unlockedFeatures,
            villageProgress = village?.completionPercentage ?: 0f,
            chapterProgress = chapterProgress,
            overallCompletion = overall,
            chapters = chapters,
        )
    }

    private fun storyStageFor(chapter: Int): String = when (chapter) {
        1 -> "village_intro"
        2 -> "forest_guide"
        3 -> "riverside_crossing"
        4 -> "night_market_food"
        5 -> "temple_visit"
        6 -> "rail_journey"
        7 -> "city_tour"
        8 -> "business_meeting"
        9 -> "shanghai_explore"
        10 -> "beijing_adventure"
        11 -> "great_wall_climb"
        12 -> "summit_trial"
        else -> "adventure_complete"
    }

    private suspend fun buildObjectives(
        regions: List<WorldRegion>,
        gameProgress: com.sworddao.phoenix.feature.gameplay.data.GameProgress,
        friendshipStates: List<com.sworddao.phoenix.feature.friendship.data.FriendshipState>,
        discoveryStats: com.sworddao.phoenix.feature.discovery.data.DiscoveryStatistics,
    ): List<CurrentObjective> {
        val unlockedRegions = regions.count { it.isUnlocked }
        val friendshipMax = friendshipStates.maxOfOrNull { it.friendshipLevel.level } ?: 0
        return listOf(
            CurrentObjective(
                id = "obj_dialogue",
                title = "完成对话",
                description = "与村民交流，提升对话能力",
                category = ObjectiveCategory.STORY,
                currentCount = gameProgress.totalDialoguesCompleted,
                targetCount = CONVERSATION_TARGET,
                icon = "💬",
            ),
            CurrentObjective(
                id = "obj_vocabulary",
                title = "发现词汇",
                description = "在冒险中发现新的词汇",
                category = ObjectiveCategory.LEARNING,
                currentCount = discoveryStats.totalDiscovered,
                targetCount = 20,
                icon = "🆕",
            ),
            CurrentObjective(
                id = "obj_quest",
                title = "完成任务",
                description = "完成主线任务推进故事",
                category = ObjectiveCategory.STORY,
                currentCount = gameProgress.totalQuestsCompleted,
                targetCount = 5,
                icon = "📜",
            ),
            CurrentObjective(
                id = "obj_speaking",
                title = "练习口语",
                description = "完成口语练习提升发音",
                category = ObjectiveCategory.LEARNING,
                currentCount = gameProgress.totalSpeakingPractices,
                targetCount = 10,
                icon = "🗣️",
            ),
            CurrentObjective(
                id = "obj_listening",
                title = "练习聆听",
                description = "完成聆听练习提升听力",
                category = ObjectiveCategory.LEARNING,
                currentCount = gameProgress.totalListeningPractices,
                targetCount = 10,
                icon = "👂",
            ),
            CurrentObjective(
                id = "obj_reading",
                title = "练习阅读",
                description = "完成阅读练习提升识字",
                category = ObjectiveCategory.LEARNING,
                currentCount = gameProgress.totalReadingPractices,
                targetCount = 10,
                icon = "📖",
            ),
            CurrentObjective(
                id = "obj_writing",
                title = "练习书写",
                description = "完成书写练习提升书写能力",
                category = ObjectiveCategory.LEARNING,
                currentCount = gameProgress.totalWritingPractices,
                targetCount = 10,
                icon = "✍️",
            ),
            CurrentObjective(
                id = "obj_friendship",
                title = "提升友谊",
                description = "与 NPC 建立深厚友谊",
                category = ObjectiveCategory.FRIENDSHIP,
                currentCount = friendshipMax,
                targetCount = MAX_FRIENDSHIP_LEVEL,
                icon = "🤝",
            ),
            CurrentObjective(
                id = "obj_exploration",
                title = "探索区域",
                description = "解锁新的区域继续冒险",
                category = ObjectiveCategory.EXPLORATION,
                currentCount = unlockedRegions,
                targetCount = 5,
                icon = "🧭",
            ),
        )
    }

    private fun buildSnapshot(
        gameProgress: com.sworddao.phoenix.feature.gameplay.data.GameProgress,
        regions: List<WorldRegion>,
        questStats: com.sworddao.phoenix.feature.quest.data.QuestStats,
        passport: com.sworddao.phoenix.feature.passport.data.Passport,
        vocabularyStats: com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics,
        friendshipStates: List<com.sworddao.phoenix.feature.friendship.data.FriendshipState>,
        speakingStats: com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics,
        listeningStats: com.sworddao.phoenix.feature.listening.data.ListeningStatistics,
        readingStats: com.sworddao.phoenix.feature.reading.data.ReadingStatistics,
    ): SourceSnapshot {
        val friendshipLevels = friendshipStates.sumOf { it.friendshipLevel.level - 1 }
        val achievements = gameProgress.milestonesCompleted.size +
            speakingStats.pronunciationBadges.count { it.isEarned } +
            listeningStats.listeningBadges.count { it.isEarned } +
            readingStats.readingBadges.count { it.isEarned } +
            vocabularyStats.masteredWords
        return SourceSnapshot(
            dialogues = gameProgress.totalDialoguesCompleted,
            wordsDiscovered = gameProgress.totalWordsDiscovered,
            questsCompleted = questStats.completedQuests,
            friendshipLevels = friendshipLevels,
            passportStamps = gameProgress.totalPassportStamps,
            speakingPractices = gameProgress.totalSpeakingPractices,
            listeningPractices = gameProgress.totalListeningPractices,
            readingPractices = gameProgress.totalReadingPractices,
            writingPractices = gameProgress.totalWritingPractices,
            regionsUnlocked = regions.count { it.isUnlocked },
            regionsCompleted = regions.count { it.isCompleted },
            achievements = achievements,
        )
    }

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private companion object {
        private const val MAX_RECENT_UNLOCKS = 20
        private const val CONVERSATION_TARGET = 10
        private const val MAX_FRIENDSHIP_LEVEL = 4
    }
}
