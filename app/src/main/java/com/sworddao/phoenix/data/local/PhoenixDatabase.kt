package com.sworddao.phoenix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sworddao.phoenix.feature.discovery.data.DiscoverySessionEntity
import com.sworddao.phoenix.feature.discovery.data.DiscoveryDao
import com.sworddao.phoenix.feature.discovery.data.VocabularyDiscoveryEntity
import com.sworddao.phoenix.feature.friendship.data.ConversationMemoryEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipDao
import com.sworddao.phoenix.feature.friendship.data.FriendshipEntity
import com.sworddao.phoenix.feature.friendship.data.FriendshipEventEntity
import com.sworddao.phoenix.feature.gameplay.data.GameProgressDao
import com.sworddao.phoenix.feature.gameplay.data.GameProgressEntity
import com.sworddao.phoenix.feature.gameplay.data.SessionSummaryEntity
import com.sworddao.phoenix.feature.listening.data.ListeningBadgesEntity
import com.sworddao.phoenix.feature.listening.data.ListeningDao
import com.sworddao.phoenix.feature.listening.data.ListeningExerciseEntity
import com.sworddao.phoenix.feature.listening.data.ListeningProgressDocEntity
import com.sworddao.phoenix.feature.listening.data.ListeningSessionsEntity
import com.sworddao.phoenix.feature.listening.data.ListeningStateEntity
import com.sworddao.phoenix.feature.listening.data.ListeningStatisticsEntity
import com.sworddao.phoenix.feature.passport.data.AchievementEntity
import com.sworddao.phoenix.feature.passport.data.CollectibleEntity
import com.sworddao.phoenix.feature.passport.data.PassportDao
import com.sworddao.phoenix.feature.passport.data.PassportEntity
import com.sworddao.phoenix.feature.passport.data.PassportEntryEntity
import com.sworddao.phoenix.feature.passport.data.PassportEventEntity
import com.sworddao.phoenix.feature.passport.data.PassportRegionEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionDao
import com.sworddao.phoenix.feature.progression.data.ProgressionDailyEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionFeaturesEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionLearningEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionObjectivesEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionPlayerEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionRecentEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionSnapshotEntity
import com.sworddao.phoenix.feature.progression.data.ProgressionStateEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingBadgesEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingDao
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingExerciseEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingProgressDocEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingSessionsEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingStateEntity
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingStatisticsEntity
import com.sworddao.phoenix.feature.quest.data.QuestDao
import com.sworddao.phoenix.feature.quest.data.QuestEntity
import com.sworddao.phoenix.feature.quest.data.QuestProgressEntity
import com.sworddao.phoenix.feature.reading.data.ReadingBadgesEntity
import com.sworddao.phoenix.feature.reading.data.ReadingDao
import com.sworddao.phoenix.feature.reading.data.ReadingExerciseEntity
import com.sworddao.phoenix.feature.reading.data.ReadingProgressDocEntity
import com.sworddao.phoenix.feature.reading.data.ReadingSessionsEntity
import com.sworddao.phoenix.feature.reading.data.ReadingStateEntity
import com.sworddao.phoenix.feature.reading.data.ReadingStatisticsEntity
import com.sworddao.phoenix.feature.review.data.ReviewDao
import com.sworddao.phoenix.feature.review.data.ReviewHistoryEntity
import com.sworddao.phoenix.feature.review.data.ReviewItemsEntity
import com.sworddao.phoenix.feature.review.data.ReviewMemoryEntity
import com.sworddao.phoenix.feature.review.data.ReviewPublishedEntity
import com.sworddao.phoenix.feature.review.data.ReviewSchedulesEntity
import com.sworddao.phoenix.feature.review.data.ReviewSessionsEntity
import com.sworddao.phoenix.feature.review.data.ReviewSnapshotEntity
import com.sworddao.phoenix.feature.review.data.ReviewStateEntity
import com.sworddao.phoenix.feature.review.data.ReviewStatsEntity
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDao
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyEntity
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyProgressEntity
import com.sworddao.phoenix.feature.world.data.WorldCollectibleEntity
import com.sworddao.phoenix.feature.world.data.WorldConnectionEntity
import com.sworddao.phoenix.feature.world.data.WorldDao
import com.sworddao.phoenix.feature.world.data.WorldLandmarkEntity
import com.sworddao.phoenix.feature.world.data.WorldLocationEntity
import com.sworddao.phoenix.feature.world.data.WorldRegionEntity
import com.sworddao.phoenix.feature.world.data.WorldRegionProgressEntity

