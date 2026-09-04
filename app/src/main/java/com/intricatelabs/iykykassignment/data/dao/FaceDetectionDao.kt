package com.intricatelabs.iykykassignment.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity

@Dao
interface FaceDetectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(detection: FaceDetectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetections(detections: List<FaceDetectionEntity>): List<Long>

    @Query("SELECT * FROM face_detections WHERE videoUri = :uri")
    suspend fun getDetectionsForVideo(uri: String): List<FaceDetectionEntity>

    @Query("SELECT * FROM face_detections WHERE id = :faceId")
    suspend fun getFaceById(faceId: Long): FaceDetectionEntity?

    @Query("DELETE FROM face_detections WHERE videoUri = :uri")
    suspend fun deleteDetectionsForVideo(uri: String)

    @Query("UPDATE face_detections SET clusterId = :clusterId WHERE id IN (:faceIds)")
    suspend fun assignCluster(faceIds: List<Long>, clusterId: Long)

    @Query("SELECT * FROM face_detections WHERE clusterId = :clusterId")
    suspend fun getFacesForCluster(clusterId: Long): List<FaceDetectionEntity>
}
