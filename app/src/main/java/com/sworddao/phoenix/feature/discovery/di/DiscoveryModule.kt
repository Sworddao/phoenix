package com.sworddao.phoenix.feature.discovery.di

import com.sworddao.phoenix.feature.discovery.data.DiscoveryRepository
import com.sworddao.phoenix.feature.discovery.data.RoomDiscoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryModule {

    @Binds
    @Singleton
    abstract fun bindDiscoveryRepository(
        impl: RoomDiscoveryRepository,
    ): DiscoveryRepository
}
