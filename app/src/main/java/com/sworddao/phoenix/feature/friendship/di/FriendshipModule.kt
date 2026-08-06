package com.sworddao.phoenix.feature.friendship.di

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FriendshipModule {

    @Binds
    @Singleton
    abstract fun bindFriendshipRepository(
        mockFriendshipRepository: MockFriendshipRepository
    ): FriendshipRepository
}
