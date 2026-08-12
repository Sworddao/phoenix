package com.sworddao.phoenix.feature.dialogue.di

import com.sworddao.phoenix.feature.dialogue.data.RoomDialogueRepository
import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DialogueModule {

    @Binds
    @Singleton
    abstract fun bindDialogueRepository(
        impl: RoomDialogueRepository
    ): DialogueRepository
}
