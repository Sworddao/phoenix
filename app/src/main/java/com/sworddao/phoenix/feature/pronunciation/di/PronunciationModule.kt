package com.sworddao.phoenix.feature.pronunciation.di

import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationEngine
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationEngine
import com.sworddao.phoenix.feature.pronunciation.data.RoomPronunciationRepository
import com.sworddao.phoenix.feature.pronunciation.domain.PronunciationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PronunciationModule {

    @Binds
    @Singleton
    abstract fun bindPronunciationRepository(
        impl: RoomPronunciationRepository
    ): PronunciationRepository

    @Binds
    @Singleton
    abstract fun bindPronunciationEngine(
        impl: MockPronunciationEngine
    ): PronunciationEngine
}