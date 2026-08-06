package com.sworddao.phoenix.feature.progression.domain

import com.sworddao.phoenix.feature.progression.data.CurrentObjective
import com.sworddao.phoenix.feature.progression.data.DailyProgress
import com.sworddao.phoenix.feature.progression.data.FeatureUnlockEntry
import com.sworddao.phoenix.feature.progression.data.LearningProgress
import com.sworddao.phoenix.feature.progression.data.PlayerProgress
import com.sworddao.phoenix.feature.progression.data.ProgressionResult
import com.sworddao.phoenix.feature.progression.data.RecentUnlock
import com.sworddao.phoenix.feature.progression.data.XpSource
import kotlinx.coroutines.flow.Flow

interface ProgressionRepository {
    fun getPlayerProgress(): Flow<PlayerProgress>
    fun getLearningProgress(): Flow<LearningProgress>
    fun getDailyProgress(): Flow<DailyProgress>
    fun getRecentUnlocks(): Flow<List<RecentUnlock>>
    fun getCurrentObjectives(): Flow<List<CurrentObjective>>
    fun getFeatureUnlockTimeline(): Flow<List<FeatureUnlockEntry>>
    suspend fun refresh(): ProgressionResult
    suspend fun awardXp(source: XpSource, count: Int = 1): ProgressionResult
    suspend fun resetProgression()
}
