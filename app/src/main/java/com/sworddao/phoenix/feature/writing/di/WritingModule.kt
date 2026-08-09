package com.sworddao.phoenix.feature.writing.di

import com.sworddao.phoenix.feature.writing.data.MockWritingEngine
import com.sworddao.phoenix.feature.writing.data.RoomWritingRepository
import com.sworddao.phoenix.feature.writing.data.WritingEngine
import com.sworddao.phoenix.feature.writing.domain.WritingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WritingModule {

    @Binds
    @Singleton
    abstract fun bindWritingRepository(
        impl: RoomWritingRepository
    ): WritingRepository

    @Binds
    @Singleton
    abstract fun bindWritingEngine(
        impl: MockWritingEngine
    ): WritingEngine
}
