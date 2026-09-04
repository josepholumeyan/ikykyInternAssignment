package com.intricatelabs.iykykassignment.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appearance_segments",
    indices = [Index(value = ["clusterId"])],
    foreignKeys = [
        ForeignKey(
            entity = PersonClusterEntity::class,
            parentColumns = ["id"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppearanceSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clusterId: Long,
    val videoUri: String,
    val startTimestampMs: Long,
    val endTimestampMs: Long
)
