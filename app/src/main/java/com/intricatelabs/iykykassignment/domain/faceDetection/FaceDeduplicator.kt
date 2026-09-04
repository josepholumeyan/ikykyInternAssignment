package com.intricatelabs.iykykassignment.domain.faceDetection

import com.intricatelabs.iykykassignment.domain.personidentification.appearance.AppearanceUtils

object FaceDeduplicator {

    private const val EMBEDDING_SIMILARITY_THRESHOLD = 0.9f

    fun <T> dedupeByEmbedding(
        candidates: List<T>,
        embeddingOf: (T) -> FloatArray,
        qualityOf: (T) -> Float
    ): List<T> {
        val kept = mutableListOf<T>()

        // Best-quality candidate survives when a duplicate is found.
        val sortedByQuality = candidates.sortedByDescending(qualityOf)

        for (candidate in sortedByQuality) {
            val isDuplicateOfKept = kept.any { existing ->
                AppearanceUtils.cosineSim(embeddingOf(existing), embeddingOf(candidate)) >
                        EMBEDDING_SIMILARITY_THRESHOLD
            }
            if (!isDuplicateOfKept) kept.add(candidate)
        }

        return kept
    }
}