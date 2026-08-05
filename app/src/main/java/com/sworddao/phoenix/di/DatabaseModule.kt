package com.sworddao.phoenix.di

import android.content.Context
import androidx.room.Room
import com.sworddao.phoenix.data.local.PhoenixDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PhoenixDatabase {
        return Room.databaseBuilder(
            context,
            PhoenixDatabase::class.java,
            "phoenix_database"
        ).build()
    }
}
