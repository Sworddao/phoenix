package com.sworddao.phoenix.feature.listening.di

import com.sworddao.phoenix.feature.listening.data.AudioEngine
import com.sworddao.phoenix.feature.listening.data.MockAudioEngine
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.listening.domain.ListeningRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ListeningModule {

    @Binds
    @Singleton
    abstract fun bindListeningRepository(
        impl: MockListeningRepository
    ): ListeningRepository

    @Binds
    @Singleton
    abstract fun bindAudioEngine(
        impl: MockAudioEngine
    ): AudioEngine
}
