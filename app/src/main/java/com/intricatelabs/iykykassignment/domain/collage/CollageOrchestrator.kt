package com.intricatelabs.iykykassignment.domain.collage

import android.content.Context
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.dao.PersonClusterDao
import com.intricatelabs.iykykassignment.domain.ImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Step 4's VM-facing entry point. Resolves each cluster -> representative
 * face ONCE, here, then hands both the composed collage path AND the
 * resolved per-person list back — so the VM never needs its own DAO access
 * or a second query to build the results screen.
 */
@Singleton
class CollageOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clusterDao: PersonClusterDao,
    private val faceDetectionDao: FaceDetectionDao
) {

    suspend fun createCollage(videoUri: String): CollageResult = withContext(Dispatchers.Default) {
        val clusters = clusterDao.getClustersForVideo(videoUri)

        val people = clusters.mapNotNull { cluster ->
            val repFaceId = cluster.representativeFaceId ?: return@mapNotNull null
            val face = faceDetectionDao.getFaceById(repFaceId) ?: return@mapNotNull null
            PersonSummary(
                personId = cluster.id,
                representativeImagePath = face.croppedImagePath,
                appearanceCount = cluster.appearanceCount
            )
        }

        require(people.isNotEmpty()) { "No people with a representative shot to compose a collage from" }

        val bitmap = CollageComposer.compose(
            people.map { PersonForCollage(it.representativeImagePath, it.appearanceCount) }
        )
        val collagePath = try {
            ImageStorage.saveCollage(context, bitmap, videoUri)
        } finally {
            bitmap.recycle()
        }

        CollageResult(collagePath, people)
    }
}