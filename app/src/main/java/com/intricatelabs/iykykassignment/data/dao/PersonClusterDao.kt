package com.intricatelabs.iykykassignment.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.intricatelabs.iykykassignment.data.entities.PersonClusterEntity

@Dao
interface PersonClusterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: PersonClusterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClusters(clusters: List<PersonClusterEntity>)

    @Query("SELECT * FROM person_clusters WHERE videoUri = :uri")
    suspend fun getClustersForVideo(uri: String): List<PersonClusterEntity>

    @Query("SELECT id FROM person_clusters WHERE videoUri = :uri")
    suspend fun getClusterIdsForVideo(uri: String): List<Long>

    @Query("DELETE FROM person_clusters WHERE videoUri = :uri")
    suspend fun deleteClustersForVideo(uri: String)

    @Query("UPDATE person_clusters SET centroidEmbedding = :centroidEmbedding, appearanceCount = :appearanceCount WHERE id = :id")
    suspend fun updateCentroidAndCount(id: Long, centroidEmbedding: String, appearanceCount: Int)

    @Query("UPDATE person_clusters SET representativeFaceId = :faceId WHERE id = :clusterId")
    suspend fun setRepresentativeFace(clusterId: Long, faceId: Long)
}
