package com.sworddao.phoenix.feature.vocabulary.di

import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VocabularyModule {
    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(impl: MockVocabularyRepository): VocabularyRepository
}
