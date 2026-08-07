package com.sworddao.phoenix.feature.review.di

import com.sworddao.phoenix.feature.review.data.RoomReviewRepository
import com.sworddao.phoenix.feature.review.domain.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewModule {

    @Binds
    @Singleton
    abstract fun bindReviewRepository(implementation: RoomReviewRepository): ReviewRepository
}