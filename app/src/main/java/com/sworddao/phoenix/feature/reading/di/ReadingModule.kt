package com.sworddao.phoenix.feature.reading.di

import com.sworddao.phoenix.feature.reading.data.HanziRenderer
import com.sworddao.phoenix.feature.reading.data.MockHanziRenderer
import com.sworddao.phoenix.feature.reading.data.MockReadingRepository
import com.sworddao.phoenix.feature.reading.domain.ReadingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReadingModule {

    @Binds
    @Singleton
    abstract fun bindReadingRepository(
        impl: MockReadingRepository
    ): ReadingRepository

    @Binds
    @Singleton
    abstract fun bindHanziRenderer(
        impl: MockHanziRenderer
    ): HanziRenderer
}
