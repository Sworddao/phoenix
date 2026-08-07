package com.sworddao.phoenix.feature.gameplay.di

import com.sworddao.phoenix.feature.gameplay.data.RoomGameProgressRepository
import com.sworddao.phoenix.feature.gameplay.domain.GameProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GameplayModule {

    @Binds
    @Singleton
    abstract fun bindGameProgressRepository(
        impl: RoomGameProgressRepository
    ): GameProgressRepository
}
