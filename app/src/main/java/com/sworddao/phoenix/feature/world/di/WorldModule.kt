package com.sworddao.phoenix.feature.world.di

import com.sworddao.phoenix.feature.world.data.MockWorldRepository
import com.sworddao.phoenix.feature.world.domain.WorldRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorldModule {
    @Binds
    @Singleton
    abstract fun bindWorldRepository(impl: MockWorldRepository): WorldRepository
}
