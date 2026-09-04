package com.intricatelabs.iykykassignment.data.entities

import android.graphics.Rect
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_detections",
    indices = [
        Index(value = ["videoUri"]),
        Index(value = ["clusterId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PersonClusterEntity::class,
            parentColumns = ["id"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FaceDetectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoUri: String,
    val timestampMs: Long,
    val boundingBox: Rect,        // Face location in frame
    val embedding: FloatArray,    // 192-d TFLite vector
    val frontalityScore: Float,   // Head pose evaluation
    val eyesOpenProb: Float,
    val smileProb: Float,
    val sharpnessScore: Float,
    val isClipped: Boolean,       // Touches image border?
    var clusterId: Long? = null ,     // Unassigned = -1
    val croppedImagePath: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceDetectionEntity

        if (id != other.id) return false
        if (timestampMs != other.timestampMs) return false
        if (frontalityScore != other.frontalityScore) return false
        if (eyesOpenProb != other.eyesOpenProb) return false
        if (smileProb != other.smileProb) return false
        if (sharpnessScore != other.sharpnessScore) return false
        if (isClipped != other.isClipped) return false
        if (clusterId != other.clusterId) return false
        if (videoUri != other.videoUri) return false
        if (boundingBox != other.boundingBox) return false
        if (!embedding.contentEquals(other.embedding)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + frontalityScore.hashCode()
        result = 31 * result + eyesOpenProb.hashCode()
        result = 31 * result + smileProb.hashCode()
        result = 31 * result + sharpnessScore.hashCode()
        result = 31 * result + isClipped.hashCode()
        result = 31 * result + clusterId.hashCode()
        result = 31 * result + videoUri.hashCode()
        result = 31 * result + boundingBox.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}
