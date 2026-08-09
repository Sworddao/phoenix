package com.sworddao.phoenix.feature.writing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "writing_exercise")
data class WritingExerciseEntity(
    @PrimaryKey val id: String,
    val type: String,
    val difficulty: String,
    val wordId: String? = null,
    val isUnlocked: Boolean = true,
    val order: Int = 0,
    val xpReward: Int = 10,
    val characterJson: String,
    val exerciseJson: String,
)

@Entity(tableName = "writing_progress_doc")
data class WritingProgressDocEntity(
    @PrimaryKey val id: String,
    val progressJson: String,
)

@Entity(tableName = "writing_statistics")
data class WritingStatisticsEntity(
    @PrimaryKey val id: String,
    val statisticsJson: String,
)

@Entity(tableName = "writing_badges")
data class WritingBadgesEntity(
    @PrimaryKey val id: String,
    val badgesJson: String,
)

@Entity(tableName = "writing_sessions")
data class WritingSessionsEntity(
    @PrimaryKey val id: String,
    val activeSessionJson: String?,
    val completedSessionsJson: String,
)

@Entity(tableName = "writing_state")
data class WritingStateEntity(
    @PrimaryKey val id: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastWritingDate: Long?,
    val correctCount: Int,
    val writtenCharactersJson: String,
    val recordedBadgeIdsJson: String,
    val firstWritingRecorded: Boolean,
)
