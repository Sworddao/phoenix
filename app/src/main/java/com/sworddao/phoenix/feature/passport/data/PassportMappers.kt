package com.sworddao.phoenix.feature.passport.data

import com.sworddao.phoenix.data.local.RoomJson

fun Passport.toEntity(): PassportEntity = PassportEntity(
    id = id,
    playerName = playerName,
    createdAt = createdAt,
    lastUpdated = lastUpdated,
    totalStamps = totalStamps,
    totalCollectibles = totalCollectibles,
    totalDiscoveries = totalDiscoveries,
    currentChapter = currentChapter,
)

fun PassportEntity.toDomain(
    regions: Map<String, PassportRegion> = emptyMap(),
    collectibles: Map<String, Collectible> = emptyMap(),
    timeline: List<DiscoveryEvent> = emptyList(),
): Passport = Passport(
    id = id,
    playerName = playerName,
    createdAt = createdAt,
    lastUpdated = lastUpdated,
    totalStamps = totalStamps,
    totalCollectibles = totalCollectibles,
    totalDiscoveries = totalDiscoveries,
    currentChapter = currentChapter,
    regions = regions,
    collectibles = collectibles,
    timeline = timeline,
)

fun PassportRegion.toEntity(): PassportRegionEntity = PassportRegionEntity(
    regionId = regionId,
    regionName = regionName,
    regionNameCn = regionNameCn,
    isDiscovered = isDiscovered,
    isCompleted = isCompleted,
    stampEarned = stampEarned,
    stampRarity = stampRarity.name,
    discoveredAt = discoveredAt,
    completedAt = completedAt,
    completionPercentage = completionPercentage,
    vocabularyLearned = vocabularyLearned,
    friendshipsMade = friendshipsMade,
    questsCompleted = questsCompleted,
    collectiblesFound = collectiblesFound,
    collectiblesTotal = collectiblesTotal,
    totalPlayTimeMinutes = totalPlayTimeMinutes,
    notes = notes,
)

fun PassportRegionEntity.toDomain(): PassportRegion = PassportRegion(
    regionId = regionId,
    regionName = regionName,
    regionNameCn = regionNameCn,
    isDiscovered = isDiscovered,
    isCompleted = isCompleted,
    stampEarned = stampEarned,
    stampRarity = StampRarity.valueOf(stampRarity),
    discoveredAt = discoveredAt,
    completedAt = completedAt,
    completionPercentage = completionPercentage,
    vocabularyLearned = vocabularyLearned,
    friendshipsMade = friendshipsMade,
    questsCompleted = questsCompleted,
    collectiblesFound = collectiblesFound,
    collectiblesTotal = collectiblesTotal,
    totalPlayTimeMinutes = totalPlayTimeMinutes,
    notes = notes,
)

fun Collectible.toEntity(): CollectibleEntity = CollectibleEntity(
    id = id,
    name = name,
    nameCn = nameCn,
    category = category.name,
    rarity = rarity.name,
    source = source.name,
    description = description,
    culturalNote = culturalNote,
    regionId = regionId,
    isCollected = isCollected,
    collectedAt = collectedAt,
    isHidden = isHidden,
    isDisplayed = isDisplayed,
    displayLocation = displayLocation,
    tradeable = tradeable,
    xpValue = xpValue,
)

fun CollectibleEntity.toDomain(): Collectible = Collectible(
    id = id,
    name = name,
    nameCn = nameCn,
    category = CollectibleCategory.valueOf(category),
    rarity = CollectibleRarity.valueOf(rarity),
    source = CollectibleSource.valueOf(source),
    description = description,
    culturalNote = culturalNote,
    regionId = regionId,
    isCollected = isCollected,
    collectedAt = collectedAt,
    isHidden = isHidden,
    isDisplayed = isDisplayed,
    displayLocation = displayLocation,
    tradeable = tradeable,
    xpValue = xpValue,
)

fun DiscoveryEvent.toEntity(): PassportEventEntity = PassportEventEntity(
    id = id,
    type = type.name,
    title = title,
    description = description,
    regionId = regionId,
    timestamp = timestamp,
    metadata = RoomJson.toJson(metadata),
)

fun PassportEventEntity.toDomain(): DiscoveryEvent = DiscoveryEvent(
    id = id,
    type = EntryType.valueOf(type),
    title = title,
    description = description,
    regionId = regionId,
    timestamp = timestamp,
    metadata = RoomJson.fromJsonMap(metadata),
)

fun AchievementProgress.toEntity(): AchievementEntity = AchievementEntity(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    icon = icon,
    isUnlocked = isUnlocked,
    unlockedAt = unlockedAt,
    progress = progress,
    requiredCount = requiredCount,
    currentCount = currentCount,
    category = category,
    xpReward = xpReward,
)

fun AchievementEntity.toDomain(): AchievementProgress = AchievementProgress(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    icon = icon,
    isUnlocked = isUnlocked,
    unlockedAt = unlockedAt,
    progress = progress,
    requiredCount = requiredCount,
    currentCount = currentCount,
    category = category,
    xpReward = xpReward,
)

fun PassportEntry.toEntity(): PassportEntryEntity = PassportEntryEntity(
    id = id,
    regionId = regionId,
    type = type.name,
    title = title,
    description = description,
    timestamp = timestamp,
    metadata = RoomJson.toJson(metadata),
)

fun PassportEntryEntity.toDomain(): PassportEntry = PassportEntry(
    id = id,
    regionId = regionId,
    type = EntryType.valueOf(type),
    title = title,
    description = description,
    timestamp = timestamp,
    metadata = RoomJson.fromJsonMap(metadata),
)
