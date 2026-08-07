package com.sworddao.phoenix.feature.vocabulary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_word")
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val mandarin: String,
    val pinyin: String,
    val english: String,
    val hanzi: String? = null,
    val audioPath: String? = null,
    val category: String,
    val difficulty: String,
    val exampleSentence: String,
    val exampleTranslation: String,
    val examplePinyin: String,
    val relatedNpcId: String? = null,
    val relatedQuestId: String? = null,
    val relatedRegionId: String? = null,
    val discoveredAt: Long? = null,
    val mastery: String,
    val timesReviewed: Int = 0,
    val timesSpoken: Int = 0,
    val timesHeard: Int = 0,
    val timesRead: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val tagsJson: String = "[]"
)

@Entity(tableName = "vocabulary_progress")
data class VocabularyProgressEntity(
    @PrimaryKey val wordId: String,
    val mastery: String,
    val timesReviewed: Int = 0,
    val timesSpoken: Int = 0,
    val timesHeard: Int = 0,
    val timesRead: Int = 0,
    val lastReviewedAt: Long? = null,
    val discoveredAt: Long? = null,
    val isFavorite: Boolean = false
)
