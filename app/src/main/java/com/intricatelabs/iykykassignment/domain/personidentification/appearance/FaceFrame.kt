package com.intricatelabs.iykykassignment.domain.personidentification.appearance

data class FaceCandidate(
    val faceId: Long,
    val embedding: FloatArray,
    val isClipped: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceCandidate

        if (faceId != other.faceId) return false
        if (isClipped != other.isClipped) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = faceId.hashCode()
        result = 31 * result + isClipped.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}