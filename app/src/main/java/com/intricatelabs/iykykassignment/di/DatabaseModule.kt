package com.intricatelabs.iykykassignment.di

import android.content.Context
import androidx.room.Room
import com.intricatelabs.iykykassignment.data.AppDatabase
import com.intricatelabs.iykykassignment.data.dao.AppearanceSegmentDao
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "iykyk_database"
        ).build()
    }

    @Provides
    fun provideFaceDetectionDao(database: AppDatabase): FaceDetectionDao {
        return database.faceDetectionDao()
    }

    @Provides
    fun providePersonClusterDao(database: AppDatabase): PersonClusterDao {
        return database.personClusterDao()
    }

    @Provides
    fun provideAppearanceSegmentDao(database: AppDatabase): AppearanceSegmentDao {
        return database.appearanceSegmentDao()
    }
}
