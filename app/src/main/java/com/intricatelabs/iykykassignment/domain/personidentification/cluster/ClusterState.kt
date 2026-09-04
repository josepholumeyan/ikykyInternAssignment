package com.intricatelabs.iykykassignment.domain.personidentification.cluster

data class ClusterState(
    var id: Long,
    var centroid: FloatArray,
    var appearanceCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClusterState

        if (id != other.id) return false
        if (appearanceCount != other.appearanceCount) return false
        if (!centroid.contentEquals(other.centroid)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + appearanceCount
        result = 31 * result + centroid.contentHashCode()
        return result
    }
}