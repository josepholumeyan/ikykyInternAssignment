package com.intricatelabs.iykykassignment.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import com.intricatelabs.iykykassignment.data.converters.RoomConverters
import com.intricatelabs.iykykassignment.data.dao.AppearanceSegmentDao
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
import com.intricatelabs.iykykassignment.data.entities.AppearanceSegmentEntity
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity
import com.intricatelabs.iykykassignment.data.entities.PersonClusterEntity

@Database(
    entities = [
        FaceDetectionEntity::class,
        PersonClusterEntity::class,
        AppearanceSegmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faceDetectionDao(): FaceDetectionDao
    abstract fun personClusterDao(): PersonClusterDao
    abstract fun appearanceSegmentDao(): AppearanceSegmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    @Transaction
    suspend fun clearVideoData(uri: String) {
        appearanceSegmentDao().deleteSegmentsForVideo(uri)
        personClusterDao().deleteClustersForVideo(uri)
        faceDetectionDao().deleteDetectionsForVideo(uri)
    }
}
