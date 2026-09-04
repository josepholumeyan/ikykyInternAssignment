package com.intricatelabs.iykykassignment.domain.personidentification.appearance

data class ClosedAppearance(
    val startMs: Long,
    val endMs: Long,
    val faceIds: List<Long>,
    val representativeEmbedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClosedAppearance

        if (startMs != other.startMs) return false
        if (endMs != other.endMs) return false
        if (faceIds != other.faceIds) return false
        if (!representativeEmbedding.contentEquals(other.representativeEmbedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startMs.hashCode()
        result = 31 * result + endMs.hashCode()
        result = 31 * result + faceIds.hashCode()
        result = 31 * result + representativeEmbedding.contentHashCode()
        return result
    }
}