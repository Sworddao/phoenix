package com.sworddao.phoenix.feature.npc.di

import com.sworddao.phoenix.feature.npc.data.MockNpcRepository
import com.sworddao.phoenix.feature.npc.domain.NpcRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NpcModule {

    @Binds
    @Singleton
    abstract fun bindNpcRepository(
        impl: MockNpcRepository
    ): NpcRepository
}
