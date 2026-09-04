package com.intricatelabs.iykykassignment.domain.faceDetection

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.face.Face
import com.intricatelabs.iykykassignment.data.dao.FaceDetectionDao
import com.intricatelabs.iykykassignment.data.entities.FaceDetectionEntity
import com.intricatelabs.iykykassignment.domain.ImageStorage
import com.intricatelabs.iykykassignment.domain.personidentification.appearance.AppearanceUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Ties the whole per-frame flow together: extract one frame, detect faces,
 * for each face crop + embed + score, build the entity, store it, then let
 * the frame's bitmap go before pulling the next one. This is the "second
 * part" — everything downstream of FaceDetectorWrapper.
 *
 * FaceDetectionEntity / FaceDetectionDao are your existing Room types —
 * import paths below assume they live in data.db, adjust to match yours.
 */
class FaceDetectionOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameExtractor: VideoFrameExtractor,
    private val faceDetector: FaceDetectorWrapper,
    private val faceEmbedder: FaceEmbedder,
    private val faceDetectionDao: FaceDetectionDao
) {
    val step = 300L

    suspend fun process(
        videoUri: Uri,
        onProgress: (progress: Int) -> Unit = {}
    )= withContext(Dispatchers.Default) {
        var framesProcessed = 0
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        if (durationMs < step) throw IllegalStateException("Video Too Short")

        val totalFrames = durationMs / step

        var timeMs = 0L
        while (timeMs < durationMs) {

            val frame = frameExtractor.extractFrames(videoUri, timeMs)
            if (frame == null) {
                framesProcessed++
                Log.i("FaceDetectionOrchestrator", "skipping empty frame at $timeMs this is the $framesProcessed frame processed")
                timeMs += step
                onProgress((framesProcessed.toFloat() / totalFrames * 100).toInt())
                continue
            }

            val rawFaces = faceDetector.detectFaces(frame.bitmap)
            Log.w("FaceDetectionOrchestrator", "amount of raw faces detected ${rawFaces.size}")
            Log.i("FaceDetectionOrchestrator", "width and height of frame ${frame.bitmap.width}, ${frame.bitmap.height}")



            val candidates = rawFaces.mapNotNull { face ->
                val box = face.boundingBox
                // 1. Natural crop for quality scoring and disk storage (visualization)
                val crop = FaceQualityUtils.cropWithMargin(frame.bitmap, box)
                val sharpness = FaceQualityUtils.sharpnessScore(crop)
                val faceArea = box.width() * box.height()

                val dynamicMinSharpness = if (faceArea < 200 * 200) 1.5f else 3.0f

                if (sharpness < dynamicMinSharpness) {
                    Log.w("FacePipeline", "Filtered out blurry face at timestamp: ${timeMs}ms (Score: $sharpness)")
                    return@mapNotNull null // blurry image affects the processing badly taking them out early
                }

                val clipped = FaceQualityUtils.isClipped(box, frame.bitmap.width, frame.bitmap.height)
                val path = ImageStorage.saveCrop(context, crop, UUID.randomUUID().toString())
                crop.recycle()

                // 2. Aligned crop for embedding generation
                val alignedCrop = FaceAlignmentUtils.align(frame.bitmap, face, 112)
                val embedding = faceEmbedder.embed(alignedCrop)
                alignedCrop.recycle()

                Candidate(face, embedding, sharpness, clipped, path)
            }

            for (i in candidates.indices) {
                for (j in i + 1 until candidates.size) {
                    val sim = AppearanceUtils.cosineSim(candidates[i].embedding, candidates[j].embedding)
                    Log.w("FaceDetectionOrchestrator", "pairwise sim candidate $i vs $j = $sim")
                }
            }

            val deduped = FaceDeduplicator.dedupeByEmbedding(
                candidates,
                embeddingOf = { it.embedding },
                qualityOf = { it.sharpness }
            )
            Log.w("FaceDetectionOrchestrator", "amount of faces detected after deduplication ${deduped.size}")

            // Clean up crop files for candidates that lost dedup — they were
            // written speculatively before we knew which ones would survive.
            // Reference equality (===), not equals(), on purpose: Candidate is a
            // data class, but two DIFFERENT candidates could in principle carry
            // near-identical embeddings; we want "is this the exact same object
            // dedup kept," not "is this value-equal to something dedup kept."
            val discarded = candidates.filter { candidate -> deduped.none { it === candidate } }
            discarded.forEach { candidate ->
                val deleted = File(candidate.croppedImagePath).delete()
                Log.i("FaceDetectionOrchestrator", "discarded duplicate crop deleted=$deleted path=${candidate.croppedImagePath}")
            }

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
                Log.i("FaceDetectionOrchestrator", "saving frame at ${entity.timestampMs}")
                faceDetectionDao.insertDetection(entity)
            }

            frame.bitmap.recycle() // release before the next frame is decoded
            framesProcessed++
            Log.i("FaceDetectionOrchestrator", "Frame processed - $framesProcessed at $timeMs")
            timeMs += step
            onProgress((framesProcessed.toFloat() / totalFrames * 100).toInt())
        }
        }
}
