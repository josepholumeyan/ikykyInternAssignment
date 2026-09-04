package com.intricatelabs.iykykassignment.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "person_clusters",
    indices = [
        Index(value = ["videoUri"]),
        Index(value = ["representativeFaceId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FaceDetectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["representativeFaceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PersonClusterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val appearanceCount: Int,
    val representativeFaceId: Long?,
    val centroidEmbedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PersonClusterEntity

        if (id != other.id) return false
        if (videoUri != other.videoUri) return false
        if (appearanceCount != other.appearanceCount) return false
        if (representativeFaceId != other.representativeFaceId) return false
        if (!centroidEmbedding.contentEquals(other.centroidEmbedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + videoUri.hashCode()
        result = 31 * result + appearanceCount
        result = 31 * result + (representativeFaceId?.hashCode() ?: 0)
        result = 31 * result + centroidEmbedding.contentHashCode()
        return result
    }
}
