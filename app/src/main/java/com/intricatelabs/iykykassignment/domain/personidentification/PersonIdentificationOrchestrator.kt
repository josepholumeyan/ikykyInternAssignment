package com.intricatelabs.iykykassignment.domain.personidentification

import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity
import com.intricatelabs.iykykassignment.domain.personidentification.appearance.AppearanceTrackerFactory
import com.intricatelabs.iykykassignment.domain.personidentification.cluster.PersonClusterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2: turns already-stored FaceDetectionEntity rows (written by
 * FaceDetectionOrchestrator in phase 1) into identified people. Reads
 * detections back out grouped by frame timestamp, replays them through
 * AppearanceTracker exactly as if they were streaming live off the video,
 * then hands each closed appearance to PersonClusterer as soon as it closes.
 */
@Singleton
class PersonIdentificationOrchestrator @Inject constructor(
    private val faceDetectionDao: FaceDetectionDao,
    private val appearanceTrackerFactory: AppearanceTrackerFactory,
    private val personClusterFactory: PersonClusterFactory
) {

    suspend fun process(
        videoUri: String,
        onProgress: (progress: Int) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        // All of this video's detections, unordered as stored. These are
        // lightweight rows (ids + small FloatArrays), not bitmaps — unlike
        // phase 1, loading the whole video's detections into memory at once
        // here is a deliberate, safe trade-off, not an oversight.
        val detections = faceDetectionDao.getDetectionsForVideo(videoUri)
        if (detections.isEmpty()) return@withContext

        val tracker = appearanceTrackerFactory.create()
        val clusterer = personClusterFactory.create(videoUri)

        // Group by exact timestamp so every face detected in the SAME frame
        // is offered to the tracker together — required for offerFrame()'s
        // two-people-in-one-frame handling to work at all.
        val detectionsByTimestamp: Map<Long, List<FaceDetectionEntity>> =
            detections.groupBy { it.timestampMs }
        val orderedTimestamps = detectionsByTimestamp.keys.sorted()

        // NOTE: this counts only frames that had at least one detected face,
        // not every sampled frame in the video (phase 1's totalFrames was
        // duration/step). Progress here approximates "share of face-bearing
        // frames processed," not "share of video decoded" — fine for a
        // progress bar, just not literally the same number as phase 1's.
        val totalFrames = orderedTimestamps.size

        orderedTimestamps.forEachIndexed { index, timestampMs ->
            val facesThisFrame = detectionsByTimestamp.getValue(timestampMs)
                .map { it.id to it.embedding }

            tracker.offerFrame(facesThisFrame, timestampMs)

            val closed = tracker.closeStaleAppearances(timestampMs)
            closed.forEach { clusterer.assign(it) }

            onProgress(((index + 1).toFloat() / totalFrames * 100).toInt())
        }

        // Whoever's still active at the last processed frame never went stale
        tracker.flushAll().forEach { clusterer.assign(it) }
    }
}