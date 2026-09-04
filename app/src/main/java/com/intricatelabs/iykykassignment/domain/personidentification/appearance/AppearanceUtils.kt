package com.intricatelabs.iykykassignment.domain.personidentification.appearance

object AppearanceUtils {
    /** Dot product = cosine similarity, since embeddings are already L2-normalized. */
    fun cosineSim(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}