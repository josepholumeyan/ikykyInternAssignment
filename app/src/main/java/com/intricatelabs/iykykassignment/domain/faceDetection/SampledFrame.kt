package com.intricatelabs.iykykassignment.domain.faceDetection

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face

data class SampledFrame(val timestampMs: Long, val bitmap: Bitmap)

// Embed + score EVERY raw candidate first — dedup needs embeddings to
// compare identities, so it can't run before this anymore. Each
// candidate's crop is saved to disk here too, since we don't yet know
// which ones will survive dedup.
data class Candidate(
    val face: Face,
    val embedding: FloatArray,
    val sharpness: Float,
    val clipped: Boolean,
    val croppedImagePath: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (sharpness != other.sharpness) return false
        if (clipped != other.clipped) return false
        if (face != other.face) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (croppedImagePath != other.croppedImagePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sharpness.hashCode()
        result = 31 * result + clipped.hashCode()
        result = 31 * result + face.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + croppedImagePath.hashCode()
        return result
    }
}