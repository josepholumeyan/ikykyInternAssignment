package com.intricatelabs.iykykassignment.domain.faceDetection

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity
import com.intricatelabs.iykykassignment.domain.ImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class FaceDetectionOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetectorWrapper,
    private val faceEmbedder: FaceEmbedder,
    private val faceDetectionDao: FaceDetectionDao
) {
    private val step = 200L
    private val extractionMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun process(
        videoUri: Uri,
        onProgress: (progress: Int) -> Unit = {}
    ) = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            if (durationMs < step) throw IllegalStateException("Video Too Short")

            val timestamps = mutableListOf<Long>()
            var currentTime = 0L
            while (currentTime < durationMs) {
                timestamps.add(currentTime)
                currentTime += step
            }

            val totalFrames = timestamps.size
            val framesProcessed = AtomicInteger(0)

            // Limited parallelism to 7 threads to maximize CPU usage without OOM
            val limitedDispatcher = Dispatchers.Default.limitedParallelism(7)

            withContext(limitedDispatcher) {
                timestamps.map { timeMs ->
                    async {
                        // Thread-safe extraction
                        val frame = extractionMutex.withLock {
                            frameExtractor.extractFrames(retriever, timeMs)
                        }

                        if (frame == null) {
                            val processed = framesProcessed.incrementAndGet()
                            onProgress((processed.toFloat() / totalFrames * 100).toInt())
                            return@async
                        }

                        try {
                            val rawFaces = faceDetector.detectFaces(frame.bitmap)

                            val candidates = rawFaces.mapNotNull { face ->
                                val box = face.boundingBox
                                val crop = FaceQualityUtils.cropWithMargin(frame.bitmap, box)
                                val sharpness = FaceQualityUtils.sharpnessScore(crop)
                                val faceArea = box.width() * box.height()

                                val dynamicMinSharpness = if (faceArea < 200 * 200) 1.5f else 3.0f

                                if (sharpness < dynamicMinSharpness) {
                                    crop.recycle()
                                    return@mapNotNull null
                                }

                                val clipped = FaceQualityUtils.isClipped(box, frame.bitmap.width, frame.bitmap.height)
                                val path = ImageStorage.saveCrop(context, crop, UUID.randomUUID().toString())
                                crop.recycle()

                                val alignedCrop = FaceAlignmentUtils.align(frame.bitmap, face, 112)
                                val embedding = faceEmbedder.embed(alignedCrop)
                                alignedCrop.recycle()

                                Candidate(face, embedding, sharpness, clipped, path)
                            }

                            // 3. Deduplication
                            val deduped = FaceDeduplicator.dedupeByEmbedding(
                                candidates,
                                embeddingOf = { it.embedding },
                                qualityOf = { it.sharpness }
                            )

                            // 4. Cleanup discarded
                            val discarded = candidates.filter { candidate -> deduped.none { it === candidate } }
                            discarded.forEach { candidate ->
                                File(candidate.croppedImagePath).delete()
                            }

                            // 5. Database Save
                            for (candidate in deduped) {
                                val entity = FaceDetectionEntity(
                                    videoUri = videoUri.toString(),
                                    timestampMs = frame.timestampMs,
                                    boundingBox = candidate.face.boundingBox,
                                    embedding = candidate.embedding,
                                    frontalityScore = faceDetector.frontalityScore(candidate.face),
                                    eyesOpenProb = faceDetector.eyesOpenScore(candidate.face),
                                    smileProb = faceDetector.smileScore(candidate.face),
                                    sharpnessScore = candidate.sharpness,
                                    isClipped = candidate.clipped,
                                    croppedImagePath = candidate.croppedImagePath
                                )
                                faceDetectionDao.insertDetection(entity)
                            }
                        } finally {
                            frame.bitmap.recycle()
                            val processed = framesProcessed.incrementAndGet()
                            onProgress((processed.toFloat() / totalFrames * 100).toInt())
                        }
                    }
                }.awaitAll()
            }
        } finally {
            retriever.release()
        }
    }
}