@Database(
    entities = [
        PlaceholderEntity::class,
        AppMetadataEntity::class,
        FriendshipEntity::class,
        ConversationMemoryEntity::class,
        FriendshipEventEntity::class,
        VocabularyEntity::class,
        VocabularyProgressEntity::class,
        VocabularyDiscoveryEntity::class,
        DiscoverySessionEntity::class,
        QuestEntity::class,
        QuestProgressEntity::class,
        GameProgressEntity::class,
        SessionSummaryEntity::class,
        PassportEntity::class,
        PassportRegionEntity::class,
        CollectibleEntity::class,
        PassportEventEntity::class,
        AchievementEntity::class,
        PassportEntryEntity::class,
        WorldRegionEntity::class,
        WorldRegionProgressEntity::class,
        WorldConnectionEntity::class,
        WorldLocationEntity::class,
        WorldLandmarkEntity::class,
        WorldCollectibleEntity::class,
        ReadingExerciseEntity::class,
        ReadingProgressDocEntity::class,
        ReadingStatisticsEntity::class,
        ReadingBadgesEntity::class,
        ReadingSessionsEntity::class,
        ReadingStateEntity::class,
        ListeningExerciseEntity::class,
        ListeningProgressDocEntity::class,
        ListeningStatisticsEntity::class,
        ListeningBadgesEntity::class,
        ListeningSessionsEntity::class,
        ListeningStateEntity::class,
        SpeakingExerciseEntity::class,
        SpeakingProgressDocEntity::class,
        SpeakingStatisticsEntity::class,
        SpeakingBadgesEntity::class,
        SpeakingSessionsEntity::class,
        SpeakingStateEntity::class,
        ReviewItemsEntity::class,
        ReviewMemoryEntity::class,
        ReviewSchedulesEntity::class,
        ReviewSessionsEntity::class,
        ReviewHistoryEntity::class,
        ReviewStateEntity::class,
        ReviewSnapshotEntity::class,
        ReviewStatsEntity::class,
        ReviewPublishedEntity::class,
        ProgressionStateEntity::class,
        ProgressionSnapshotEntity::class,
        ProgressionDailyEntity::class,
        ProgressionRecentEntity::class,
        ProgressionFeaturesEntity::class,
        ProgressionPlayerEntity::class,
        ProgressionLearningEntity::class,
        ProgressionObjectivesEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PhoenixDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun friendshipDao(): FriendshipDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun discoveryDao(): DiscoveryDao
    abstract fun questDao(): QuestDao
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun passportDao(): PassportDao
    abstract fun worldDao(): WorldDao
    abstract fun readingDao(): ReadingDao
    abstract fun listeningDao(): ListeningDao
    abstract fun speakingDao(): SpeakingDao
    abstract fun reviewDao(): ReviewDao
    abstract fun progressionDao(): ProgressionDao
}

/**
 * Migration from version 2 (placeholder + friendship tables) to version 3
 * (all feature storage tables). Existing v2 tables are left untouched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_metadata` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `conversation_memory` (`id` TEXT NOT NULL, `npcId` TEXT NOT NULL, `dialogueId` TEXT NOT NULL, `dialogueTitle` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `topicsDiscussed` TEXT NOT NULL, `xpGained` INTEGER NOT NULL, `choicesSummary` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `friendship_event` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `npcId` TEXT NOT NULL, `description` TEXT NOT NULL, `xpChange` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `metadata` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vocabulary_word` (`id` TEXT NOT NULL, `mandarin` TEXT NOT NULL, `pinyin` TEXT NOT NULL, `english` TEXT NOT NULL, `hanzi` TEXT, `audioPath` TEXT, `category` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `exampleSentence` TEXT NOT NULL, `exampleTranslation` TEXT NOT NULL, `examplePinyin` TEXT NOT NULL, `relatedNpcId` TEXT, `relatedQuestId` TEXT, `relatedRegionId` TEXT, `discoveredAt` INTEGER, `mastery` TEXT NOT NULL, `timesReviewed` INTEGER NOT NULL, `timesSpoken` INTEGER NOT NULL, `timesHeard` INTEGER NOT NULL, `timesRead` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `notes` TEXT, `tagsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vocabulary_progress` (`wordId` TEXT NOT NULL, `mastery` TEXT NOT NULL, `timesReviewed` INTEGER NOT NULL, `timesSpoken` INTEGER NOT NULL, `timesHeard` INTEGER NOT NULL, `timesRead` INTEGER NOT NULL, `lastReviewedAt` INTEGER, `discoveredAt` INTEGER, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`wordId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vocabulary_discovery` (`id` TEXT NOT NULL, `wordId` TEXT NOT NULL, `wordJson` TEXT, `source` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `discoveredAt` INTEGER NOT NULL, `isFirstDiscovery` INTEGER NOT NULL, `bonusXp` INTEGER NOT NULL, `bonusFriendshipXp` INTEGER NOT NULL, `relatedNpcId` TEXT, `relatedQuestId` TEXT, `relatedRegionId` TEXT, `metadataJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `discovery_session` (`id` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `discoveriesJson` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `totalXpEarned` INTEGER NOT NULL, `totalFriendshipXpEarned` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quest` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `type` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `status` TEXT NOT NULL, `category` TEXT NOT NULL, `objectivesJson` TEXT NOT NULL, `rewardsJson` TEXT NOT NULL, `prerequisitesJson` TEXT NOT NULL, `npcId` TEXT, `locationId` TEXT, `dialogueId` TEXT, `repeatable` INTEGER NOT NULL, `daily` INTEGER NOT NULL, `timeLimitMinutes` INTEGER, `completionDialogue` TEXT, `failureDialogue` TEXT, `order` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `quest_progress` (`questId` TEXT NOT NULL, `status` TEXT NOT NULL, `objectivesJson` TEXT NOT NULL, `startedAt` INTEGER, `completedAt` INTEGER, `lastPlayedAt` INTEGER, `attempts` INTEGER NOT NULL, PRIMARY KEY(`questId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `game_progress` (`id` TEXT NOT NULL, `gameProgressJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_summary` (`id` TEXT NOT NULL, `sessionSummaryJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport` (`id` TEXT NOT NULL, `playerName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, `totalStamps` INTEGER NOT NULL, `totalCollectibles` INTEGER NOT NULL, `totalDiscoveries` INTEGER NOT NULL, `currentChapter` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport_region` (`regionId` TEXT NOT NULL, `regionName` TEXT NOT NULL, `regionNameCn` TEXT NOT NULL, `isDiscovered` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `stampEarned` INTEGER NOT NULL, `stampRarity` TEXT NOT NULL, `discoveredAt` INTEGER, `completedAt` INTEGER, `completionPercentage` REAL NOT NULL, `vocabularyLearned` INTEGER NOT NULL, `friendshipsMade` INTEGER NOT NULL, `questsCompleted` INTEGER NOT NULL, `collectiblesFound` INTEGER NOT NULL, `collectiblesTotal` INTEGER NOT NULL, `totalPlayTimeMinutes` INTEGER NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`regionId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport_collectible` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameCn` TEXT NOT NULL, `category` TEXT NOT NULL, `rarity` TEXT NOT NULL, `source` TEXT NOT NULL, `description` TEXT NOT NULL, `culturalNote` TEXT, `regionId` TEXT NOT NULL, `isCollected` INTEGER NOT NULL, `collectedAt` INTEGER, `isHidden` INTEGER NOT NULL, `isDisplayed` INTEGER NOT NULL, `displayLocation` TEXT, `tradeable` INTEGER NOT NULL, `xpValue` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport_event` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `regionId` TEXT, `timestamp` INTEGER NOT NULL, `metadata` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport_achievement` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameCn` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL, `isUnlocked` INTEGER NOT NULL, `unlockedAt` INTEGER, `progress` REAL NOT NULL, `requiredCount` INTEGER NOT NULL, `currentCount` INTEGER NOT NULL, `category` TEXT NOT NULL, `xpReward` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `passport_entry` (`id` TEXT NOT NULL, `regionId` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `metadata` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_region` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameCn` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `order` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `unlockRequirementsJson` TEXT NOT NULL, `connectionsJson` TEXT NOT NULL, `travelMethodsJson` TEXT NOT NULL, `npcIdsJson` TEXT NOT NULL, `questIdsJson` TEXT NOT NULL, `completionPercentage` REAL NOT NULL, `musicTrack` TEXT, `ambienceTrack` TEXT, `mapPositionX` REAL NOT NULL, `mapPositionY` REAL NOT NULL, `color` INTEGER NOT NULL, `icon` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_region_progress` (`regionId` TEXT NOT NULL, `status` TEXT NOT NULL, `completionPercentage` REAL NOT NULL, `discoveredLocationsJson` TEXT NOT NULL, `completedQuestsJson` TEXT NOT NULL, `collectedItemsJson` TEXT NOT NULL, `visitedNpcsJson` TEXT NOT NULL, `unlockedFastTravel` INTEGER NOT NULL, `firstVisitedAt` INTEGER, `lastVisitedAt` INTEGER, `totalPlayTimeMinutes` INTEGER NOT NULL, PRIMARY KEY(`regionId`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_connection` (`id` TEXT NOT NULL, `fromRegionId` TEXT NOT NULL, `toRegionId` TEXT NOT NULL, `travelMethod` TEXT NOT NULL, `travelTimeMinutes` INTEGER NOT NULL, `isUnlocked` INTEGER NOT NULL, `description` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_location` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameCn` TEXT NOT NULL, `description` TEXT NOT NULL, `regionId` TEXT NOT NULL, `type` TEXT NOT NULL, `positionX` REAL NOT NULL, `positionY` REAL NOT NULL, `npcIdsJson` TEXT NOT NULL, `questIdsJson` TEXT NOT NULL, `isDiscovered` INTEGER NOT NULL, `isAccessible` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_landmark` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `nameCn` TEXT NOT NULL, `type` TEXT NOT NULL, `description` TEXT NOT NULL, `regionId` TEXT NOT NULL, `positionX` REAL NOT NULL, `positionY` REAL NOT NULL, `isDiscovered` INTEGER NOT NULL, `isInteractable` INTEGER NOT NULL, `npcIdsJson` TEXT NOT NULL, `questIdsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `world_collectible` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `regionId` TEXT NOT NULL, `locationId` TEXT, `positionX` REAL NOT NULL, `positionY` REAL NOT NULL, `isCollected` INTEGER NOT NULL, `isHidden` INTEGER NOT NULL, `description` TEXT, `culturalNote` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_exercise` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `wordId` TEXT, `npcId` TEXT, `questId` TEXT, `isUnlocked` INTEGER NOT NULL, `order` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, `exerciseJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_progress_doc` (`id` TEXT NOT NULL, `progressJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_statistics` (`id` TEXT NOT NULL, `statisticsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_badges` (`id` TEXT NOT NULL, `badgesJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_sessions` (`id` TEXT NOT NULL, `activeSessionJson` TEXT, `completedSessionsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading_state` (`id` TEXT NOT NULL, `currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastReadingDate` INTEGER, `correctCount` INTEGER NOT NULL, `npcExerciseCount` INTEGER NOT NULL, `readWordsJson` TEXT NOT NULL, `recordedBadgeIdsJson` TEXT NOT NULL, `firstReadingRecorded` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_exercise` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `wordId` TEXT, `npcId` TEXT, `questId` TEXT, `isUnlocked` INTEGER NOT NULL, `order` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, `exerciseJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_progress_doc` (`id` TEXT NOT NULL, `progressJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_statistics` (`id` TEXT NOT NULL, `statisticsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_badges` (`id` TEXT NOT NULL, `badgesJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_sessions` (`id` TEXT NOT NULL, `activeSessionJson` TEXT, `completedSessionsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `listening_state` (`id` TEXT NOT NULL, `currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastListeningDate` INTEGER, `correctCount` INTEGER NOT NULL, `npcExerciseCount` INTEGER NOT NULL, `practicedWordsJson` TEXT NOT NULL, `recordedBadgeIdsJson` TEXT NOT NULL, `replayCountsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_exercise` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `wordId` TEXT, `phraseId` TEXT, `npcId` TEXT, `questId` TEXT, `isUnlocked` INTEGER NOT NULL, `order` INTEGER NOT NULL, `xpReward` INTEGER NOT NULL, `exerciseJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_progress_doc` (`id` TEXT NOT NULL, `progressJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_statistics` (`id` TEXT NOT NULL, `statisticsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_badges` (`id` TEXT NOT NULL, `badgesJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_sessions` (`id` TEXT NOT NULL, `activeSessionJson` TEXT, `completedSessionsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `speaking_state` (`id` TEXT NOT NULL, `currentStreak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastPracticeDate` INTEGER, `practicedWordsJson` TEXT NOT NULL, `highConfidenceWordsJson` TEXT NOT NULL, `perfectToneExercisesJson` TEXT NOT NULL, `dialoguePhraseExercisesJson` TEXT NOT NULL, `attemptedExerciseIdsJson` TEXT NOT NULL, `practiceCountByTypeJson` TEXT NOT NULL, `practiceCountByDifficultyJson` TEXT NOT NULL, `recordedBadgeIdsJson` TEXT NOT NULL, `lastConfidenceByKeyJson` TEXT NOT NULL, `confidenceSum` REAL NOT NULL, `toneSum` REAL NOT NULL, `fluencySum` REAL NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_items` (`id` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_memory` (`id` TEXT NOT NULL, `memoryJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_schedules` (`id` TEXT NOT NULL, `schedulesJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_sessions` (`id` TEXT NOT NULL, `sessionsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_history` (`id` TEXT NOT NULL, `historyJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_state` (`id` TEXT NOT NULL, `itemIdCounter` INTEGER NOT NULL, `historyIdCounter` INTEGER NOT NULL, `sessionIdCounter` INTEGER NOT NULL, `todayDate` TEXT NOT NULL, `reviewsToday` INTEGER NOT NULL, `currentStreakDays` INTEGER NOT NULL, `longestStreakDays` INTEGER NOT NULL, `xpEarnedTotal` INTEGER NOT NULL, `reviewedWordIdsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_snapshot` (`id` TEXT NOT NULL, `snapshotJson` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_stats` (`id` TEXT NOT NULL, `statsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `review_published` (`id` TEXT NOT NULL, `todayJson` TEXT NOT NULL, `upcomingJson` TEXT NOT NULL, `recommendationsJson` TEXT NOT NULL, `dailyJson` TEXT NOT NULL, `memoryStrengthsJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_state` (`id` TEXT NOT NULL, `lastTotalXp` INTEGER NOT NULL, `lastLevel` INTEGER NOT NULL, `dailyDate` TEXT NOT NULL, `goalStreak` INTEGER NOT NULL, `lastUnlockedChaptersJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_snapshot` (`id` TEXT NOT NULL, `snapshotJson` TEXT, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_daily` (`id` TEXT NOT NULL, `dailyJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_recent` (`id` TEXT NOT NULL, `recentJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_features` (`id` TEXT NOT NULL, `timelineJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_player` (`id` TEXT NOT NULL, `playerJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_learning` (`id` TEXT NOT NULL, `learningJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `progression_objectives` (`id` TEXT NOT NULL, `objectivesJson` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}
