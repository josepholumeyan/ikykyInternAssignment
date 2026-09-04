package com.intricatelabs.iykykassignment.domain.personidentification.cluster

import com.intricatelabs.iykykassignment.data.dao.AppearanceSegmentDao
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonClusterFactory @Inject constructor(
    private val clusterDao: PersonClusterDao,
    private val segmentDao: AppearanceSegmentDao,
    private val faceDetectionDao: FaceDetectionDao
) {
    fun create(
        videoUri: String,
        identityThreshold: Float = 0.6f,
    ): PersonClusterer {
        return PersonClusterer(
            videoUri,
            identityThreshold,
            clusterDao,
            segmentDao,
            faceDetectionDao
        )
    }
}