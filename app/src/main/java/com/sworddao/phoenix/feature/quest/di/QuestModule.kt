package com.sworddao.phoenix.feature.quest.di

import com.sworddao.phoenix.feature.quest.data.RoomQuestRepository
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuestModule {
    @Binds
    @Singleton
    abstract fun bindQuestRepository(impl: RoomQuestRepository): QuestRepository
}
