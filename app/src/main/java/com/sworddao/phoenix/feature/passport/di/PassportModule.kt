package com.sworddao.phoenix.feature.passport.di

import com.sworddao.phoenix.feature.passport.data.RoomPassportRepository
import com.sworddao.phoenix.feature.passport.domain.PassportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PassportModule {
    @Binds
    @Singleton
    abstract fun bindPassportRepository(impl: RoomPassportRepository): PassportRepository
}
