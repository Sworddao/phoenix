package com.sworddao.phoenix.feature.quest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: String,
    val status: String,
    val category: String,
    val objectivesJson: String,
    val rewardsJson: String,
    val prerequisitesJson: String,
    val npcId: String? = null,
    val locationId: String? = null,
    val dialogueId: String? = null,
    val repeatable: Boolean = false,
    val daily: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val completionDialogue: String? = null,
    val failureDialogue: String? = null,
    val order: Int = 0,
    val chapter: Int = 1,
)

@Entity(tableName = "quest_progress")
data class QuestProgressEntity(
    @PrimaryKey val questId: String,
    val status: String,
    val objectivesJson: String,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    val attempts: Int = 0,
)
