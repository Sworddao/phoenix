package com.sworddao.phoenix.feature.progression.data

import com.sworddao.phoenix.feature.discovery.data.MockDiscoveryRepository
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameProgress
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.ListeningStatistics
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.passport.data.Passport
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatistics
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.QuestStats
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.reading.data.ReadingStatistics
import com.sworddao.phoenix.feature.progression.domain.ProgressionRepository
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics
import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import com.sworddao.phoenix.feature.world.data.RegionStatus
import com.sworddao.phoenix.feature.world.data.WorldRegion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Singleton
class MockProgressionRepository @Inject constructor(
    private val gameProgressRepository: MockGameProgressRepository,
    private val worldRepository: MockWorldRepository,
    private val questRepository: MockQuestRepository,
    private val passportRepository: MockPassportRepository,
    private val vocabularyRepository: MockVocabularyRepository,
    private val friendshipRepository: MockFriendshipRepository,
    private val discoveryRepository: MockDiscoveryRepository,
    private val pronunciationRepository: MockPronunciationRepository,
    private val listeningRepository: MockListeningRepository,
    private val readingRepository: MockReadingRepository,
) : ProgressionRepository {

    private val _playerProgress = MutableStateFlow(PlayerProgress())
    private val _learningProgress = MutableStateFlow(LearningProgress())
    private val _dailyProgress = MutableStateFlow(DailyProgress())
    private val _recentUnlocks = MutableStateFlow<List<RecentUnlock>>(emptyList())
    private val _currentObjectives = MutableStateFlow<List<CurrentObjective>>(emptyList())
    private val _featureUnlockTimeline = MutableStateFlow<List<FeatureUnlockEntry>>(
        FeatureUnlock.entries.map { FeatureUnlockEntry(feature = it) }
    )

    private var lastSnapshot: SourceSnapshot? = null
    private var lastTotalXp: Int = 0
    private var lastLevel: Int = 1
    private var lastUnlockedChapters: MutableSet<String> = mutableSetOf()
    private var dailyDate: String = today()
    private var goalStreak: Int = 0

    override fun getPlayerProgress(): Flow<PlayerProgress> = _playerProgress.asStateFlow()

    override fun getLearningProgress(): Flow<LearningProgress> = _learningProgress.asStateFlow()

    override fun getDailyProgress(): Flow<DailyProgress> = _dailyProgress.asStateFlow()

    override fun getRecentUnlocks(): Flow<List<RecentUnlock>> = _recentUnlocks.asStateFlow()

    override fun getCurrentObjectives(): Flow<List<CurrentObjective>> = _currentObjectives.asStateFlow()

    override fun getFeatureUnlockTimeline(): Flow<List<FeatureUnlockEntry>> = _featureUnlockTimeline.asStateFlow()

    override suspend fun refresh(): ProgressionResult {
        val gameProgress = gameProgressRepository.getGameProgress().first()
        val regions = worldRepository.getAllRegions().first()
        val questStats = questRepository.getQuestStats().first()
        val passport = passportRepository.getPassport().first()
        val vocabularyStats = vocabularyRepository.getStatistics().first()
        val friendshipStates = friendshipRepository.getAllFriendshipStates().first()
        val speakingStats = pronunciationRepository.getSpeakingStatistics().first()
        val listeningStats = listeningRepository.getListeningStatistics().first()
        val readingStats = readingRepository.getReadingStatistics().first()
        discoveryRepository.getDiscoveryStatistics().first()

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
            gameProgress = gameProgress,
        )
        _learningProgress.value = learning

        val player = buildPlayerProgress(
            learning = learning,
            regions = regions,
            gameProgress = gameProgress,
        )
        _playerProgress.value = player
        _currentObjectives.value = buildObjectives(
            regions = regions,
            gameProgress = gameProgress,
            vocabularyStats = vocabularyStats,
            friendshipStates = friendshipStates,
        )

        lastSnapshot = snapshot

        return ProgressionResult.Refreshed(
            playerProgress = player,
            learningProgress = learning,
        )
    }

    override suspend fun awardXp(source: XpSource, count: Int): ProgressionResult {
        val amount = source.baseXp * count
        recordDailyActivity(source, count)
        addRecentUnlock(
            title = source.displayName,
            description = "获得 $amount XP",
            icon = source.icon,
        )

        val newTotalXp = lastTotalXp + amount
        val newLevel = XpCalculator.levelForTotalXp(newTotalXp)
        val levelUp = newLevel > lastLevel
        val unlockedFeatures = if (levelUp) {
            FeatureUnlock.entries
                .filter { it.requiredLevel <= newLevel && it.requiredLevel > lastLevel }
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

        lastTotalXp = newTotalXp
        lastLevel = newLevel

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
            gameProgress = gameProgressRepository.getGameProgress().first(),
        )
        _learningProgress.value = learning
        _playerProgress.value = buildPlayerProgress(
            learning = learning,
            regions = regions,
            gameProgress = gameProgressRepository.getGameProgress().first(),
        )
        _currentObjectives.value = buildObjectives(
            regions = regions,
            gameProgress = gameProgressRepository.getGameProgress().first(),
            vocabularyStats = vocabularyRepository.getStatistics().first(),
            friendshipStates = friendshipRepository.getAllFriendshipStates().first(),
        )

        return if (levelUp) {
            ProgressionResult.LevelUp(newLevel, unlockedFeatures)
        } else {
            ProgressionResult.XpAwarded(source, amount, newLevel)
        }
    }

    override suspend fun resetProgression() {
        _playerProgress.value = PlayerProgress()
        _learningProgress.value = LearningProgress()
        _dailyProgress.value = DailyProgress()
        _recentUnlocks.value = emptyList()
        _currentObjectives.value = emptyList()
        _featureUnlockTimeline.value = FeatureUnlock.entries.map { FeatureUnlockEntry(feature = it) }
        lastSnapshot = null
        lastTotalXp = 0
        lastLevel = 1
        lastUnlockedChapters = mutableSetOf()
        dailyDate = today()
        goalStreak = 0
    }

    // ---------------------------------------------------------------------
    // Snapshot delta detection
    // ---------------------------------------------------------------------

    private data class SourceSnapshot(
        val dialogues: Int = 0,
        val wordsDiscovered: Int = 0,
        val questsCompleted: Int = 0,
        val friendshipLevels: Int = 0,
        val passportStamps: Int = 0,
        val speakingPractices: Int = 0,
        val listeningPractices: Int = 0,
        val readingPractices: Int = 0,
        val writingPractices: Int = 0,
        val regionsUnlocked: Int = 0,
        val regionsCompleted: Int = 0,
        val achievements: Int = 0,
    )

    private fun buildSnapshot(
        gameProgress: GameProgress,
        regions: List<WorldRegion>,
        questStats: QuestStats,
        passport: Passport,
        vocabularyStats: VocabularyStatistics,
        friendshipStates: List<FriendshipState>,
        speakingStats: SpeakingStatistics,
        listeningStats: ListeningStatistics,
        readingStats: ReadingStatistics,
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

    private suspend fun applySnapshotDeltas(snapshot: SourceSnapshot) {
        val previous = lastSnapshot
        if (previous == null) {
            lastTotalXp = 0
            lastLevel = 1
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

        val newTotalXp = lastTotalXp + xp
        val newLevel = XpCalculator.levelForTotalXp(newTotalXp)
        if (newLevel > lastLevel) {
            val newlyUnlocked = FeatureUnlock.entries
                .filter { it.requiredLevel <= newLevel && it.requiredLevel > lastLevel }
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

        lastTotalXp = newTotalXp
        lastLevel = newLevel

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

    private fun recordDailyActivity(source: XpSource, count: Int) {
        val now = today()
        if (now != dailyDate) {
            if (_dailyProgress.value.isGoalReached) {
                goalStreak += 1
            }
            dailyDate = now
            _dailyProgress.value = DailyProgress(
                date = now,
                dailyGoal = _dailyProgress.value.dailyGoal,
                goalStreak = goalStreak,
            )
        }

        val current = _dailyProgress.value
        val byType = current.activitiesByType.toMutableMap()
        byType[source] = (byType[source] ?: 0) + count
        val activities = current.activitiesCompletedToday + count
        val goalReachedBefore = current.isGoalReached

        _dailyProgress.value = current.copy(
            date = now,
            xpEarnedToday = current.xpEarnedToday + source.baseXp * count,
            activitiesCompletedToday = activities,
            activitiesByType = byType,
        )

        if (!goalReachedBefore && _dailyProgress.value.isGoalReached) {
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

    private fun markFeatureUnlocked(feature: FeatureUnlock) {
        _featureUnlockTimeline.value = _featureUnlockTimeline.value.map { entry ->
            if (entry.feature == feature) {
                entry.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
            } else {
                entry
            }
        }
    }

    private suspend fun checkChapterUnlocks(regions: List<WorldRegion>) {
        val sorted = regions.sortedBy { it.order }
        sorted.forEachIndexed { index, region ->
            val chapterId = "chapter_${index + 1}"
            if (chapterId in lastUnlockedChapters) return@forEachIndexed

            val previous = if (index > 0) sorted[index - 1] else null
            val requirementMet = when {
                index == 0 -> true
                previous == null -> false
                else -> {
                    val previousCompleted = previous.status == RegionStatus.COMPLETED
                    val levelOk = XpCalculator.levelForTotalXp(lastTotalXp) >= (1 + index / 2)
                    levelOk || previousCompleted
                }
            }

            if (requirementMet && region.isUnlocked) {
                lastUnlockedChapters += chapterId
                addRecentUnlock(
                    title = "新章节解锁：${region.nameCn}",
                    description = region.name,
                    icon = region.icon,
                )
            }
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

    private fun addRecentUnlock(title: String, description: String, icon: String) {
        _recentUnlocks.value = (listOf(
            RecentUnlock(title = title, description = description, icon = icon)
        ) + _recentUnlocks.value).take(MAX_RECENT_UNLOCKS)
    }

    // ---------------------------------------------------------------------
    // Progress computation
    // ---------------------------------------------------------------------

    private fun buildLearningProgress(
        regions: List<WorldRegion>,
        questStats: QuestStats,
        passport: Passport,
        vocabularyStats: VocabularyStatistics,
        friendshipStates: List<FriendshipState>,
        speakingStats: SpeakingStatistics,
        listeningStats: ListeningStatistics,
        readingStats: ReadingStatistics,
        gameProgress: GameProgress,
    ): LearningProgress {
        val discovered = vocabularyStats.discoveredWords.coerceAtLeast(1)
        val friendshipMaxLevel = friendshipStates.maxOfOrNull { it.friendshipLevel.level } ?: 0

        return LearningProgress(
            speakingPercent = (speakingStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            listeningPercent = (listeningStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            readingPercent = (readingStats.wordsMastered.toFloat() / discovered).coerceIn(0f, 1f),
            vocabularyPercent = vocabularyStats.completionPercentage.coerceIn(0f, 1f),
            conversationPercent = (gameProgress.totalDialoguesCompleted.toFloat() / CONVERSATION_TARGET).coerceIn(0f, 1f),
            questPercent = questStats.completionRate.coerceIn(0f, 1f),
            friendshipPercent = (friendshipMaxLevel.toFloat() / MAX_FRIENDSHIP_LEVEL).coerceIn(0f, 1f),
            explorationPercent = (regions.count { it.isUnlocked }.toFloat() / regions.size.coerceAtLeast(1)).coerceIn(0f, 1f),
            passportPercent = passport.completionPercentage.coerceIn(0f, 1f),
        )
    }

    private fun buildPlayerProgress(
        learning: LearningProgress,
        regions: List<WorldRegion>,
        gameProgress: GameProgress,
    ): PlayerProgress {
        val level = XpCalculator.levelForTotalXp(lastTotalXp)
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
            totalXp = lastTotalXp,
            xpIntoLevel = XpCalculator.xpIntoLevel(lastTotalXp),
            xpToNextLevel = XpCalculator.xpRemainingToNextLevel(lastTotalXp),
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

    private fun buildObjectives(
        regions: List<WorldRegion>,
        gameProgress: GameProgress,
        vocabularyStats: VocabularyStatistics,
        friendshipStates: List<FriendshipState>,
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
                currentCount = vocabularyStats.discoveredWords,
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

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    companion object {
        private const val MAX_RECENT_UNLOCKS = 20
        private const val CONVERSATION_TARGET = 10
        private const val MAX_FRIENDSHIP_LEVEL = 4
    }
}
