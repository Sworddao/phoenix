package com.sworddao.phoenix.feature.discovery.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord

fun VocabularyDiscovery.toEntity(): VocabularyDiscoveryEntity = VocabularyDiscoveryEntity(
    id = id,
    wordId = wordId,
    wordJson = word?.let { RoomJson.toJson(it) },
    source = source.name,
    sourceId = sourceId,
    sourceName = sourceName,
    discoveredAt = discoveredAt,
    isFirstDiscovery = isFirstDiscovery,
    bonusXp = bonusXp,
    bonusFriendshipXp = bonusFriendshipXp,
    relatedNpcId = relatedNpcId,
    relatedQuestId = relatedQuestId,
    relatedRegionId = relatedRegionId,
    metadataJson = RoomJson.toJson(metadata),
)

fun VocabularyDiscoveryEntity.toDomain(): VocabularyDiscovery = VocabularyDiscovery(
    id = id,
    wordId = wordId,
    word = RoomJson.fromJsonOrNull<VocabularyWord>(wordJson),
    source = DiscoverySourceType.valueOf(source),
    sourceId = sourceId,
    sourceName = sourceName,
    discoveredAt = discoveredAt,
    isFirstDiscovery = isFirstDiscovery,
    bonusXp = bonusXp,
    bonusFriendshipXp = bonusFriendshipXp,
    relatedNpcId = relatedNpcId,
    relatedQuestId = relatedQuestId,
    relatedRegionId = relatedRegionId,
    metadata = RoomJson.fromJsonMap(metadataJson),
)

fun DiscoverySession.toEntity(): DiscoverySessionEntity = DiscoverySessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    discoveriesJson = RoomJson.toJsonList(discoveries),
    source = source.name,
    sourceId = sourceId,
    totalXpEarned = totalXpEarned,
    totalFriendshipXpEarned = totalFriendshipXpEarned,
    isActive = isActive,
)

fun DiscoverySessionEntity.toDomain(): DiscoverySession = DiscoverySession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    discoveries = RoomJson.fromJsonList(discoveriesJson),
    source = DiscoverySourceType.valueOf(source),
    sourceId = sourceId,
    totalXpEarned = totalXpEarned,
    totalFriendshipXpEarned = totalFriendshipXpEarned,
    isActive = isActive,
)
