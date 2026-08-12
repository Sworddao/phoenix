package com.sworddao.phoenix.di

import android.content.Context
import androidx.room.Room
import com.sworddao.phoenix.data.local.AppMetadataDao
import com.sworddao.phoenix.data.local.MIGRATION_2_3
import com.sworddao.phoenix.data.local.MIGRATION_3_4
import com.sworddao.phoenix.data.local.MIGRATION_4_5
import com.sworddao.phoenix.data.local.PhoenixDatabase
import com.sworddao.phoenix.feature.dialogue.data.DialogueDao
import com.sworddao.phoenix.feature.discovery.data.DiscoveryDao
import com.sworddao.phoenix.feature.friendship.data.FriendshipDao
import com.sworddao.phoenix.feature.gameplay.data.GameProgressDao
import com.sworddao.phoenix.feature.listening.data.ListeningDao
import com.sworddao.phoenix.feature.npc.data.NpcDao
import com.sworddao.phoenix.feature.passport.data.PassportDao
import com.sworddao.phoenix.feature.progression.data.ProgressionDao
import com.sworddao.phoenix.feature.pronunciation.data.SpeakingDao
import com.sworddao.phoenix.feature.quest.data.QuestDao
import com.sworddao.phoenix.feature.reading.data.ReadingDao
import com.sworddao.phoenix.feature.review.data.ReviewDao
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDao
import com.sworddao.phoenix.feature.world.data.WorldDao
import com.sworddao.phoenix.feature.writing.data.WritingDao
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
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppMetadataDao(database: PhoenixDatabase): AppMetadataDao {
        return database.appMetadataDao()
    }

    @Provides
    @Singleton
    fun provideFriendshipDao(database: PhoenixDatabase): FriendshipDao {
        return database.friendshipDao()
    }

    @Provides
    @Singleton
    fun provideVocabularyDao(database: PhoenixDatabase): VocabularyDao {
        return database.vocabularyDao()
    }

    @Provides
    @Singleton
    fun provideDiscoveryDao(database: PhoenixDatabase): DiscoveryDao {
        return database.discoveryDao()
    }

    @Provides
    @Singleton
    fun provideQuestDao(database: PhoenixDatabase): QuestDao {
        return database.questDao()
    }

    @Provides
    @Singleton
    fun provideGameProgressDao(database: PhoenixDatabase): GameProgressDao {
        return database.gameProgressDao()
    }

    @Provides
    @Singleton
    fun providePassportDao(database: PhoenixDatabase): PassportDao {
        return database.passportDao()
    }

    @Provides
    @Singleton
    fun provideWorldDao(database: PhoenixDatabase): WorldDao {
        return database.worldDao()
    }

    @Provides
    @Singleton
    fun provideReadingDao(database: PhoenixDatabase): ReadingDao {
        return database.readingDao()
    }

    @Provides
    @Singleton
    fun provideListeningDao(database: PhoenixDatabase): ListeningDao {
        return database.listeningDao()
    }

    @Provides
    @Singleton
    fun provideSpeakingDao(database: PhoenixDatabase): SpeakingDao {
        return database.speakingDao()
    }

    @Provides
    @Singleton
    fun provideReviewDao(database: PhoenixDatabase): ReviewDao {
        return database.reviewDao()
    }

    @Provides
    @Singleton
    fun provideProgressionDao(database: PhoenixDatabase): ProgressionDao {
        return database.progressionDao()
    }

    @Provides
    @Singleton
    fun provideWritingDao(database: PhoenixDatabase): WritingDao {
        return database.writingDao()
    }

    @Provides
    @Singleton
    fun provideNpcDao(database: PhoenixDatabase): NpcDao {
        return database.npcDao()
    }

    @Provides
    @Singleton
    fun provideDialogueDao(database: PhoenixDatabase): DialogueDao {
        return database.dialogueDao()
    }
}
