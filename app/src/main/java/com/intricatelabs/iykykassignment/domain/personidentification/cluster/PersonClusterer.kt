package com.intricatelabs.iykykassignment.domain.personidentification.cluster

import android.util.Log
import com.intricatelabs.iykykassignment.data.dao.AppearanceSegmentDao
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
import com.intricatelabs.iykykassignment.data.entities.AppearanceSegmentEntity
import com.intricatelabs.iykykassignment.data.entities.PersonClusterEntity
import com.intricatelabs.iykykassignment.domain.personidentification.appearance.AppearanceUtils
import com.intricatelabs.iykykassignment.domain.personidentification.appearance.ClosedAppearance

/**
 * Takes a CLOSED appearance segment and decides: is this a person already
 * seen elsewhere in this video, or a new one? Greedy/online — compares
 * against existing cluster centroids rather than re-clustering everything
 * from scratch, so it works one segment at a time as they close, without
 * ever needing every embedding in memory at once.
 */
class PersonClusterer(
    private val videoUri: String,
    private val identityThreshold: Float = 0.6f,
    private val clusterDao: PersonClusterDao,
    private val segmentDao: AppearanceSegmentDao,
    private val faceDetectionDao: FaceDetectionDao
) {
    private val clusters = mutableListOf<ClusterState>()

    suspend fun assign(closedAppearance: ClosedAppearance) {
        val closestMatch = clusters.maxByOrNull {
            AppearanceUtils.cosineSim(
                it.centroid,
                closedAppearance.representativeEmbedding
            )
        }
        val matchScore = closestMatch?.let {
            AppearanceUtils.cosineSim(
                it.centroid,
                closedAppearance.representativeEmbedding
            )
        } ?: -1f
        Log.d("ClusterDebug", "bestScore=$matchScore threshold=$identityThreshold " +
                "matched=${matchScore >= identityThreshold} existingClusters=${clusters.size} at ${closedAppearance.startMs} - ${closedAppearance.endMs}")

        val clusterId: Long

        if (closestMatch != null && matchScore >= identityThreshold) {
            clusterId = closestMatch.id
            closestMatch.appearanceCount++
            // Weighted running average — one noisy segment (bad lighting,
            // odd angle) shouldn't swing an established person's centroid
            // as much as it would if this were their first appearance.
            closestMatch.centroid = weightedAverage(
                closestMatch.centroid, closestMatch.appearanceCount - 1, closedAppearance.representativeEmbedding
            )
            clusterDao.updateCentroidAndCount(clusterId, closestMatch.centroid.joinToString(","), closestMatch.appearanceCount)
        } else {
             clusterId = clusterDao.insertCluster(
                 PersonClusterEntity(
                     videoUri = videoUri,
                     appearanceCount = 1,
                     centroidEmbedding = closedAppearance.representativeEmbedding,
                     representativeFaceId = null // still null at this stage
                 )
             )
            clusters.add(ClusterState(clusterId, closedAppearance.representativeEmbedding, 1))
        }

         segmentDao.insertSegment(
             AppearanceSegmentEntity(
                 clusterId = clusterId,
                 videoUri = videoUri,
                 startTimestampMs = closedAppearance.startMs,
                 endTimestampMs = closedAppearance.endMs
             )
         )
         faceDetectionDao.assignCluster(closedAppearance.faceIds, clusterId)
    }

    private fun weightedAverage(existing: FloatArray, existingWeight: Int, new: FloatArray): FloatArray {
        val total = existingWeight + 1
        return FloatArray(existing.size) { i -> (existing[i] * existingWeight + new[i]) / total }
    }
}