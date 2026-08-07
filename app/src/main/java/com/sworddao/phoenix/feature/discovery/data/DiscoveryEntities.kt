package com.sworddao.phoenix.feature.discovery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_discovery")
data class VocabularyDiscoveryEntity(
    @PrimaryKey val id: String,
    val wordId: String,
    val wordJson: String? = null,
    val source: String,
    val sourceId: String,
    val sourceName: String,
    val discoveredAt: Long,
    val isFirstDiscovery: Boolean,
    val bonusXp: Int = 0,
    val bonusFriendshipXp: Int = 0,
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedRegionId: String? = null,
    val metadataJson: String = "{}"
)

@Entity(tableName = "discovery_session")
data class DiscoverySessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val discoveriesJson: String,
    val source: String,
    val sourceId: String,
    val totalXpEarned: Int = 0,
    val totalFriendshipXpEarned: Int = 0,
    val isActive: Boolean = true
)
