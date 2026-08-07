package com.sworddao.phoenix.feature.review.data

import com.sworddao.phoenix.data.local.RoomJson

data class ReviewState(
    val itemIdCounter: Int = 0,
    val historyIdCounter: Int = 0,
    val sessionIdCounter: Int = 0,
    val todayDate: String = "",
    val reviewsToday: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val xpEarnedTotal: Int = 0,
    val reviewedWordIds: Set<String> = emptySet(),
)

fun ReviewState.toEntity(): ReviewStateEntity = ReviewStateEntity(
    id = "all",
    itemIdCounter = itemIdCounter,
    historyIdCounter = historyIdCounter,
    sessionIdCounter = sessionIdCounter,
    todayDate = todayDate,
    reviewsToday = reviewsToday,
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    xpEarnedTotal = xpEarnedTotal,
    reviewedWordIdsJson = RoomJson.toJsonList(reviewedWordIds.toList()),
)

fun ReviewStateEntity.toDomain(): ReviewState = ReviewState(
    itemIdCounter = itemIdCounter,
    historyIdCounter = historyIdCounter,
    sessionIdCounter = sessionIdCounter,
    todayDate = todayDate,
    reviewsToday = reviewsToday,
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    xpEarnedTotal = xpEarnedTotal,
    reviewedWordIds = RoomJson.fromJsonList<String>(reviewedWordIdsJson).toSet(),
)
