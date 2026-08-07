package com.sworddao.phoenix.feature.vocabulary.data

import com.sworddao.phoenix.data.local.RoomJson
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.first

fun VocabularyEntity.toDomain(): VocabularyWord = VocabularyWord(
    id = id,
    mandarin = mandarin,
    pinyin = pinyin,
    english = english,
    hanzi = hanzi,
    audioPath = audioPath,
    category = VocabularyCategory.valueOf(category),
    difficulty = VocabularyDifficulty.valueOf(difficulty),
    exampleSentence = exampleSentence,
    exampleTranslation = exampleTranslation,
    examplePinyin = examplePinyin,
    relatedNpcId = relatedNpcId,
    relatedQuestId = relatedQuestId,
    relatedRegionId = relatedRegionId,
    discoveredAt = discoveredAt,
    mastery = VocabularyMastery.valueOf(mastery),
    timesReviewed = timesReviewed,
    timesSpoken = timesSpoken,
    timesHeard = timesHeard,
    timesRead = timesRead,
    isFavorite = isFavorite,
    notes = notes,
    tags = RoomJson.fromJsonList(tagsJson),
)

fun VocabularyWord.toEntity(): VocabularyEntity = VocabularyEntity(
    id = id,
    mandarin = mandarin,
    pinyin = pinyin,
    english = english,
    hanzi = hanzi,
    audioPath = audioPath,
    category = category.name,
    difficulty = difficulty.name,
    exampleSentence = exampleSentence,
    exampleTranslation = exampleTranslation,
    examplePinyin = examplePinyin,
    relatedNpcId = relatedNpcId,
    relatedQuestId = relatedQuestId,
    relatedRegionId = relatedRegionId,
    discoveredAt = discoveredAt,
    mastery = mastery.name,
    timesReviewed = timesReviewed,
    timesSpoken = timesSpoken,
    timesHeard = timesHeard,
    timesRead = timesRead,
    isFavorite = isFavorite,
    notes = notes,
    tagsJson = RoomJson.toJsonList(tags),
)

fun VocabularyProgressEntity.toDomain(): VocabularyProgress = VocabularyProgress(
    wordId = wordId,
    mastery = VocabularyMastery.valueOf(mastery),
    timesReviewed = timesReviewed,
    timesSpoken = timesSpoken,
    timesHeard = timesHeard,
    timesRead = timesRead,
    lastReviewedAt = lastReviewedAt,
    discoveredAt = discoveredAt,
    isFavorite = isFavorite,
)

fun VocabularyProgress.toEntity(): VocabularyProgressEntity = VocabularyProgressEntity(
    wordId = wordId,
    mastery = mastery.name,
    timesReviewed = timesReviewed,
    timesSpoken = timesSpoken,
    timesHeard = timesHeard,
    timesRead = timesRead,
    lastReviewedAt = lastReviewedAt,
    discoveredAt = discoveredAt,
    isFavorite = isFavorite,
)
