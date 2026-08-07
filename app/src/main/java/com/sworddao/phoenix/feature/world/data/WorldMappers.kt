package com.sworddao.phoenix.feature.world.data

import com.sworddao.phoenix.data.local.RoomJson

fun WorldRegion.toEntity(): WorldRegionEntity = WorldRegionEntity(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    status = status.name,
    order = order,
    chapter = chapter,
    unlockRequirementsJson = RoomJson.toJson(unlockRequirements),
    connectionsJson = RoomJson.toJsonList(connections),
    travelMethodsJson = RoomJson.toJsonList(travelMethods),
    npcIdsJson = RoomJson.toJsonList(npcIds),
    questIdsJson = RoomJson.toJsonList(questIds),
    completionPercentage = completionPercentage,
    musicTrack = musicTrack,
    ambienceTrack = ambienceTrack,
    mapPositionX = mapPositionX,
    mapPositionY = mapPositionY,
    color = color,
    icon = icon,
)

fun WorldRegionEntity.toDomain(
    landmarks: List<Landmark> = emptyList(),
    collectibles: List<CollectibleLocation> = emptyList(),
): WorldRegion = WorldRegion(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    status = RegionStatus.valueOf(status),
    order = order,
    chapter = chapter,
    unlockRequirements = RoomJson.fromJsonOrNull<UnlockRequirement>(unlockRequirementsJson) ?: UnlockRequirement(),
    connections = RoomJson.fromJsonList(connectionsJson),
    travelMethods = RoomJson.fromJsonList(travelMethodsJson),
    landmarks = landmarks,
    npcIds = RoomJson.fromJsonList(npcIdsJson),
    questIds = RoomJson.fromJsonList(questIdsJson),
    collectibles = collectibles,
    completionPercentage = completionPercentage,
    musicTrack = musicTrack,
    ambienceTrack = ambienceTrack,
    mapPositionX = mapPositionX,
    mapPositionY = mapPositionY,
    color = color,
    icon = icon,
)

fun RegionProgress.toEntity(): WorldRegionProgressEntity = WorldRegionProgressEntity(
    regionId = regionId,
    status = status.name,
    completionPercentage = completionPercentage,
    discoveredLocationsJson = RoomJson.toJsonList(discoveredLocations),
    completedQuestsJson = RoomJson.toJsonList(completedQuests),
    collectedItemsJson = RoomJson.toJsonList(collectedItems),
    visitedNpcsJson = RoomJson.toJsonList(visitedNpcs),
    unlockedFastTravel = unlockedFastTravel,
    firstVisitedAt = firstVisitedAt,
    lastVisitedAt = lastVisitedAt,
    totalPlayTimeMinutes = totalPlayTimeMinutes,
)

fun WorldRegionProgressEntity.toDomain(): RegionProgress = RegionProgress(
    regionId = regionId,
    status = RegionStatus.valueOf(status),
    completionPercentage = completionPercentage,
    discoveredLocations = RoomJson.fromJsonList(discoveredLocationsJson),
    completedQuests = RoomJson.fromJsonList(completedQuestsJson),
    collectedItems = RoomJson.fromJsonList(collectedItemsJson),
    visitedNpcs = RoomJson.fromJsonList(visitedNpcsJson),
    unlockedFastTravel = unlockedFastTravel,
    firstVisitedAt = firstVisitedAt,
    lastVisitedAt = lastVisitedAt,
    totalPlayTimeMinutes = totalPlayTimeMinutes,
)

fun RegionConnection.toEntity(): WorldConnectionEntity = WorldConnectionEntity(
    id = "${fromRegionId}|${toRegionId}",
    fromRegionId = fromRegionId,
    toRegionId = toRegionId,
    travelMethod = travelMethod.name,
    travelTimeMinutes = travelTimeMinutes,
    isUnlocked = isUnlocked,
    description = description,
)

fun WorldConnectionEntity.toDomain(): RegionConnection = RegionConnection(
    fromRegionId = fromRegionId,
    toRegionId = toRegionId,
    travelMethod = TravelMethod.valueOf(travelMethod),
    travelTimeMinutes = travelTimeMinutes,
    isUnlocked = isUnlocked,
    description = description,
)

fun WorldLocation.toEntity(): WorldLocationEntity = WorldLocationEntity(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    regionId = regionId,
    type = type.name,
    positionX = positionX,
    positionY = positionY,
    npcIdsJson = RoomJson.toJsonList(npcIds),
    questIdsJson = RoomJson.toJsonList(questIds),
    isDiscovered = isDiscovered,
    isAccessible = isAccessible,
)

fun WorldLocationEntity.toDomain(): WorldLocation = WorldLocation(
    id = id,
    name = name,
    nameCn = nameCn,
    description = description,
    regionId = regionId,
    type = LandmarkType.valueOf(type),
    positionX = positionX,
    positionY = positionY,
    npcIds = RoomJson.fromJsonList(npcIdsJson),
    questIds = RoomJson.fromJsonList(questIdsJson),
    isDiscovered = isDiscovered,
    isAccessible = isAccessible,
)

fun Landmark.toEntity(): WorldLandmarkEntity = WorldLandmarkEntity(
    id = id,
    name = name,
    nameCn = nameCn,
    type = type.name,
    description = description,
    regionId = regionId,
    positionX = positionX,
    positionY = positionY,
    isDiscovered = isDiscovered,
    isInteractable = isInteractable,
    npcIdsJson = RoomJson.toJsonList(npcIds),
    questIdsJson = RoomJson.toJsonList(questIds),
)

fun WorldLandmarkEntity.toDomain(): Landmark = Landmark(
    id = id,
    name = name,
    nameCn = nameCn,
    type = LandmarkType.valueOf(type),
    description = description,
    regionId = regionId,
    positionX = positionX,
    positionY = positionY,
    isDiscovered = isDiscovered,
    isInteractable = isInteractable,
    npcIds = RoomJson.fromJsonList(npcIdsJson),
    questIds = RoomJson.fromJsonList(questIdsJson),
)

fun CollectibleLocation.toEntity(): WorldCollectibleEntity = WorldCollectibleEntity(
    id = id,
    name = name,
    type = type.name,
    regionId = regionId,
    locationId = locationId,
    positionX = positionX,
    positionY = positionY,
    isCollected = isCollected,
    isHidden = isHidden,
    description = description,
    culturalNote = culturalNote,
)

fun WorldCollectibleEntity.toDomain(): CollectibleLocation = CollectibleLocation(
    id = id,
    name = name,
    type = CollectibleType.valueOf(type),
    regionId = regionId,
    locationId = locationId,
    positionX = positionX,
    positionY = positionY,
    isCollected = isCollected,
    isHidden = isHidden,
    description = description,
    culturalNote = culturalNote,
)
