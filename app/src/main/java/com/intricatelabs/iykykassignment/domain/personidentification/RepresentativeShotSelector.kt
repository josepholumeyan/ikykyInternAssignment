package com.intricatelabs.iykykassignment.domain.personidentification

import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepresentativeShotSelector @Inject constructor(
    private val faceDetectionDao: FaceDetectionDao,
    private val clusterDao: PersonClusterDao
) {
    private val frontalityWeight = 0.35f
    private val eyesOpenWeight = 0.25f
    private val smileWeight = 0.15f
    private val sharpnessWeight = 0.25f
    private val clippedPenalty = 0.5f

    suspend fun selectAll(
        videoUri: String,
        onProgress: (progress: Int) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        val clusterIds = clusterDao.getClusterIdsForVideo(videoUri)
        if (clusterIds.isEmpty()) return@withContext

        val total = clusterIds.size

        clusterIds.forEachIndexed { index, clusterId ->
            val candidates = faceDetectionDao.getFacesForCluster(clusterId)
            if (candidates.isNotEmpty()) {
                val best = candidates.maxByOrNull { score(it, candidates) }
                if (best != null) {
                    clusterDao.setRepresentativeFace(clusterId, best.id)
                }
            }
            onProgress(((index + 1).toFloat() / total * 100).toInt())
        }
    }

    private fun score(face: FaceDetectionEntity, candidatesInSameCluster: List<FaceDetectionEntity>): Float {
        // sharpnessScore is raw Laplacian variance — no fixed scale, unlike
        // ML Kit's 0-1 probabilities. Normalize against THIS person's own
        // candidates so it's comparable to frontality/eyesOpen/smile before
        // combining into one weighted score.
        val maxSharpness = candidatesInSameCluster.maxOf { it.sharpnessScore }.coerceAtLeast(1e-6f)
        val normalizedSharpness = face.sharpnessScore / maxSharpness

        var score = face.frontalityScore * frontalityWeight +
                face.eyesOpenProb * eyesOpenWeight +
                face.smileProb * smileWeight +
                normalizedSharpness * sharpnessWeight

        if (face.isClipped) score -= clippedPenalty

        return score
    }
}