package com.intricatelabs.iykykassignment.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.intricatelabs.iykykassignment.data.entities.AppearanceSegmentEntity

@Dao
interface AppearanceSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: AppearanceSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<AppearanceSegmentEntity>)

    @Query("DELETE FROM appearance_segments WHERE clusterId IN (SELECT id FROM person_clusters WHERE videoUri = :uri)")
    suspend fun deleteSegmentsForVideo(uri: String)
}
