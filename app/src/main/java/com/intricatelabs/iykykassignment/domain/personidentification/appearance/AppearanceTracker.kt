package com.intricatelabs.iykykassignment.domain.personidentification.appearance

import android.util.Log

/**
 * Groups consecutive per-frame face detections into "appearance" segments —
 * matches the assignment's definition: a continuous visible span for one
 * person. This is SHORT-TERM, frame-to-frame continuity only — it does NOT
 * decide whether this is the same person as an appearance from 15 seconds
 * ago. That's a separate, harder problem: PersonClusterer, once a segment
 * closes here.
 */
class AppearanceTracker(
    private val continuityThreshold: Float = 0.5f,
    private val maxGapMs: Long = 1000L
) {

    // list of ongoing appearances currently in front of the camera
    private val activeAppearances = mutableListOf<Appearance>()

    fun offerFrame(facesInThisFrame: List<Pair<Long, FloatArray>>, timestampMs: Long) {
        val appearancesClaimedInThisFrame = mutableSetOf<Appearance>()

        for ((faceId, embedding) in facesInThisFrame) {
            val bestMatchingAppearance = activeAppearances
                .filter {
                    it !in appearancesClaimedInThisFrame &&
                            timestampMs - it.lastSeenMs <= maxGapMs
                }
                .maxByOrNull { AppearanceUtils.cosineSim(it.embeddings.last(), embedding) }

            val score = bestMatchingAppearance?.let {
                AppearanceUtils.cosineSim(
                    it.embeddings.last(),
                    embedding
                )
            } ?: -1f
            Log.d("AppearanceDebug", "score=$score threshold=$continuityThreshold " +
                    "matched=${score >= continuityThreshold} existingClusters=${activeAppearances.size} at time $timestampMs")

            if (bestMatchingAppearance != null && score >= continuityThreshold) {
                bestMatchingAppearance.lastSeenMs = timestampMs
                bestMatchingAppearance.faceIds.add(faceId)
                bestMatchingAppearance.embeddings.add(embedding)
                appearancesClaimedInThisFrame.add(bestMatchingAppearance)
            } else {
                val newAppearance = Appearance(startMs = timestampMs, lastSeenMs = timestampMs).apply {
                    faceIds.add(faceId)
                    embeddings.add(embedding)
                }
                activeAppearances.add(newAppearance)
                appearancesClaimedInThisFrame.add(newAppearance)
            }
        }
    }

    fun closeStaleAppearances(currentTimeMs: Long): List<ClosedAppearance> {
        val (stale, alive) = activeAppearances.partition { currentTimeMs - it.lastSeenMs > maxGapMs }
        activeAppearances.clear()
        activeAppearances.addAll(alive)
        return stale.map { it.toClosedAppearance() }
    }

    fun flushAll(): List<ClosedAppearance> {
        val remaining = activeAppearances.map { it.toClosedAppearance() }
        activeAppearances.clear()
        return remaining
    }

    private fun Appearance.toClosedAppearance() = ClosedAppearance(
        startMs = startMs,
        endMs = lastSeenMs,
        faceIds = faceIds.toList(),
        representativeEmbedding = averageEmbedding(embeddings)
    )

    private fun averageEmbedding(vectors: List<FloatArray>): FloatArray {
        val size = vectors.first().size
        val avg = FloatArray(size)
        for (v in vectors) for (i in v.indices) avg[i] += v[i]
        for (i in avg.indices) avg[i] /= vectors.size
        return avg
    }

}