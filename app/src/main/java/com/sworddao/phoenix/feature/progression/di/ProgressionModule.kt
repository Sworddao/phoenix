package com.sworddao.phoenix.feature.progression.di

import com.sworddao.phoenix.feature.progression.data.MockProgressionRepository
import com.sworddao.phoenix.feature.progression.domain.ProgressionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProgressionModule {

    @Binds
    @Singleton
    abstract fun bindProgressionRepository(
        impl: MockProgressionRepository
    ): ProgressionRepository
}
