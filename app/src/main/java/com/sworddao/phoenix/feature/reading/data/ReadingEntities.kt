package com.sworddao.phoenix.feature.reading.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_exercise")
data class ReadingExerciseEntity(
    @PrimaryKey val id: String,
    val type: String,
    val difficulty: String,
    val wordId: String? = null,
    val npcId: String? = null,
    val questId: String? = null,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
    val xpReward: Int = 10,
    val exerciseJson: String,
)

@Entity(tableName = "reading_progress_doc")
data class ReadingProgressDocEntity(
    @PrimaryKey val id: String,
    val progressJson: String,
)

@Entity(tableName = "reading_statistics")
data class ReadingStatisticsEntity(
    @PrimaryKey val id: String,
    val statisticsJson: String,
)

@Entity(tableName = "reading_badges")
data class ReadingBadgesEntity(
    @PrimaryKey val id: String,
    val badgesJson: String,
)

@Entity(tableName = "reading_sessions")
data class ReadingSessionsEntity(
    @PrimaryKey val id: String,
    val activeSessionJson: String?,
    val completedSessionsJson: String,
)

@Entity(tableName = "reading_state")
data class ReadingStateEntity(
    @PrimaryKey val id: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadingDate: Long?,
    val correctCount: Int,
    val npcExerciseCount: Int,
    val readWordsJson: String,
    val recordedBadgeIdsJson: String,
    val firstReadingRecorded: Boolean,
)
