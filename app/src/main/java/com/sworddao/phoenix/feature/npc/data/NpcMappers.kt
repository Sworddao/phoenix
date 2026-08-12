package com.sworddao.phoenix.feature.npc.data

import com.sworddao.phoenix.data.local.RoomJson

fun NpcEntity.toDomain(): Npc = Npc(
    id = id,
    displayName = displayName,
    occupation = occupation,
    personality = personality,
    currentLocation = currentLocation,
    friendshipXp = friendshipXp,
    schedule = NpcSchedule(entries = RoomJson.fromJsonList(scheduleJson)),
    avatarEmoji = avatarEmoji,
    idleAnimationState = runCatching { IdleAnimationState.valueOf(idleAnimationState) }
        .getOrDefault(IdleAnimationState.IDLE),
    interactionAvailability = runCatching { InteractionAvailability.valueOf(interactionAvailability) }
        .getOrDefault(InteractionAvailability.AVAILABLE),
    unlockRequirements = unlockRequirements,
    vocabularyCategories = RoomJson.fromJsonList(vocabularyCategoriesJson),
    dialogueReferences = RoomJson.fromJsonList(dialogueReferencesJson),
    shortDescription = shortDescription
)

fun Npc.toEntity(): NpcEntity = NpcEntity(
    id = id,
    displayName = displayName,
    occupation = occupation,
    personality = personality,
    currentLocation = currentLocation,
    friendshipXp = friendshipXp,
    scheduleJson = RoomJson.toJsonList(schedule.entries),
    avatarEmoji = avatarEmoji,
    idleAnimationState = idleAnimationState.name,
    interactionAvailability = interactionAvailability.name,
    unlockRequirements = unlockRequirements,
    vocabularyCategoriesJson = RoomJson.toJsonList(vocabularyCategories),
    dialogueReferencesJson = RoomJson.toJsonList(dialogueReferences),
    shortDescription = shortDescription
)
